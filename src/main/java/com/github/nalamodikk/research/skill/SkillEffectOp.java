package com.github.nalamodikk.research.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * A concrete on-hit payload produced by an effect aspect during skill compilation.
 *
 * The combination's effect aspects ({@link SkillRole#EFFECT}) are mapped to these
 * ops by {@link SkillCompiler}; the carrier applies them when it lands. Base damage
 * is handled by the carrier itself, so ops are the "extra" status payloads.
 */
public enum SkillEffectOp {
    FIRE {
        @Override public void apply(ServerLevel level, LivingEntity target, float power) {
            target.setRemainingFireTicks((int) (60 + power * 20));
        }
    },
    FROST {
        @Override public void apply(ServerLevel level, LivingEntity target, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    },
    POISON {
        @Override public void apply(ServerLevel level, LivingEntity target, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
    },
    WEAKEN {
        @Override public void apply(ServerLevel level, LivingEntity target, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    };

    public abstract void apply(ServerLevel level, LivingEntity target, float power);
}
