package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/**
 * Boss 進場演出 controller（從 PlayerCloneEntity 抽出）。
 *
 * 涵蓋：地底鑽出 → 飛高集氣 → 爆炸顯現裝備 → 降落的進場動畫（含粒子/音效時序），以及裝備顯現邏輯
 * （pendingEquipment 套用 + 從背包挑最強主/副手武器）。擁有所有進場動畫 state field。
 *
 * 跨子系統借用（主人仍是本體）：introActive（過場旗標，被多處讀；本體擁有）、pendingEquipment（鏡像時填，
 * 這裡套用）、rebuildHandTurretsFromEquipped()（turret 系統的手持砲同步）、activateAfterIntro()（boss
 * 開戰初始化：bossEvent / graceTicks / Nara follow / 同源去重，本體擁有，動畫結束時委派呼叫）。
 */
class PlayerCloneIntroSequence {

    private static final int INTRO_RISE_START = 430;
    private static final int INTRO_RISE_END = 480;
    private static final int INTRO_FLY_END = 530;
    private static final int INTRO_REVEAL_TICK = 570;
    private static final int INTRO_DESCEND_START = 575;
    private static final int INTRO_LEN = 620;

    private final PlayerCloneEntity clone;

    PlayerCloneIntroSequence(PlayerCloneEntity clone) {
        this.clone = clone;
    }

    private int introTicks = 0;
    private double introX, introBaseY, introZ;

    /** 在登場點啟動演出：記錄座標、重置計時，埋在地底由 tick 推進。setPos/flags 由本體 startIntro 負責。 */
    void begin(double x, double baseY, double z) {
        this.introTicks = 0;
        this.introX = x;
        this.introBaseY = baseY;
        this.introZ = z;
    }

    /** 玩家跳過過場：直接推進到進場結尾，下次 tick 立即啟動 boss。 */
    void skip() {
        introTicks = INTRO_LEN;
    }

    // 登場座標：activateAfterIntro 結束時把 boss 定位回登場點
    double spawnX() { return introX; }
    double spawnBaseY() { return introBaseY; }
    double spawnZ() { return introZ; }

