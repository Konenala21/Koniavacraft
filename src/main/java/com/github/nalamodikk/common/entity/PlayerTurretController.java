package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.event.FloatingTurretEventHandler;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * 浮游砲玩家模式邏輯封裝。
 * owner 是真的 Player（透過 OWNER_UUID_DATA 解析），實體跟在裝備槽或手持邊。
 * 負責位移計算、自動攻擊、控制彈、治療、耐久消耗、裝備返還。
 */
class PlayerTurretController {

    private static final int MANA_PER_ATTACK = 50;

    private final FloatingTurretEntity turret;

    // 控制彈各自獨立冷卻（自走砲模式；實體每次戰鬥重建故 transient）
    private final Map<TurretUpgradeBehavior, Integer> controlCooldowns =
            new EnumMap<>(TurretUpgradeBehavior.class);

    PlayerTurretController(FloatingTurretEntity turret) {
        this.turret = turret;
    }

    /** 由 FloatingTurretEntity.serverTick 在 clone 模式未啟用時呼叫。回傳 true 表示已 discard，外部應 return。 */
    boolean serverTick() {
        Player owner = turret.getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            turret.discard();
            return true;
        }

        int slotIdx = turret.getSlotIndex();

        // 手持模式：確認玩家還持有浮游砲
        if (slotIdx >= 2) {
            ItemStack handItem = slotIdx == 2 ? owner.getMainHandItem() : owner.getOffhandItem();
            if (handItem.isEmpty() || !(handItem.getItem() instanceof FloatingTurretItem)) {
                turret.discard();
                return true;
            }
        }

        updateOrbitAngle();
        positionTurret(owner, slotIdx);

        // 更新戰鬥狀態
        long lastCombat = owner.getData(ModDataAttachments.LAST_COMBAT_TIME.get());
        boolean inCombat = lastCombat >= 0
                && (turret.level().getGameTime() - lastCombat) < FloatingTurretEntity.COMBAT_LINGER_TICKS;
        turret.setInCombat(inCombat);

        // 治療升級：定期回復擁有者
        if (turret.tickCount % FloatingTurretItem.HEAL_INTERVAL_TICKS == 0) {
            tryHealOwner(owner);
        }

        // 自走砲：戰鬥結束立刻消失（不等 EventHandler 20-tick 間隔）
        if (slotIdx < 2 && !inCombat) {
            turret.discard();
            return true;
        }

