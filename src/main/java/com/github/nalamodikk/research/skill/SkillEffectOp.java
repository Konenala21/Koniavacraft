package com.github.nalamodikk.research.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
    /** 能量: an energetic detonation. A no-grief explosion (entity damage + knockback,
     *  no block breaking) whose radius scales with power, so stacking amplifiers turns
     *  a grenade into a nuke. Any carrier + energy becomes its explosive variant. */
    ENERGY {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float radius = Math.min(7.0F, 2.0F + power * 0.06F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
        }
    },
    /** 奧法: a heavier arcane detonation. */
    ARCANA {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 3.0F + power * 0.25F);
        }
    },

    // ── Abstract aspects: imaginative effects, several with combo synergy ─────
    /** 挖掘: armor shred. Armor-bypassing damage plus mining fatigue (can't dig or swing well). */
    EXCAVATION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + power * 0.3F); // magic bypasses armor
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 2));
        }
    },
    /** 獸性: a savage maul. Heavy physical bonus hit plus a deep bleeding poison. */
    BESTIA {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(caster != null ? level.damageSources().mobAttack(caster) : level.damageSources().magic(), 3.0F + power * 0.3F);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 2)); // bleed
        }
    },
    /** 收割: a reaping sweep. Damages every living thing around the target (scythe AoE). */
    HARVEST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.invulnerableTime = 0;
                le.hurt(level.damageSources().magic(), 2.0F + power * 0.25F);
            }
        }
    },
    /** 感知: expose. Long glow that reveals through walls, plus a tracking slow. */
    SENSUS {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
    },
    /** 認知: mind scramble. Nausea plus weakness as the target loses its grip. */
    COGNITION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
    },
    /** 慾望: lure. Drags the target toward the caster and slows it (drawn in by desire). */
    DESIRE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null) {
                Vec3 pull = caster.position().subtract(target.position());
                if (pull.lengthSqr() > 0.01) {
                    target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(0.6)));
                    target.hurtMarked = true;
                }
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    },
    /** 法則: judgment. Roots the target and the strike lands harder the more debuffs it
     *  already carries, so it executes a target you have stacked status effects onto. */
    LAW {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            long debuffs = target.getActiveEffects().stream()
                    .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    .count();
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + debuffs * 2.0F + power * 0.2F);
        }
    },
    /** 商貿: plunder. Bonus damage that also nets the caster a profit shield (absorption). */
    COMMERCE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 1.5F + power * 0.15F);
            if (caster != null && caster.isAlive()) {
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
            }
        }
    },
    /** 語言: a word of weakness. A binding curse: weakness plus mining fatigue, no damage. */
    LANGUAGE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1));
        }
    },
    /** 原初: mutation. A different random debuff every cast, chaotic and unpredictable. */
    PRIMORDIAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            switch (level.getRandom().nextInt(5)) {
                case 0 -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                case 1 -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                case 2 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                case 3 -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                default -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 0));
            }
        }
    },
    /** 財富: a Midas strike. The single heaviest flat bonus hit, raw greed. */
    WEALTH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 4.0F + power * 0.4F);
        }
    },

    // ── Reaction outputs: emergent results when two reacting aspects combine ──
    // These are produced by SkillCompiler's reaction table, never mapped from a
    // single aspect. They are the "chemistry" payoff of combining aspects.
    /** 熱裂 (fire + frost): rapid temperature shock, a heavy burst hit. */
    THERMAL_SHOCK {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 6.0F + power * 0.4F);
        }
    },
    /** 爆燃 (fire + energy): a larger fiery explosion that ignites the target. */
    COMBUSTION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float radius = Math.min(8.0F, 3.0F + power * 0.07F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
            target.setRemainingFireTicks(80);
        }
    },
    /** 毒燃 (fire + venom): a burning toxic cloud, poison and fire on everything nearby. */
    TOXIC_BURN {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                le.setRemainingFireTicks(60);
            }
        }
    },
    /** 碎冰 (frost + force): the chilled target is shattered for a heavy burst. */
    SHATTER {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 5.0F + power * 0.35F);
        }
    },
    /** 超載 (energy + crystal/arcana): an overcharged detonation, a much bigger blast. */
    OVERLOAD {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float radius = Math.min(9.0F, 4.0F + power * 0.08F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
        }
    },
    /** 死亡虹吸 (death + lifesteal): the wither feeds the caster a large heal. */
    DEATH_SIPHON {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) caster.heal(Math.max(2.0F, power * 0.6F));
        }
    },
    /** 野火 (growth/root + fire): the binding ignites and spreads fire to everything around. */
    WILDFIRE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.setRemainingFireTicks(100);
                le.invulnerableTime = 0;
                le.hurt(level.damageSources().magic(), 1.0F + power * 0.15F);
            }
        }
    },
    /** 強酸 (venom + corrosion): concentrated acid, armor-melt plus deep poison. */
    STRONG_ACID {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 3.0F + power * 0.2F);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
        }
    },
    /** 湮滅 (radiance + dark): light and dark collide, a massive burst of damage. */
    ANNIHILATION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 7.0F + power * 0.45F);
        }
    };

    public abstract void apply(ServerLevel level, LivingEntity target,
                               @Nullable LivingEntity caster, Vec3 dir, float power);
}
