package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 浮游砲 boss 鏡像模式的邏輯封裝。
 * owner 是 boss 分身（PlayerCloneEntity 之類的 LivingEntity 而非 Player），
 * 自帶鏡像過的砲 stack。負責繞行/手持的位移、射擊、玩家免疫、boss-vs-boss 同源免疫。
 */
class CloneTurretController {

    private static final int CLONE_HAND_CHARGE_COOLDOWN = 70;

    private final FloatingTurretEntity turret;

    @Nullable private LivingEntity cloneOwner = null;
    private ItemStack cloneTurretStack = ItemStack.EMPTY;
    private int cloneShotCount = 0;

    CloneTurretController(FloatingTurretEntity turret) {
        this.turret = turret;
    }

    boolean isActive() {
        return cloneOwner != null;
    }

    @Nullable
    LivingEntity getCloneOwner() {
        return cloneOwner;
    }

    void setup(LivingEntity owner, ItemStack turretStack, int slotIndex) {
        this.cloneOwner = owner;
        this.cloneTurretStack = turretStack.copy();
        turret.setSlotIndex(slotIndex);
        var attr = turret.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(100.0 + FloatingTurretItem.getHealthBonus(cloneTurretStack));
            turret.setHealth(turret.getMaxHealth());
        }
    }

    /** 由 FloatingTurretEntity.serverTick 在 isActive() 時委派呼叫。 */
    void serverTick() {
        if (cloneOwner == null || !cloneOwner.isAlive() || cloneOwner.isRemoved()) {
            turret.discard();
            return;
        }
        // 二階段變身過場期間：boss 凍結 + 無敵，砲也跟著凍結
        if (cloneOwner instanceof PlayerCloneEntity pc && pc.isPhase2Transitioning()) {
            return;
        }
        int slotIdx = turret.getSlotIndex();

        float angle = turret.getOrbitAngle();
        angle += FloatingTurretEntity.ORBIT_SPEED;
        if (angle > (float) (2 * Math.PI)) angle -= (float) (2 * Math.PI);
        turret.setOrbitAngle(angle);
        turret.setInCombat(true);

        if (slotIdx >= 2) {
            cloneHandTick(slotIdx);
        } else {
            cloneOrbitTick(slotIdx);
        }
    }

    /**
     * 處理 clone 模式下的 hurt。
     * blocked=true → entity.hurt 直接 return false
     * blocked=false → entity.hurt 用 adjustedAmount 呼叫 super.hurt
     */
    HurtResult handleHurt(DamageSource source, float amount) {
        // 玩家無法傷害：強制把仇恨導向 boss 本體
        if (source.getEntity() instanceof Player) {
            return HurtResult.BLOCK;
        }
        // 免疫同源浮游砲彈（不被自己或同伴炸死）
        if (source.getDirectEntity() instanceof FloatingTurretProjectile proj
                && proj.getOwner() == cloneOwner) {
            return HurtResult.BLOCK;
        }
        if (cloneTurretStack.getItem() instanceof FloatingTurretItem) {
            amount *= (1f - FloatingTurretItem.getDamageReduction(cloneTurretStack));
        }
        return new HurtResult(false, amount);
    }

    // ── 自走砲：繞行分身背後左右兩側，自動射擊（每 4 發蓄力） ─────────────────
    private void cloneOrbitTick(int slotIdx) {
        float yawRad = cloneOwner.getYRot() * (float) (Math.PI / 180.0);
        float bob = (float) (Math.sin(turret.tickCount * 0.08) * 0.2);
        boolean mechaHead = cloneOwner instanceof PlayerCloneEntity pc && pc.isArmored();
        Vec3 mount = mechaHead
                ? ((PlayerCloneEntity) cloneOwner).getTurretMountOffset(slotIdx)
                : null;
        if (mount != null) {
            double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
            double wx = mount.x * cosY - mount.z * sinY;
            double wz = mount.x * sinY + mount.z * cosY;
            turret.setPos(
                    cloneOwner.getX() + wx,
                    cloneOwner.getY() + mount.y + bob * 0.5,
                    cloneOwner.getZ() + wz);
        } else {
            float behindAngle = (float) Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));
            float spread = (float) (Math.PI / 5);
            float finalAngle = behindAngle + (slotIdx == 0 ? -spread : spread);
            float radius = mechaHead ? 0.9F : FloatingTurretEntity.ORBIT_RADIUS;
            double yOffset = mechaHead
                    ? (PlayerCloneEntity.ARMORED_HEAD_TOP_Y + 2.0)
                    : FloatingTurretEntity.ORBIT_HEIGHT;
            turret.setPos(
                    cloneOwner.getX() + Math.cos(finalAngle) * radius,
                    cloneOwner.getY() + yOffset + bob,
                    cloneOwner.getZ() + Math.sin(finalAngle) * radius);
        }

        if (turret.attackTimer == 0) {
            LivingEntity target = cloneTarget();
            if (target != null && turret.level() instanceof ServerLevel sl) {
                cloneShotCount++;
                float charge = (cloneShotCount % 4 == 0) ? 1.0F : 0.0F;
                FloatingTurretProjectile proj = FloatingTurretProjectile.shootAt(
                        sl, cloneOwner, turret.position(), target.getBoundingBox().getCenter(), charge);
                proj.setNoBlockDamage(true);
                sl.addFreshEntity(proj);
                turret.attackTimer = FloatingTurretEntity.PASSIVE_ATTACK_COOLDOWN;
            }
        }
        if (turret.attackTimer > 0) turret.attackTimer--;
    }

    // ── 手持模式：站在分身手邊，蓄力後發強化彈 ───────────────────────────
    private void cloneHandTick(int slotIdx) {
        float yawRad = cloneOwner.getYRot() * (float) (Math.PI / 180.0);
        boolean mechaHead = cloneOwner instanceof PlayerCloneEntity pc && pc.isArmored();
        Vec3 mount = mechaHead
                ? ((PlayerCloneEntity) cloneOwner).getTurretMountOffset(slotIdx)
                : null;
        if (mount != null) {
            double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
            double wx = mount.x * cosY - mount.z * sinY;
            double wz = mount.x * sinY + mount.z * cosY;
            turret.setPos(
                    cloneOwner.getX() + wx,
                    cloneOwner.getY() + mount.y,
                    cloneOwner.getZ() + wz);
        } else {
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            boolean isLeftHanded = cloneOwner.getMainArm() == HumanoidArm.LEFT;
            double mainHandSide = isLeftHanded ? -1.0 : 1.0;
            double side = (slotIdx == 2) ? mainHandSide : -mainHandSide;
            turret.setPos(
                    cloneOwner.getX() + rightX * side * 1.0 + forwardX * 1.2,
                    cloneOwner.getEyeY() + 0.3,
                    cloneOwner.getZ() + rightZ * side * 1.0 + forwardZ * 1.2);
        }

        if (turret.attackTimer > 0) {
            // 蓄力末段的砲口集氣特效
            if (turret.attackTimer <= 15 && turret.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        turret.getX(), turret.getY(), turret.getZ(),
                        2, 0.08, 0.08, 0.08, 0.01);
            }
            turret.attackTimer--;
        } else {
            LivingEntity target = cloneTarget();
            if (target != null && turret.level() instanceof ServerLevel sl) {
                FloatingTurretProjectile proj = FloatingTurretProjectile.shootAt(
                        sl, cloneOwner, turret.position(), target.getBoundingBox().getCenter(), 1.0F);
                proj.setNoBlockDamage(true);
                sl.addFreshEntity(proj);
                turret.attackTimer = CLONE_HAND_CHARGE_COOLDOWN;
            }
        }
    }

    @Nullable
    private LivingEntity cloneTarget() {
        if (cloneOwner instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            return mob.getTarget();
        }
        return turret.level().getNearestPlayer(turret, FloatingTurretEntity.PASSIVE_RANGE);
    }

    /** hurt 處理結果。 */
    record HurtResult(boolean blocked, float adjustedAmount) {
        static final HurtResult BLOCK = new HurtResult(true, 0f);
    }
}
