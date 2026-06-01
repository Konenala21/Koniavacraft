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
 * Each effect aspect ({@link SkillRole#EFFECT}) maps to its OWN op (see
 * {@link SkillCompiler#opFor}); no two aspects share behavior, so combining
 * different aspects always plays differently. The carrier applies the ops where
 * it lands. Base damage is the carrier's; ops are the "extra" status payloads.
 *
 * {@code caster} is the shooter (may be null), {@code dir} is the impact direction
 * (for knockback), and {@code power} is the damage that was dealt.
 */
public enum SkillEffectOp {

    // ── Fire family: all burn, each with a different twist ──────────────────
    /** 燃素: raw, intense burn. */
    PHLOGISTON {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks((int) (80 + power * 20));
        }
    },
    /** 熔岩: burn and erupt the target upward. */
    MAGMA {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks(60);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.55, 0.0));
            target.hurtMarked = true;
        }
    },
    /** 離 (fire trigram): burn and brand the target with a revealing glow. */
    LI {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks(60);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
    },
    /** 爐火: a long, smelting burn. */
    FURNACE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks((int) (200 + power * 30));
        }
    },
    /** 蒸騰: burn plus a wave of heat that slows. */
    VAPOR {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks(50);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        }
    },
    /** 蒸汽: a scalding cloud that burns and blinds. */
    STEAM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks(40);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
        }
    },

    // ── Cold / nature ───────────────────────────────────────────────────────
    /** 霜: deep chill, slows and freezes. */
    FROST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 140));
        }
    },
    /** 生長: roots the target in place (also the binding/gen modifier payload). */
    ROOT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5)); // near-immobile
        }
    },
    /** 毒: a strong, lingering poison. */
    VENOM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
        }
    },
    /** 腐蝕: weakens and eats away with acid. */
    CORROSION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 1.0F + power * 0.1F);
        }
    },
    /** 飢荒: starves the target, sapping hunger and strength. */
    FAMINE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    },

    // ── Electric ────────────────────────────────────────────────────────────
    /** 震 (thunder trigram): calls down a real lightning bolt. */
    ZHEN {
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
    /** 電弧: a quick electric shock that briefly stuns. */
    ARC {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 3));
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().lightningBolt(), 1.0F + power * 0.15F);
        }
    },

    // ── Force ───────────────────────────────────────────────────────────────
    /** 風暴: a powerful blast that hurls the target away. */
    STORM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.knockback(1.0 + power * 0.05, -dir.x, -dir.z); // away from caster
        }
    },
    /** 坎 (abyss trigram): drags the target back toward the caster. */
    KAN {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.knockback(0.6 + power * 0.04, dir.x, dir.z); // toward caster (reversed)
        }
    },
    /** 兌 (lake trigram): launches the target straight up. */
    DUI {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.6 + power * 0.02, 0.0));
            target.hurtMarked = true;
        }
    },

    // ── Light ─────────────────────────────────────────────────────────────--
    /** 光輝: a searing flash that blinds and reveals. */
    RADIANCE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
    },

    // ── Dark / death family: each a different kind of decay ──────────────────
    /** 死: raw wither. */
    DEATH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        }
    },
    /** 亡靈: rot, wither plus a wasting hunger. */
    UNDEAD {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0));
        }
    },
    /** 污染: corruption, poison fused with wither. */
    TAINT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
    },
    /** 異界: maddening, darkness and nausea. */
    ELDRITCH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0));
        }
    },
    /** 靈魂: a soul drag, wither plus slow plus a glow. */
    SPIRITUS {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        }
    },
    /** 虛空: void pull, lifts the target off the ground. */
    VOID {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 1.0F + power * 0.15F);
        }
    },

    // ── Life family: heals, each a different kind ────────────────────────────
    /** 血氣: drains the target's life to the caster. */
    LIFESTEAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) caster.heal(Math.max(1.0F, power * 0.3F));
        }
    },
    /** 生命: an instant heal (on the FLIGHT self-cast, that is the caster). */
    VITALITY {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(4.0F + power * 0.25F);
        }
    },
    /** 修補: steady regeneration over time. */
    MENDING {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        }
    },
    /** 生命流: a small heal plus an absorption shield. */
    LIFEFLOW {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(2.0F + power * 0.15F);
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
        }
    },
    /** 滋養: nourishing regeneration and saturation. */
    NOURISH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0));
        }
    },

    // ── Bonus damage family: each a different flavour of extra hit ────────────
    /** 水晶: sharp crystalline shards, bonus magic damage. */
    CRYSTAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + power * 0.2F);
        }
    },
    /** 能量: an energized burst, bonus damage plus a glow mark. */
    ENERGY {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + power * 0.2F);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
        }
    },
    /** 奧法: a heavier arcane detonation. */
    ARCANA {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 3.0F + power * 0.25F);
        }
    };

    public abstract void apply(ServerLevel level, LivingEntity target,
                               @Nullable LivingEntity caster, Vec3 dir, float power);
}
