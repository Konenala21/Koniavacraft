package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Generic spell projectile fired by aspect skills.
 *
 * The entity itself is invisible (registered with NoopRenderer); all visuals
 * are server-spawned particles in {@link #tick()} (trail) and {@link #burst}
 * (impact). This is the canvas for the "flashy" pass: restyle the trail/burst
 * here without touching the skill plumbing.
 */
public class SpellProjectileEntity extends ThrowableProjectile {

    private static final double SPEED = 1.2;
    private static final int MAX_LIFETIME = 40; // ~48 blocks

    private float damage = 6.0F;
    private int fireSeconds = 4;
    private int lifetime = 0;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /** Fire a spell projectile from the shooter's eyes along their look direction. */
    public static SpellProjectileEntity shoot(Level level, Player shooter, float damage, int fireSeconds) {
        SpellProjectileEntity p = new SpellProjectileEntity(ModEntities.SPELL_PROJECTILE.get(), level);
        p.damage = damage;
        p.fireSeconds = fireSeconds;
        p.setOwner(shooter);

        Vec3 look = shooter.getLookAngle();
        Vec3 spawn = shooter.getEyePosition().add(look.scale(0.6));
        p.setPos(spawn.x, spawn.y, spawn.z);
        p.setDeltaMovement(look.scale(SPEED));
        return p;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // no synched data; visuals are particle-driven
    }

    // ── Visuals (your flashy playground) ────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (level() instanceof ServerLevel sl) {
            Vec3 p = position();
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.01);
            sl.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }

        if (++lifetime > MAX_LIFETIME) discard();
    }

    private void burst(Vec3 pos) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 24, 0.25, 0.25, 0.25, 0.08);
        sl.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 6, 0.2, 0.2, 0.2, 0.0);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    // ── Impact ──────────────────────────────────────────────────────────────

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        if (target == getOwner()) return;

        LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
        DamageSource source = damageSources().mobProjectile(this, owner);
        target.invulnerableTime = 0;
        target.hurt(source, damage);
        if (fireSeconds > 0) target.setRemainingFireTicks(fireSeconds * 20);

        burst(result.getLocation());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) return;
        BlockState state = level().getBlockState(result.getBlockPos());
        if (!state.getFluidState().isEmpty()) return;
        if (state.getCollisionShape(level(), result.getBlockPos()).isEmpty()) return;
        burst(result.getLocation());
        discard();
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