        // 自動攻擊：僅裝備槽模式，手持靠右鍵主動攻擊
        if (slotIdx < 2 && inCombat) {
            LivingEntity target = findTarget(owner);
            if (target != null) {
                if (turret.attackTimer == 0) {
                    performPassiveAttack(owner, target);
                    turret.attackTimer = FloatingTurretEntity.PASSIVE_ATTACK_COOLDOWN;
                }
                tickControlShots(owner, target);
            }
        }
        if (turret.attackTimer > 0) turret.attackTimer--;
        return false;
    }

    /** hurt 套用 DEFENSE 減傷；玩家來源仍直接擋。回傳要傳給 super.hurt 的 amount。 */
    float applyDamageReduction(float amount) {
        Player owner = turret.getOwnerPlayer();
        if (owner != null) {
            ItemStack stack = getSourceStack(owner);
            if (stack.getItem() instanceof FloatingTurretItem) {
                amount *= (1f - FloatingTurretItem.getDamageReduction(stack));
            }
        }
        return amount;
    }

    /** 取得本實體對應的浮游砲 ItemStack（裝備槽或手持）。 */
    ItemStack getSourceStack(Player owner) {
        int slot = turret.getSlotIndex();
        if (slot == FloatingTurretEventHandler.HAND_MAIN_SLOT) return owner.getMainHandItem();
        if (slot == FloatingTurretEventHandler.HAND_OFF_SLOT) return owner.getOffhandItem();
        NonNullList<ItemStack> equipment = owner.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        int dataIdx = 8 + slot;
        return dataIdx < equipment.size() ? equipment.get(dataIdx) : ItemStack.EMPTY;
    }

    /** 依升級重算最大血量（生成時呼叫）。 */
    void applyUpgradesFromOwner(Player owner) {
        ItemStack stack = getSourceStack(owner);
        if (stack.isEmpty() || !(stack.getItem() instanceof FloatingTurretItem)) return;
        var attr = turret.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(100.0 + FloatingTurretItem.getHealthBonus(stack));
            turret.setHealth(turret.getMaxHealth());
        }
    }

    /** die() 時把對應裝備槽的浮游砲還給玩家。 */
    void returnItemToOwner() {
        Player owner = turret.getOwnerPlayer();
        if (owner == null) return;

        int slotIdx = turret.getSlotIndex();
        if (slotIdx >= 2) return; // 手持實體無裝備槽資料

        int dataIdx = 8 + slotIdx;
        NonNullList<ItemStack> equipment = owner.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        if (dataIdx >= equipment.size()) return;

        ItemStack stack = equipment.get(dataIdx);
        if (stack.isEmpty()) return;

        equipment.set(dataIdx, ItemStack.EMPTY);
        owner.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), equipment);

        if (!owner.getInventory().add(stack.copy())) {
            owner.drop(stack.copy(), false);
        }
    }

    // ── 內部 ───────────────────────────────────────────────────────────────────

    private void updateOrbitAngle() {
        float angle = turret.getOrbitAngle();
        angle += FloatingTurretEntity.ORBIT_SPEED;
        if (angle > (float) (2 * Math.PI)) angle -= (float) (2 * Math.PI);
        turret.setOrbitAngle(angle);
    }

    private void positionTurret(Player owner, int slotIdx) {
        float yawRad = owner.getYRot() * (float) (Math.PI / 180.0);
        double posX, posY, posZ;

        if (slotIdx < 2) {
            // 保護使用者 Mk1：擋在玩家與最近敵人之間
            LivingEntity protectTarget = FloatingTurretItem.getUpgradeMk(
                    getSourceStack(owner), TurretUpgradeBehavior.PROTECT) >= 1
                    ? turret.findNearestHostile() : null;
            if (protectTarget != null) {
                Vec3 dir = protectTarget.position().subtract(owner.position());
                if (dir.horizontalDistanceSqr() > 0.001) {
                    dir = dir.normalize();
                    posX = owner.getX() + dir.x * FloatingTurretEntity.ORBIT_RADIUS;
                    posY = owner.getY() + FloatingTurretEntity.ORBIT_HEIGHT;
                    posZ = owner.getZ() + dir.z * FloatingTurretEntity.ORBIT_RADIUS;
                } else {
                    posX = owner.getX();
                    posY = owner.getY() + FloatingTurretEntity.ORBIT_HEIGHT;
                    posZ = owner.getZ();
                }
            } else {
                // 裝備槽：固定在玩家背後左右兩側
                float behindAngle = (float) Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));
                float spread = (float) (Math.PI / 5);
                float finalAngle = behindAngle + (slotIdx == 0 ? -spread : spread);
                float bob = (float) (Math.sin(turret.tickCount * 0.08) * 0.2);
                posX = owner.getX() + Math.cos(finalAngle) * FloatingTurretEntity.ORBIT_RADIUS;
                posY = owner.getY() + FloatingTurretEntity.ORBIT_HEIGHT + bob;
                posZ = owner.getZ() + Math.sin(finalAngle) * FloatingTurretEntity.ORBIT_RADIUS;
            }
        } else {
            // 手持：側邊 1.8 + 前方 1.5（與 FloatingTurretPlayerRenderer 相同的數學）
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            boolean isLeftHanded = owner.getMainArm() == HumanoidArm.LEFT;
            double mainHandSide = isLeftHanded ? -1.0 : 1.0;
            double side = (slotIdx == 2) ? mainHandSide : -mainHandSide;
            double translateCorrect = (1.0 - 0.0843) * 0.35;
            posX = owner.getX() + rightX * side * 1.8 + forwardX * 1.5 + rightX * translateCorrect;
            posY = owner.getEyeY() + 0.8;
            posZ = owner.getZ() + rightZ * side * 1.8 + forwardZ * 1.5 + rightZ * translateCorrect;
        }
        turret.setPos(posX, posY, posZ);
    }

    // 控制彈：每種控制升級獨立冷卻
    private void tickControlShots(Player owner, LivingEntity target) {
        ItemStack stack = getSourceStack(owner);
        if (!(stack.getItem() instanceof FloatingTurretItem)) return;
        if (!(turret.level() instanceof ServerLevel sl)) return;
        for (ItemStack upg : FloatingTurretItem.getData(stack).upgrades().values()) {
            if (!(upg.getItem() instanceof TurretUpgradeItem tu)) continue;
            TurretUpgradeBehavior b = tu.getBehavior();
            if (!b.isControl() || b.getControlEffect() == null) continue;
            int cd = controlCooldowns.getOrDefault(b, 0);
            if (cd > 0) { controlCooldowns.put(b, cd - 1); continue; }
            Vec3 spawn = turret.position().add(0, 0.2, 0);
            Vec3 tgt = target.getBoundingBox().getCenter();
            // owner 用 player 而非 turret：FloatingTurretProjectile.canHitEntity 會擋 owner，避免控制彈反控玩家自己
            sl.addFreshEntity(FloatingTurretProjectile.shootControl(
                    sl, owner, spawn, tgt, b.getControlEffect(), b.getControlDuration()));
            controlCooldowns.put(b, b.getControlCooldown());
        }
    }

    // 目標選取：預設最近敵對；裝有 PLAYER_LOCK 升級時納入鎖定的攻擊者玩家
    @Nullable
    private LivingEntity findTarget(Player owner) {
        LivingEntity nearest = turret.findNearestHostile();
        ItemStack stack = getSourceStack(owner);
        if (stack.getItem() instanceof FloatingTurretItem
                && FloatingTurretItem.hasUpgrade(stack, TurretUpgradeBehavior.PLAYER_LOCK)
                && turret.level() instanceof ServerLevel sl) {
            UUID lockedId = FloatingTurretEventHandler.getLockedAttacker(owner.getUUID());
            if (lockedId != null) {
                Player locked = sl.getPlayerByUUID(lockedId);
                if (locked != null && locked.isAlive() && locked != owner
                        && locked.distanceToSqr(turret) <= FloatingTurretEntity.PASSIVE_RANGE * FloatingTurretEntity.PASSIVE_RANGE
                        && (nearest == null || locked.distanceToSqr(turret) < nearest.distanceToSqr(turret))) {
                    return locked;
                }
            }
        }
        return nearest;
    }

    private void performPassiveAttack(Player owner, LivingEntity target) {
        int dataIdx = 8 + turret.getSlotIndex();
        NonNullList<ItemStack> equipment = owner.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        if (dataIdx >= equipment.size()) return;

        ItemStack stack = equipment.get(dataIdx);
        if (stack.isEmpty() || !(stack.getItem() instanceof FloatingTurretItem)) return;

        int mana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mana < MANA_PER_ATTACK) return;

        // 發射砲彈，同時重設戰鬥計時器
        owner.setData(ModDataAttachments.LAST_COMBAT_TIME.get(), turret.level().getGameTime());
        if (turret.level() instanceof ServerLevel sl) {
            Vec3 targetPos = target.getBoundingBox().getCenter();
            FloatingTurretProjectile proj = FloatingTurretProjectile.shootAt(
                    sl, owner, turret.position(), targetPos);
            sl.addFreshEntity(proj);
        }

        stack.set(ModDataComponents.MANA_STORED, mana - MANA_PER_ATTACK);
        consumeDurability(owner, equipment, dataIdx, stack);
    }

    private void consumeDurability(Player owner, NonNullList<ItemStack> equipment, int dataIdx, ItemStack stack) {
        if (!stack.isDamageableItem()) return;
        int newDamage = stack.getDamageValue() + 1;
        if (newDamage >= stack.getMaxDamage()) {
            equipment.set(dataIdx, ItemStack.EMPTY);
            owner.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), equipment);
            turret.discard();
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    private void tryHealOwner(Player owner) {
        ItemStack stack = getSourceStack(owner);
        if (stack.isEmpty() || !(stack.getItem() instanceof FloatingTurretItem)) return;
        int heal = FloatingTurretItem.getHealAmount(stack);
        if (heal <= 0) return;
        if (owner.getHealth() >= owner.getMaxHealth()) return;
        int mana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mana < FloatingTurretItem.HEAL_MANA_COST) return;
        owner.heal(heal);
        stack.set(ModDataComponents.MANA_STORED, mana - FloatingTurretItem.HEAL_MANA_COST);
    }
}
