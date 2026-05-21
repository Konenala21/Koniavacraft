package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.network.packet.client.turret.TurretHitPacket;
import com.github.nalamodikk.register.ModDamageTypes;
import com.github.nalamodikk.register.ModEntities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FloatingTurretProjectile extends ThrowableProjectile {

    // 普通彈
    public static final float DAMAGE = 8.0F;
    // 雙持蓄力彈
    public static final float CHARGED_DAMAGE_MIN = 16.0F;
    public static final float CHARGED_DAMAGE_MAX = 24.0F;

    private static final double BOLT_SPEED = 1.5;
    private static final int MAX_LIFETIME = 32; // ~48 格

    private static final EntityDataAccessor<Float> CHARGE_RATIO_DATA =
            SynchedEntityData.defineId(FloatingTurretProjectile.class, EntityDataSerializers.FLOAT);

    private int lifetime = 0;

    public FloatingTurretProjectile(EntityType<? extends FloatingTurretProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_RATIO_DATA, 0.0F);
    }

    // ── 工廠方法 ────────────────────────────────────────────────────────────────

    private static final double AIM_RANGE = 48.0;

    public static FloatingTurretProjectile shoot(Level level, Player shooter, Vec3 spawnPos) {
        return create(level, shooter, 0.0F, spawnPos);
    }

    public static FloatingTurretProjectile shootCharged(Level level, Player shooter, float chargeRatio, Vec3 spawnPos) {
        return create(level, shooter, chargeRatio, spawnPos);
    }

    // 裝備槽被動攻擊用：從砲管直接瞄準目標位置，不走玩家視線 raycast
    public static FloatingTurretProjectile shootAt(Level level, Player owner, Vec3 spawnPos, Vec3 targetPos) {
        FloatingTurretProjectile p = new FloatingTurretProjectile(
                ModEntities.FLOATING_TURRET_PROJECTILE.get(), level);
        p.setOwner(owner);
        p.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        Vec3 dir = targetPos.subtract(spawnPos);
        p.setDeltaMovement((dir.lengthSqr() < 0.001 ? new Vec3(0, 1, 0) : dir.normalize()).scale(BOLT_SPEED));
        p.setChargeRatio(0.0F);
        return p;
    }

    private static FloatingTurretProjectile create(Level level, Player shooter, float chargeRatio, Vec3 spawnPos) {
        FloatingTurretProjectile p = new FloatingTurretProjectile(
                ModEntities.FLOATING_TURRET_PROJECTILE.get(), level);
        p.setOwner(shooter);
        p.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        // Raycast 找到準心實際命中點，再從砲管位置朝那個點計算方向
        // 這樣視覺上子彈從砲管出發，準心也對準目標
        Vec3 direction = calcAimDirection(shooter, spawnPos);
        p.setDeltaMovement(direction.scale(BOLT_SPEED));
        p.setChargeRatio(chargeRatio);
        return p;
    }

    private static Vec3 calcAimDirection(Player shooter, Vec3 spawnPos) {
        Vec3 eyePos = shooter.getEyePosition();
        Vec3 look   = shooter.getLookAngle();
        Vec3 farPos = eyePos.add(look.scale(AIM_RANGE));

        // 方塊 raycast
        HitResult blockHit = shooter.pick(AIM_RANGE, 1.0F, false);
        Vec3 blockTarget = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation() : farPos;

        // 實體 raycast
        AABB searchBox = new AABB(eyePos, farPos).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                shooter, eyePos, farPos, searchBox,
                e -> !e.isSpectator() && e.isPickable() && e != shooter, AIM_RANGE * AIM_RANGE);
        Vec3 entityTarget = entityHit != null ? entityHit.getLocation() : farPos;

        // 取最近的命中點
        Vec3 target = eyePos.distanceToSqr(entityTarget) < eyePos.distanceToSqr(blockTarget)
                ? entityTarget : blockTarget;

        // 從砲管往目標點方向（若砲管幾乎在目標上則 fallback 用視線方向）
        Vec3 dir = target.subtract(spawnPos);
        return dir.lengthSqr() < 0.001 ? look : dir.normalize();
    }

    // ── 命中處理 ─────────────────────────────────────────────────────────────────

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        if (target == getOwner()) return;

        float ratio = getChargeRatio();
        float dmg = ratio > 0
                ? CHARGED_DAMAGE_MIN + (CHARGED_DAMAGE_MAX - CHARGED_DAMAGE_MIN) * ratio
                : DAMAGE;
        DamageSource source = new DamageSource(
                level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageTypes.FLOATING_TURRET),
                this, getOwner());
        target.hurt(source, dmg);
        showHitEffect(result.getLocation());
        explodeIfCharged(result.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) return;
        BlockState state = level().getBlockState(result.getBlockPos());
        if (!state.getFluidState().isEmpty()) return; // 液體直接穿過
        showHitEffect(result.getLocation());
        explodeIfCharged(result.getLocation());
        this.discard();
    }

    private void showHitEffect(Vec3 pos) {
        if (!(level() instanceof ServerLevel sl)) return;
        float ratio = getChargeRatio();

        // 閃白光
        sl.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

        // 藍白碎光向外爆散
        int rodCount = ratio > 0 ? 24 : 12;
        sl.sendParticles(ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z, rodCount, 0.2, 0.2, 0.2, 0.25);

        // 藍色電弧火花
        int sparkCount = ratio > 0 ? 20 : 8;
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z, sparkCount, 0.15, 0.15, 0.15, 0.1);

        // 命中音效：短促清脆的「啪！」
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS,
                ratio > 0 ? 1.2F : 0.7F,
                ratio > 0 ? 1.6F : 2.0F);

        // 通知附近 client 播放衝擊波特效
        PacketDistributor.sendToPlayersNear(sl, null, pos.x, pos.y, pos.z, 64.0,
                new TurretHitPacket(pos.x, pos.y, pos.z, ratio));
    }

    private void explodeIfCharged(Vec3 pos) {
        float ratio = getChargeRatio();
        if (ratio > 0) {
            float radius = 1.0F + ratio * 1.5F;
            level().explode(this, pos.x, pos.y, pos.z,
                    radius, false, Level.ExplosionInteraction.BLOCK);
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && ++lifetime > MAX_LIFETIME) discard();
    }

    // ── 雜項 ──────────────────────────────────────────────────────────────────

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldBeSaved() { return false; }

    public float getChargeRatio() {
        return entityData.get(CHARGE_RATIO_DATA);
    }

    public void setChargeRatio(float ratio) {
        entityData.set(CHARGE_RATIO_DATA, ratio);
    }
}
