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
    private List<SkillEffectOp> ops = List.of();
    private final Set<Integer> alreadyHit = new HashSet<>();
    private int lifetime = 0;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /** Fire a spell projectile along {@code dir} from the shooter's eyes. */
    public static SpellProjectileEntity shoot(Level level, Player shooter, Vec3 dir,
                                              float damage, List<SkillEffectOp> ops, boolean pierce) {
        SpellProjectileEntity p = new SpellProjectileEntity(ModEntities.SPELL_PROJECTILE.get(), level);
        p.damage = damage;
        p.ops = List.copyOf(ops);
        p.pierce = pierce;
        p.setOwner(shooter);

        Vec3 d = dir.lengthSqr() < 1.0E-6 ? shooter.getLookAngle() : dir.normalize();
        Vec3 spawn = shooter.getEyePosition().add(d.scale(0.6));
        p.setPos(spawn.x, spawn.y, spawn.z);
        p.setDeltaMovement(d.scale(SPEED));
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
        if (pierce && !alreadyHit.add(target.getId())) return; // pierce: hit each entity once

        LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
        DamageSource source = damageSources().mobProjectile(this, owner);
        target.invulnerableTime = 0;
        target.hurt(source, damage);

        if (target instanceof LivingEntity living && level() instanceof ServerLevel sl) {
            Vec3 dir = getDeltaMovement().lengthSqr() < 1.0E-6 ? Vec3.ZERO : getDeltaMovement().normalize();
            for (SkillEffectOp op : ops) op.apply(sl, living, owner, dir, damage);
        }

        burst(result.getLocation());
        if (!pierce) discard();
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
