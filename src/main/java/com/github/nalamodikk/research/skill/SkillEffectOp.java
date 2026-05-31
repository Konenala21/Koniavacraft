package com.github.nalamodikk.research.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A concrete on-hit payload produced by an effect aspect during skill compilation.
 *
 * The combination's effect aspects ({@link SkillRole#EFFECT}) are mapped to these
 * ops by {@link SkillCompiler}; the carrier applies them when it lands. Base damage
 * is handled by the carrier itself, so ops are the "extra" status payloads.
 *
 * {@code caster} is the shooter (may be null), {@code dir} is the impact direction
 * (for knockback), and {@code power} is the damage that was dealt.
 */
public enum SkillEffectOp {
    FIRE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks((int) (60 + power * 20));
        }
    },
    FROST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    },
    POISON {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
    },
    WEAKEN {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    },
    LIGHTNING {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true); // visual only: no block fire/grief; we deal the damage
                if (caster instanceof ServerPlayer sp) bolt.setCause(sp);
                level.addFreshEntity(bolt);
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().lightningBolt(), 2.0F + power * 0.25F);
        }
    },
    KNOCKBACK {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            double strength = 0.6 + power * 0.04;
            target.knockback(strength, -dir.x, -dir.z); // push along the impact direction
        }
    },
    BLIND {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
        }
    },
    WITHER {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        }
    },
    ROOT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5)); // near-immobile
        }
    },
    LIFESTEAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) caster.heal(Math.max(1.0F, power * 0.3F));
        }
    },
    HEAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            // heals whoever it lands on; with the FLIGHT self-cast that is the caster
            target.heal(4.0F + power * 0.25F);
        }
    },
    SHARD {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + power * 0.2F); // bonus crystalline damage
        }
    };

    public abstract void apply(ServerLevel level, LivingEntity target,
                               @Nullable LivingEntity caster, Vec3 dir, float power);
}
