package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.config.ModCommonConfig;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Boss 鏡像 + 浮游砲 controller（從 PlayerCloneEntity 抽出）。
 *
 * 涵蓋：mirrorFrom（鏡像玩家裝備/背包/浮游砲，含遞迴展開容器杜絕「藏進盒子規避鏡像」）、
 * 自走砲與手持砲生成、boss 主動齊射技能（telegraph + 同步齊射）。
 * 擁有鏡像浮游砲清單 + 齊射 state field。
 *
 * 跨子系統借用（主人仍是本體）：pendingEquipment（鏡像時填，intro.revealEquipment 套用）、
 * getClonedInventory()（疊牆 AI 取材）、bossHotbar（戰鬥中切換主手）、updateBossBarName()、
 * setNoDrop()（Mob.setDropChance protected 的 package-private wrapper）、isProperWeapon()（武器排序）。
 */
class PlayerCloneMirrorManager {

    private static final int CONTAINER_DEPTH_CAP = 4;

    // Boss 主動發砲齊射技能：1 秒 telegraph + 同步所有 boss 砲對玩家蓄力齊射
    private static final int TURRET_VOLLEY_INTERVAL = 200; // 10 秒一次齊射
    private static final int TURRET_VOLLEY_TELEGRAPH = 20; // 1 秒前搖
    private static final double TURRET_VOLLEY_RANGE_SQ = 32 * 32;

    private final PlayerCloneEntity clone;

    // 鏡像的浮游砲（已過濾不鏡射的升級）。自走砲（繞行）vs 手持（雙持蓄力）分開
    // package-private：本體 NBT save/load 直接讀寫
    final List<ItemStack> mirroredTurrets = new ArrayList<>();      // 裝備槽/背包 → 繞行自走砲
    final List<ItemStack> mirroredHandTurrets = new ArrayList<>();  // 主手/副手 → 手持蓄力

    private int turretVolleyCooldown = 80; // 開戰初始 4 秒緩衝再開始算
    private int turretVolleyTelegraph = 0;

    PlayerCloneMirrorManager(PlayerCloneEntity clone) {
        this.clone = clone;
    }

    void mirrorFrom(Player player) {
        clone.setSourceUUID(player.getUUID());
        clone.setSourceName(player.getGameProfile().getName());

        // 收集玩家「擁有的全部物品」：穿戴 + 主副手 + 背包 + 額外裝備槽，再遞迴展開容器內容
        // （界伏盒/潛影盒/收納袋/巢狀），杜絕把裝備藏進盒子讓 boss 鏡像不到的逃課
        List<ItemStack> pool = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) pool.add(player.getItemBySlot(slot));
        pool.addAll(player.getInventory().items);
        NonNullList<ItemStack> extra = player.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        pool.addAll(extra);
        // 9 格儲存欄位（NINE_GRID 飾品背包）— 玩家會把武器藏這裡逃 boss 鏡像
        NonNullList<ItemStack> nineGrid = player.getData(ModDataAttachments.NINE_GRID.get());
        pool.addAll(nineGrid);
        List<ItemStack> contained = new ArrayList<>();
        for (ItemStack s : pool) collectContained(s, contained, 0);
        pool.addAll(contained);

