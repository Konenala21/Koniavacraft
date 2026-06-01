package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.register.ModEntities;
import com.github.nalamodikk.research.skill.SkillEffectOp;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic spell projectile fired by aspect skills.
 *
 * Invisible (NoopRenderer); visuals are server-spawned particles. Carries a base
 * damage plus a list of {@link SkillEffectOp} status payloads applied on hit, and
 * an optional pierce flag (from the BLADE modifier). Configured by
 * {@code SkillCompiler}; this stays the flashy-visual canvas (tick/burst).
 */
public class SpellProjectileEntity extends ThrowableProjectile {

    private static final double SPEED = 1.2;
    private static final int MAX_LIFETIME = 40; // ~48 blocks

    private float damage = 6.0F;
    private boolean pierce = false;
    private boolean homing = false;
    private int chainCount = 0;
    private double speed = SPEED;
    private int maxLifetime = MAX_LIFETIME;
    private float knockback = 0.0F;
    private List<SkillEffectOp> ops = List.of();
    private final Set<Integer> alreadyHit = new HashSet<>();
    private int lifetime = 0;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /** Fire a spell projectile along {@code dir} from the shooter's eyes. */
    public static SpellProjectileEntity shoot(Level level, Player shooter, Vec3 dir,
                                              float damage, List<SkillEffectOp> ops, boolean pierce,
                                              boolean homing, int chainCount,
                                              double speed, int maxLifetime, float knockback) {
        SpellProjectileEntity p = new SpellProjectileEntity(ModEntities.SPELL_PROJECTILE.get(), level);
        p.damage = damage;
        p.ops = List.copyOf(ops);
        p.pierce = pierce;
        p.homing = homing;
        p.chainCount = chainCount;
        p.speed = speed;
        p.maxLifetime = maxLifetime;
        p.knockback = knockback;
        p.setOwner(shooter);

        Vec3 d = dir.lengthSqr() < 1.0E-6 ? shooter.getLookAngle() : dir.normalize();
        Vec3 spawn = shooter.getEyePosition().add(d.scale(0.6));
        p.setPos(spawn.x, spawn.y, spawn.z);
        p.setDeltaMovement(d.scale(speed));
        return p;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // no synched data; visuals are particle-driven
    }

    // ── Visuals (flashy playground) ─────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (homing && level() instanceof ServerLevel) steerToward(findHomingTarget());

        if (level() instanceof ServerLevel sl) {
            Vec3 p = position();
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.01);
            sl.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }

        if (++lifetime > maxLifetime) discard();
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
        if (pierce && !alreadyHit.add(target.getId())) return; // pierce: hit each entity once

        LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
        DamageSource source = damageSources().mobProjectile(this, owner);
        target.invulnerableTime = 0;
        target.hurt(source, damage);

        if (target instanceof LivingEntity living && level() instanceof ServerLevel sl) {
            Vec3 dir = getDeltaMovement().lengthSqr() < 1.0E-6 ? Vec3.ZERO : getDeltaMovement().normalize();
            for (SkillEffectOp op : ops) op.applyTo(sl, living, owner, dir, damage);
            if (knockback > 0.0F) living.knockback(knockback, -dir.x, -dir.z); // carrier impact (momentum/wind)
            if (chainCount > 0) chainFrom(living, owner, source, dir, sl);
        }

        burst(result.getLocation());
        if (!pierce) discard();
    }

    /** Arc to nearby enemies (ARC / PROPAGATION modifier), dealing reduced damage + ops. */
    private void chainFrom(LivingEntity from, LivingEntity owner, DamageSource source, Vec3 dir, ServerLevel sl) {
        List<LivingEntity> near = sl.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(5.0),
                e -> e != from && e != getOwner() && e.isAlive() && !alreadyHit.contains(e.getId()));
        int jumps = Math.min(chainCount, near.size());
        for (int i = 0; i < jumps; i++) {
            LivingEntity e = near.get(i);
            alreadyHit.add(e.getId());
            e.invulnerableTime = 0;
            e.hurt(source, damage * 0.5F);
            for (SkillEffectOp op : ops) op.applyTo(sl, e, owner, dir, damage * 0.5F);
            Vec3 a = from.getEyePosition();
            Vec3 b = e.getEyePosition();
            for (int s = 0; s <= 6; s++) {
                Vec3 m = a.lerp(b, s / 6.0);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.x, m.y, m.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Nullable
    private LivingEntity findHomingTarget() {
        if (!(level() instanceof ServerLevel sl)) return null;
        Vec3 vel = getDeltaMovement();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12.0),
                e -> e != getOwner() && e.isAlive())) {
            Vec3 to = e.getEyePosition().subtract(position());
            if (to.dot(vel) <= 0) continue;     // only steer toward things ahead
            double d = to.lengthSqr();
            if (d < bestDist) { bestDist = d; best = e; }
        }
        return best;
    }

    private void steerToward(@Nullable LivingEntity target) {
        if (target == null) return;
        Vec3 to = target.getEyePosition().subtract(position()).normalize();
        Vec3 steered = getDeltaMovement().normalize().scale(0.82).add(to.scale(0.18)).normalize().scale(speed);
        setDeltaMovement(steered);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) return;
        BlockState state = level().getBlockState(result.getBlockPos());
        if (!state.getFluidState().isEmpty()) return;
        if (state.getCollisionShape(level(), result.getBlockPos()).isEmpty()) return;
        burst(result.getLocation());
        discard(); // a wall stops even piercing shots
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
