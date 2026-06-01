package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.network.packet.client.turret.TurretHitPacket;
import com.github.nalamodikk.register.ModDamageTypes;
import com.github.nalamodikk.register.ModEntities;
import com.github.nalamodikk.research.skill.SkillEffectOp;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
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
import net.minecraft.world.level.ClipContext;
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
    private boolean noBlockDamage = false;

    // 控制彈酬載：命中時套用效果而非造成傷害
    @org.jetbrains.annotations.Nullable
    private net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> controlEffect = null;
    private int controlDuration = 0;

    // 技能酬載：MACHINE 載體用。玩家用本源組合射出的浮游砲子彈，帶自訂傷害 + 命中效果 ops。
    // 預設空 / 0，所以一般浮游砲行為完全不變。
    private List<SkillEffectOp> skillOps = List.of();
    private float skillDamage = 0.0F;

    public FloatingTurretProjectile setSkillPayload(float damage, List<SkillEffectOp> ops) {
        this.skillDamage = damage;
        this.skillOps = ops;
        return this;
    }

    public FloatingTurretProjectile(EntityType<? extends FloatingTurretProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public FloatingTurretProjectile setNoBlockDamage(boolean value) {
        this.noBlockDamage = value;
        return this;
    }

    public FloatingTurretProjectile setControl(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int durationTicks) {
        this.controlEffect = effect;
        this.controlDuration = durationTicks;
        return this;
    }

    // 控制彈：從砲管朝目標，命中套效果不造成傷害
    public static FloatingTurretProjectile shootControl(Level level, net.minecraft.world.entity.LivingEntity owner,
                                                        Vec3 spawnPos, Vec3 targetPos,
                                                        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                                        int durationTicks) {
        FloatingTurretProjectile p = shootAt(level, owner, spawnPos, targetPos, 0.0F);
        p.setControl(effect, durationTicks);
        return p;
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
    // owner 用 LivingEntity，讓分身（非玩家）也能當砲的擁有者
    public static FloatingTurretProjectile shootAt(Level level, net.minecraft.world.entity.LivingEntity owner, Vec3 spawnPos, Vec3 targetPos) {
        return shootAt(level, owner, spawnPos, targetPos, 0.0F);
    }

    // 帶蓄力比例的版本（分身繞行砲偶爾發蓄力彈）
    public static FloatingTurretProjectile shootAt(Level level, net.minecraft.world.entity.LivingEntity owner, Vec3 spawnPos, Vec3 targetPos, float chargeRatio) {
        FloatingTurretProjectile p = new FloatingTurretProjectile(
                ModEntities.FLOATING_TURRET_PROJECTILE.get(), level);
        p.setOwner(owner);
        p.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        Vec3 dir = targetPos.subtract(spawnPos);
        p.setDeltaMovement((dir.lengthSqr() < 0.001 ? new Vec3(0, 1, 0) : dir.normalize()).scale(BOLT_SPEED));
        p.setChargeRatio(chargeRatio);
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

        // 方塊 raycast：用 COLLIDER 形狀，忽略草/花等無碰撞箱方塊
        HitResult blockHit = shooter.level().clip(new ClipContext(
                eyePos, farPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
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

        // 控制彈：套用效果（boss 免疫 / 同種不疊加由 helper 處理），不造成傷害
        if (controlEffect != null) {
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                com.github.nalamodikk.common.entity.control.TurretControlHelper.applyControl(
                        living, controlEffect, controlDuration);
            }
            showHitEffect(result.getLocation());
            this.discard();
            return;
        }

        float ratio = getChargeRatio();
        float dmg = skillDamage > 0 // 技能彈用自訂傷害；否則原生普通/蓄力傷害
                ? skillDamage
                : (ratio > 0
                    ? CHARGED_DAMAGE_MIN + (CHARGED_DAMAGE_MAX - CHARGED_DAMAGE_MIN) * ratio
                    : DAMAGE);
        DamageSource source = new DamageSource(
                level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageTypes.FLOATING_TURRET),
                this, getOwner());
        target.invulnerableTime = 0; // bypass hurt cooldown so dual-wield hits both register
        target.hurt(source, dmg);
        // 技能 ops：把組合本源的命中效果套在目標上（一般浮游砲 skillOps 為空，不影響）
        if (!skillOps.isEmpty() && target instanceof LivingEntity living && level() instanceof ServerLevel sl) {
            Vec3 d = getDeltaMovement().lengthSqr() > 1.0E-4 ? getDeltaMovement().normalize() : new Vec3(0, 0, 1);
            LivingEntity casterLE = getOwner() instanceof LivingEntity le ? le : null;
            for (SkillEffectOp op : skillOps) op.applyTo(sl, living, casterLE, d, dmg);
        }
        showHitEffect(result.getLocation());
        explodeIfCharged(result.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) return;
        BlockState state = level().getBlockState(result.getBlockPos());
        if (!state.getFluidState().isEmpty()) return;
        if (state.getCollisionShape(level(), result.getBlockPos()).isEmpty()) return;
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

        // 普通彈才送衝擊波環（蓄力彈有爆炸，不需要額外的環）
        if (ratio == 0F) {
            PacketDistributor.sendToPlayersNear(sl, null, pos.x, pos.y, pos.z, 64.0,
                    new TurretHitPacket(pos.x, pos.y, pos.z, 0F));
        }
    }

    private void explodeIfCharged(Vec3 pos) {
        float ratio = getChargeRatio();
        if (ratio > 0) {
            float radius = 1.0F + ratio * 1.5F;
            Level.ExplosionInteraction interaction = noBlockDamage
                    ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.BLOCK;
            level().explode(this, pos.x, pos.y, pos.z, radius, false, interaction);
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