    void tick() {
        introTicks++;
        double y;
        if (introTicks < INTRO_RISE_START) {
            y = introBaseY - 3.0;                                  // 埋在地底
        } else if (introTicks < INTRO_RISE_END) {
            y = Mth.lerp(smooth(frac(introTicks, INTRO_RISE_START, INTRO_RISE_END)), introBaseY - 3.0, introBaseY); // 鑽出
        } else if (introTicks < INTRO_FLY_END) {
            y = Mth.lerp(smooth(frac(introTicks, INTRO_RISE_END, INTRO_FLY_END)), introBaseY, introBaseY + 4.0);    // 飛高
        } else if (introTicks < INTRO_DESCEND_START) {
            y = introBaseY + 4.0 + Math.sin((introTicks - INTRO_FLY_END) * 0.25) * 0.25;                            // 浮動集氣
        } else if (introTicks < INTRO_LEN) {
            y = Mth.lerp(smooth(frac(introTicks, INTRO_DESCEND_START, INTRO_LEN)), introBaseY + 4.0, introBaseY);   // 降落
        } else {
            clone.activateAfterIntro();
            return;
        }
        clone.setPos(introX, y, introZ);
        clone.setDeltaMovement(Vec3.ZERO);
        clone.setYRot(180.0F);
        clone.yBodyRot = 180.0F;
        clone.yHeadRot = 180.0F;

        if (clone.level() instanceof ServerLevel sl) {
            if (introTicks == INTRO_RISE_START) {
                // 鑽出瞬間：地面崩裂音 + 一圈塵土
                sl.playSound(null, introX, introBaseY, introZ, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.2F, 0.7F);
                sl.sendParticles(ParticleTypes.EXPLOSION, introX, introBaseY, introZ, 1, 0, 0, 0, 0);
            }
            if (introTicks >= INTRO_RISE_START && introTicks < INTRO_RISE_END) {
                // 地底鑽出：塵土 + 靈魂煙
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, introX, introBaseY, introZ, 8, 0.5, 0.1, 0.5, 0.01);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, introX, y + 0.5, introZ, 4, 0.4, 0.6, 0.4, 0.02);
            } else if (introTicks >= INTRO_FLY_END && introTicks < INTRO_REVEAL_TICK) {
                // 集氣：粒子從四周遠處慢慢飄向中心（越接近爆炸越密越快）
                float prog = frac(introTicks, INTRO_FLY_END, INTRO_REVEAL_TICK);
                int count = 14 + (int) (prog * 26);
                double radius = 7.0 - prog * 2.5;
                for (int i = 0; i < count; i++) {
                    double a = sl.random.nextDouble() * Math.PI * 2;
                    double h = sl.random.nextDouble() * 3.5 - 1.4;
                    // count=0：(dx,dy,dz) 為單位方向，speed 為速度大小 → 緩慢往中心飄
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            introX + Math.cos(a) * radius, y + h, introZ + Math.sin(a) * radius,
                            0, -Math.cos(a), -h * 0.3, -Math.sin(a), 0.10 + prog * 0.14);
                }
                if (introTicks % 5 == 0) {
                    sl.sendParticles(ParticleTypes.ENCHANT, introX, y + 1.0, introZ, 14, 2.5, 1.8, 2.5, 0.4);
                }
                if (introTicks % 10 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, introX, y + 0.5, introZ, 6, 1.5, 1.2, 1.5, 0.02);
                }
            }
            if (introTicks == INTRO_REVEAL_TICK) {
                // 爆炸：裝備此刻顯現
                revealEquipment();
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, introX, y, introZ, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.FLASH, introX, y + 0.5, introZ, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, introX, y + 0.5, introZ, 60, 0.1, 0.1, 0.1, 0.6);
                sl.playSound(null, clone.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.7F);
                sl.playSound(null, clone.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        }
    }

    void revealEquipment() {
        for (var e : clone.pendingEquipment.entrySet()) {
            clone.setItemSlot(e.getKey(), e.getValue());
        }
        equipBestWeapon();
        equipBestOffhand();
        clone.rebuildHandTurretsFromEquipped(); // 同步手持浮游砲列表（含 equipBestOffhand 從背包挑出的）
    }

    // 副手優先：浮游砲 → 盾牌；都找不到就保留 player 原本副手（不做去重，玩家手上有一堆一樣的物品很正常）
    private void equipBestOffhand() {
        ItemStack turret = ItemStack.EMPTY;
        ItemStack shield = ItemStack.EMPTY;
        for (ItemStack st : clone.getClonedInventory()) {
            if (st.isEmpty()) continue;
            if (turret.isEmpty() && st.getItem() instanceof FloatingTurretItem) turret = st;
            else if (shield.isEmpty() && st.getItem() instanceof ShieldItem) shield = st;
            if (!turret.isEmpty()) break;
        }
        ItemStack pick = !turret.isEmpty() ? turret : shield;
        if (!pick.isEmpty()) {
            clone.setItemSlot(EquipmentSlot.OFFHAND, pick.copy());
            clone.setNoDrop(EquipmentSlot.OFFHAND);
        }
    }

    // 拿背包裡最強武器到主手，三層優先級：
    //   tier 0: 浮游砲（本模組招牌武器）
    //   tier 1: vanilla 真武器（劍/斧/三叉戟）
    //   tier 2: vanilla 工具（鎬/鏟/任何有 ATTACK_DAMAGE）
    // 前一 tier 有東西就直接用，後 tier 高傷不會搶走
    private void equipBestWeapon() {
        ItemStack[] bestPerTier = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
        double[] bestAtkPerTier = { -1, -1, -1 };
        var clonedInventory = clone.getClonedInventory();
        java.util.List<ItemStack> candidates = new ArrayList<>(clonedInventory.size() + 1);
        candidates.add(clone.getMainHandItem());
        candidates.addAll(clonedInventory);
        for (ItemStack st : candidates) {
            if (st.isEmpty()) continue;
            int tier;
            if (st.getItem() instanceof FloatingTurretItem) tier = 0;
            else if (PlayerCloneEntity.isProperWeapon(st)) tier = 1;
            else tier = 2;
            double a = weaponAttack(st);
            if (tier == 2 && a <= 0) continue; // 工具沒攻擊加成不考慮
            if (a > bestAtkPerTier[tier]) {
                bestAtkPerTier[tier] = a;
                bestPerTier[tier] = st;
            }
        }
        ItemStack best = ItemStack.EMPTY;
        for (int i = 0; i < bestPerTier.length; i++) {
            if (!bestPerTier[i].isEmpty()) { best = bestPerTier[i]; break; }
        }
        if (!best.isEmpty() && !ItemStack.isSameItemSameComponents(best, clone.getMainHandItem())) {
            clone.setItemSlot(EquipmentSlot.MAINHAND, best.copy());
            clone.setNoDrop(EquipmentSlot.MAINHAND);
        }
    }

    private static double weaponAttack(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        double atk = 0;
        for (var e : stack.getAttributeModifiers().modifiers()) {
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE) && e.slot().test(EquipmentSlot.MAINHAND)) {
                atk += e.modifier().amount();
            }
        }
        return atk;
    }

    private static float frac(int t, int start, int end) {
        return (t - start) / (float) (end - start);
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