        // 裝備：每個防具槽鏡像「擁有的最強」（藏在盒子裡的也算進來），進場爆炸那刻才顯現
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack best = ItemStack.EMPTY;
                double bestScore = -1;
                for (ItemStack s : pool) {
                    double sc = armorScore(s, slot);
                    if (sc > bestScore) { bestScore = sc; best = s; }
                }
                clone.pendingEquipment.put(slot, best.copy());
            } else {
                // 主/副手先放玩家當下手持（供下方手持砲判定），武器最強由 equipBestWeapon 補
                clone.pendingEquipment.put(slot, player.getItemBySlot(slot).copy());
            }
            clone.setNoDrop(slot);
        }

        // clonedInventory：把擁有的物品填進去（供疊牆 AI 取方塊、equipBestWeapon 選到盒裡的武器）
        // 重要物品（浮游砲/武器/盾）先排前面，避免主背包滿 36 格垃圾把 EXTRA_EQUIPMENT / 盒子內武器擠掉
        // 之前 bug：玩家把好武器藏進 EXTRA_EQUIPMENT 而主背包塞滿雜物 → boss 永遠選不到那把武器
        NonNullList<ItemStack> clonedInventory = clone.getClonedInventory();
        List<ItemStack> prioritized = new ArrayList<>(pool.size());
        for (ItemStack s : pool) if (!s.isEmpty() && s.getItem() instanceof FloatingTurretItem) prioritized.add(s);
        for (ItemStack s : pool) if (!s.isEmpty() && PlayerCloneEntity.isProperWeapon(s)) prioritized.add(s);
        for (ItemStack s : pool) if (!s.isEmpty() && s.getItem() instanceof ShieldItem) prioritized.add(s);
        // 其餘物品（包含工具、方塊等）尾隨在後
        for (ItemStack s : pool) {
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof FloatingTurretItem) continue;
            if (PlayerCloneEntity.isProperWeapon(s)) continue;
            if (s.getItem() instanceof ShieldItem) continue;
            prioritized.add(s);
        }
        int idx = 0;
        for (ItemStack s : prioritized) {
            if (idx >= clonedInventory.size()) break;
            clonedInventory.set(idx++, s.copy());
        }
        while (idx < clonedInventory.size()) clonedInventory.set(idx++, ItemStack.EMPTY);

        // 鏡像浮游砲：mirroredHandTurrets[main, off]，mirroredTurrets 對應 player 的兩個 EXTRA_EQUIPMENT 槽
        mirroredTurrets.clear();
        mirroredHandTurrets.clear();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        mirroredHandTurrets.add(mainHand.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(mainHand) : ItemStack.EMPTY);
        mirroredHandTurrets.add(offHand.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(offHand) : ItemStack.EMPTY);
        // 繞行砲：優先從 EXTRA_EQUIPMENT 的 2 個槽鏡像
        for (int i = 0; i < extra.size() && mirroredTurrets.size() < 2; i++) {
            ItemStack s = extra.get(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof FloatingTurretItem) {
                mirroredTurrets.add(filterMirroredTurret(s));
            }
        }
        // 補位：玩家把浮游砲藏在背包/盒子也要鏡像（避免「不裝就規避」），從 pool 補到 2 個
        // 跳過已經安排在主/副手/extra 那些浮游砲，避免重複鏡像
        Set<ItemStack> alreadyMirrored = new HashSet<>();
        if (mainHand.getItem() instanceof FloatingTurretItem) alreadyMirrored.add(mainHand);
        if (offHand.getItem() instanceof FloatingTurretItem) alreadyMirrored.add(offHand);
        for (ItemStack s : extra) if (s.getItem() instanceof FloatingTurretItem) alreadyMirrored.add(s);
        for (ItemStack s : pool) {
            if (mirroredTurrets.size() >= 2) break;
            if (s.isEmpty() || alreadyMirrored.contains(s)) continue;
            if (s.getItem() instanceof FloatingTurretItem) {
                mirroredTurrets.add(filterMirroredTurret(s));
            }
        }

        // 鏡像玩家快捷欄（前 9 格）供戰鬥中切換
        for (int i = 0; i < 9; i++) {
            ItemStack hot = i < player.getInventory().items.size() ? player.getInventory().items.get(i) : ItemStack.EMPTY;
            clone.bossHotbar.set(i, hot.copy());
        }

        clone.setHealth(clone.getMaxHealth());
        clone.updateBossBarName();
        // 進場過場期間先不發光（保留神秘感），由 customServerAiStep 在「正常戰鬥」狀態才開
        clone.setGlowingTag(false);
    }

    // 遞迴展開容器物品內容：界伏盒/潛影盒等（CONTAINER component）+ 收納袋（BUNDLE_CONTENTS）
    private static void collectContained(ItemStack stack, List<ItemStack> out, int depth) {
        if (stack.isEmpty() || depth >= CONTAINER_DEPTH_CAP) return;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            for (ItemStack inner : container.nonEmptyItems()) {
                out.add(inner);
                collectContained(inner, out, depth + 1);
            }
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            bundle.itemCopyStream().forEach(inner -> {
                out.add(inner);
                collectContained(inner, out, depth + 1);
            });
        }
    }

    // 該物品作為指定防具槽的防護評分（ARMOR + ARMOR_TOUGHNESS），非該槽可穿戴回 -1
    private static double armorScore(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return -1;
        Equipable eq = Equipable.get(stack);
        if (eq == null || eq.getEquipmentSlot() != slot) return -1;
        double v = 0;
        for (var e : stack.getAttributeModifiers().modifiers()) {
            if (!e.slot().test(slot)) continue;
            if (e.attribute().equals(Attributes.ARMOR) || e.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                v += e.modifier().amount();
            }
        }
        return v;
    }

    // 不鏡射治療升級（避免分身自我回血變消耗戰）；其餘升級保留
    private static ItemStack filterMirroredTurret(ItemStack original) {
        ItemStack copy = original.copy();
        EquipmentUpgradeData data = FloatingTurretItem.getData(copy);
        if (data.upgrades().isEmpty()) return copy;
        EquipmentUpgradeData filtered = data;
        for (var e : new HashMap<>(data.upgrades()).entrySet()) {
            if (e.getValue().getItem() instanceof TurretUpgradeItem tu
                    && tu.getBehavior() == TurretUpgradeBehavior.HEALING) {
                filtered = filtered.withUpgrade(e.getKey(), ItemStack.EMPTY);
            }
        }
        FloatingTurretItem.setData(copy, filtered);
        return copy;
    }

    void spawnMirroredTurrets() {
        if (!(clone.level() instanceof ServerLevel sl)) return;
        // 自走砲：slotIndex 0/1（繞行）
        for (int i = 0; i < mirroredTurrets.size(); i++) {
            spawnCloneTurret(sl, mirroredTurrets.get(i), i);
        }
        // 手持砲：mirroredHandTurrets 永遠 [main, off]（可為 EMPTY），slotIndex 2=主手、3=副手
        for (int i = 0; i < mirroredHandTurrets.size(); i++) {
            ItemStack hs = mirroredHandTurrets.get(i);
            if (hs.isEmpty()) continue;
            spawnCloneTurret(sl, hs, i + 2);
        }
    }

    // 依目前裝備的主/副手浮游砲重建 mirroredHandTurrets（永遠 2 槽 [main, off]，可 EMPTY）
    // revealEquipment 後呼叫，讓 equipBestOffhand 從背包挑的浮游砲也能變成實體會射擊
    void rebuildHandTurretsFromEquipped() {
        mirroredHandTurrets.clear();
        ItemStack main = clone.getMainHandItem();
        ItemStack off = clone.getOffhandItem();
        mirroredHandTurrets.add(main.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(main) : ItemStack.EMPTY);
        mirroredHandTurrets.add(off.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(off) : ItemStack.EMPTY);
    }

    private void spawnCloneTurret(ServerLevel sl, ItemStack stack, int slotIndex) {
        FloatingTurretEntity turret = ModEntities.FLOATING_TURRET.get().create(sl);
        if (turret == null) return;
        turret.moveTo(clone.getX(), clone.getY() + 1.0, clone.getZ(), 0f, 0f);
        turret.setupAsCloneTurret(clone, stack, slotIndex);
        sl.addFreshEntity(turret);
    }

    // Boss 主動發砲齊射：每 10 秒一次，前 1 秒在每門砲上噴 END_ROD 粒子當預兆，然後同時對玩家蓄力齊射
    void tickTurretVolley() {
        if (!ModCommonConfig.INSTANCE.bossTurretVolleyEnabled.get()) return;
        if (turretVolleyTelegraph > 0) {
            turretVolleyTelegraph--;
            if (clone.level() instanceof ServerLevel sl) {
                for (FloatingTurretEntity t : findOwnedTurrets(sl)) {
                    sl.sendParticles(ParticleTypes.END_ROD,
                            t.getX(), t.getY(), t.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (turretVolleyTelegraph == 0) executeTurretVolley();
            return;
        }
        if (turretVolleyCooldown > 0) { turretVolleyCooldown--; return; }
        LivingEntity tgt = clone.getTarget();
        if (tgt == null || !tgt.isAlive()) return;
        if (clone.distanceToSqr(tgt) > TURRET_VOLLEY_RANGE_SQ) return;
        // 啟動前搖：播一個低沉蓄力音效讓玩家有反應時間
        turretVolleyTelegraph = TURRET_VOLLEY_TELEGRAPH;
        if (clone.level() instanceof ServerLevel sl) {
            sl.playSound(null, clone.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.4F);
        }
    }

    private void executeTurretVolley() {
        turretVolleyCooldown = TURRET_VOLLEY_INTERVAL;
        LivingEntity tgt = clone.getTarget();
        if (tgt == null || !tgt.isAlive()) return;
        if (!(clone.level() instanceof ServerLevel sl)) return;
        Vec3 targetPos = tgt.getBoundingBox().getCenter();
        for (FloatingTurretEntity t : findOwnedTurrets(sl)) {
            FloatingTurretProjectile proj = FloatingTurretProjectile.shootAt(
                    sl, clone, t.position(), targetPos, 1.0F); // charged
            proj.setNoBlockDamage(true);
            sl.addFreshEntity(proj);
        }
        sl.playSound(null, clone.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.6F);
    }

    private List<FloatingTurretEntity> findOwnedTurrets(ServerLevel sl) {
        return sl.getEntitiesOfClass(FloatingTurretEntity.class,
                clone.getBoundingBox().inflate(32.0),
                t -> t.getCloneOwner() == clone);
    }
}
