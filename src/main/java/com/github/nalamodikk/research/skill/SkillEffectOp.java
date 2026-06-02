package com.github.nalamodikk.research.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.github.nalamodikk.common.entity.control.TurretControlHelper;
import com.github.nalamodikk.research.skill.reaction.ReactionFx;
import com.github.nalamodikk.register.ModMobEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A concrete on-hit payload produced by an effect aspect during skill compilation.
 *
 * Each effect aspect ({@link SkillRole#EFFECT}) maps to its OWN op (see
 * {@code SkillCompiler.opFor}); no two aspects share behavior, so combining
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
            Vec3 mv = target.getDeltaMovement();
            target.setDeltaMovement(mv.x, 0.9, mv.z); // strong eruption pop
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
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 80, 0);
        }
    },
    /** 蒸汽: a scalding cloud that burns and blinds. */
    STEAM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks(40);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
            TurretControlHelper.applySkillControl(target, ModMobEffects.DAZE, 50, 0); // 真致盲:怪丟失目標
        }
    },

    // ── Cold / nature ───────────────────────────────────────────────────────
    /** 霜: deep chill, slows and freezes. */
    FROST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 80, 2);
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 140));
        }
    },
    /** 生長: roots the target in place (also the binding/gen modifier payload).
     *  Uses the custom ROOT effect (zeroes horizontal velocity each tick), so it
     *  truly immobilizes even flying mobs, unlike vanilla slowness. */
    ROOT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, 60, 0);
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
    /** 電弧: a quick electric shock that briefly stuns. Uses ROOT so it actually
     *  locks the target down for a moment (reads as a stun), even on flying mobs. */
    ARC {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, 15, 0);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().lightningBolt(), 1.0F + power * 0.15F);
        }
    },

    // ── Force ───────────────────────────────────────────────────────────────
    /** 風暴: a powerful blast that hurls the target away. Direct velocity (not
     *  knockback) so knockback-resistance can't soften it. */
    STORM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            double s = 1.4 + power * 0.06;
            target.setDeltaMovement(away.x * s, 0.42, away.z * s);
            target.hurtMarked = true;
        }
    },
    /** 坎 (abyss trigram): yanks the target across the gap to the caster. The pull
     *  scales with distance so a far target actually arrives, not just nudges. */
    KAN {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 diff = caster != null ? caster.position().subtract(target.position()) : dir.scale(-1.0);
            if (diff.lengthSqr() < 1.0E-4) diff = dir.scale(-1.0);
            double dist = diff.length();
            Vec3 toCaster = diff.normalize();
            // far = stronger yank (covers the gap), capped so it isn't absurd
            double strength = Math.min(3.0, 0.8 + dist * 0.35);
            target.setDeltaMovement(toCaster.x * strength,
                    Math.max(target.getDeltaMovement().y, 0.0) + 0.25, toCaster.z * strength);
            target.hurtMarked = true;
        }
    },
    /** 兌 (lake trigram): launches the target straight up. */
    DUI {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 mv = target.getDeltaMovement();
            target.setDeltaMovement(mv.x, 1.1 + power * 0.02, mv.z); // strong launch upward
            target.hurtMarked = true;
        }
    },

    // ── Light ─────────────────────────────────────────────────────────────--
    /** 光輝: a searing flash that blinds and reveals. */
    RADIANCE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            TurretControlHelper.applySkillControl(target, ModMobEffects.DAZE, 60, 0); // 真致盲:怪丟失目標
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
            TurretControlHelper.applySkillControl(target, ModMobEffects.DAZE, 100, 0); // 對怪:混亂=丟失目標
        }
    },
    /** 靈魂: a soul drag, wither plus slow plus a glow. */
    SPIRITUS {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 60, 1);
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
            if (caster != null && caster.isAlive()) caster.heal(Math.max(1.0F, power * 0.4F)); // 吸血:傷害的 40%
        }
    },
    /** 生命: an instant heal (on the FLIGHT self-cast, that is the caster). */
    VITALITY {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(8.0F + power * 0.5F); // 瞬補:一大口
        }
    },
    /** 修補: steady regeneration over time. */
    MENDING {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(4.0F + power * 0.3F); // 即時補一口
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 2)); // 再生 III,持續強回
        }
    },
    /** 生命流: a small heal plus an absorption shield. */
    LIFEFLOW {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(5.0F + power * 0.35F);
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1)); // 補 + 吸收 II 護盾
        }
    },
    /** 滋養: nourishing regeneration and saturation. */
    NOURISH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1)); // 再生 II
            target.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0));
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
            target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 1)); // 自訂流血
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
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 60, 0);
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 120, 0)); // 自訂易傷:標記後受傷放大
        }
    },
    /** 認知: mind scramble. Nausea plus weakness as the target loses its grip. */
    COGNITION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            TurretControlHelper.applySkillControl(target, ModMobEffects.DAZE, 120, 0); // 對怪:心智干擾=丟失目標
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
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 60, 1);
        }
    },
    /** 法則: judgment. Roots the target and the strike lands harder the more debuffs it
     *  already carries, so it executes a target you have stacked status effects onto. */
    LAW {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            long debuffs = target.getActiveEffects().stream()
                    .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    .count();
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, 80, 0);
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
                case 2 -> TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 100, 3);
                case 3 -> TurretControlHelper.applySkillControl(target, ModMobEffects.DAZE, 80, 0);
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
    /** 熱裂 (fire + frost): rapid temperature shock, a heavy burst hit. Extra burst and
     *  extinguish if the target is burning (the sudden cooling cracks it). */
    THERMAL_SHOCK {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float bonus = 0.0F;
            if (target.isOnFire() || target.getRemainingFireTicks() > 0) {
                bonus = 4.0F + power * 0.4F; // 燃燒中急冷:額外爆發
                target.clearFire();
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 6.0F + power * 0.4F + bonus);
        }
    },
    /** 爆燃 (fire + energy): a larger fiery explosion that ignites the target. A poisoned
     *  target's toxin ignites for a bonus hit (the poison is consumed). */
    COMBUSTION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (target.hasEffect(MobEffects.POISON)) {
                target.removeEffect(MobEffects.POISON);
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().magic(), 4.0F + power * 0.4F); // 引爆毒氣
            }
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
    /** 碎冰 (frost + force): the chilled target is shattered. Massive bonus if it is
     *  chilled or frozen (the ice is consumed), rewarding cold-then-force combos. */
    SHATTER {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float bonus = 0.0F;
            if (target.hasEffect(ModMobEffects.CHILL) || target.getTicksFrozen() > 0) {
                bonus = 6.0F + power * 0.5F; // 暴擊冰凍目標
                target.removeEffect(ModMobEffects.CHILL);
                target.setTicksFrozen(0);
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 5.0F + power * 0.35F + bonus);
        }
    },
    /** 超載 (energy + crystal/arcana): an overcharged detonation, a much bigger blast.
     *  The blast bites deeper into a target whose armor is cracked (vulnerable). */
    OVERLOAD {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (target.hasEffect(ModMobEffects.VULNERABLE)) {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().magic(), 4.0F + power * 0.3F);
            }
            float radius = Math.min(9.0F, 4.0F + power * 0.08F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
        }
    },
    /** 死亡虹吸 (death + lifesteal): the wither feeds the caster a large heal, larger
     *  the more the target is already rotting (withered). */
    DEATH_SIPHON {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) {
                float heal = Math.max(2.0F, power * 0.6F);
                if (target.hasEffect(MobEffects.WITHER)) heal += 4.0F; // 凋零目標:虹吸更多
                caster.heal(heal);
            }
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
    /** 湮滅 (radiance + dark): light and dark collide, a massive burst that grows with
     *  every harmful effect already on the target (it annihilates the afflicted). */
    ANNIHILATION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            long debuffs = target.getActiveEffects().stream()
                    .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    .count();
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 7.0F + power * 0.45F + debuffs * 1.5F);
        }
    },

    // ── Reaction outputs, batch 2 ────────────────────────────────────────────
    /** 煉獄火 (dark + fire): cursed flame, a burst plus burn plus a touch of wither.
     *  The cursed flame feeds on a withering target for extra damage. */
    SOULFIRE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float bonus = target.hasEffect(MobEffects.WITHER) ? 4.0F + power * 0.35F : 0.0F;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 5.0F + power * 0.4F + bonus);
            target.setRemainingFireTicks(80);
            target.addEffect(new MobEffectInstance(ModMobEffects.SOULBURN, 100, 1)); // 自訂靈焰
        }
    },
    /** 凍毒 (frost + venom): cold preserves the venom, deep poison and a hard chill. */
    CRYO_VENOM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 2));
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 120, 3);
        }
    },
    /** 閃焰 (radiance + fire): a solar flare, blinding fire to everything nearby. */
    SOLAR_FLARE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                TurretControlHelper.applySkillControl(le, ModMobEffects.DAZE, 60, 0);
                le.setRemainingFireTicks(70);
            }
        }
    },
    /** 蔓生 (growth + water): roots spread, rooting everything around the target. */
    OVERGROWTH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                TurretControlHelper.applySkillControl(le, ModMobEffects.ROOT, 80, 0);
            }
        }
    },
    /** 晶爆 (crystal + force): the crystal shatters into shrapnel, hitting all nearby.
     *  Shards dig into bleeding targets for extra damage. */
    SHRAPNEL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                float bonus = le.hasEffect(ModMobEffects.BLEED) ? 2.0F + power * 0.2F : 0.0F;
                le.invulnerableTime = 0;
                le.hurt(level.damageSources().magic(), 2.0F + power * 0.2F + bonus);
            }
        }
    },

    // ── Three-aspect ultimate reactions (use all three effect slots) ─────────
    /** 元素風暴 (fire + frost + lightning): a heavy multi-element AoE burst. */
    ELEMENTAL_STORM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(4.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.invulnerableTime = 0;
                le.hurt(level.damageSources().magic(), 4.0F + power * 0.3F);
                le.setRemainingFireTicks(40);
                TurretControlHelper.applySkillControl(le, ModMobEffects.CHILL, 80, 2);
            }
        }
    },
    /** 熱核 (fire + energy + lightning): a thermonuclear blast, the biggest explosion. */
    THERMONUCLEAR {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float radius = Math.min(10.0F, 5.0F + power * 0.09F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
            target.setRemainingFireTicks(120);
        }
    },
    /** 生化浩劫 (venom + corrosion + fire): a wide toxic inferno cloud. */
    BIOHAZARD {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(4.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 2));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
                le.setRemainingFireTicks(60);
            }
            lingerCloud(level, target, new MobEffectInstance(MobEffects.POISON, 80, 1), 4.0F, 160); // 留毒火地形
        }
    },
    /** 聖核爆 (radiance + dark + energy): a holy nova, a massive burst to all nearby. */
    HOLY_NOVA {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(4.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                float bonus = le.getType().is(EntityTypeTags.UNDEAD) ? 4.0F + power * 0.3F : 0.0F; // 聖核爆剋不死
                le.invulnerableTime = 0;
                le.hurt(level.damageSources().magic(), 6.0F + power * 0.4F + bonus);
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                TurretControlHelper.applySkillControl(le, ModMobEffects.DAZE, 60, 0);
            }
        }
    },
    /** 腐化 (growth + fire + venom): blight, rooting fire-poison over an area. */
    BLIGHT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                TurretControlHelper.applySkillControl(le, ModMobEffects.ROOT, 80, 0);
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                le.setRemainingFireTicks(80);
            }
        }
    },
    /** 引力亂流 (storm + kan + dui): a maelstrom that yanks nearby foes upward and hurts. */
    MAELSTROM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 center = target.position();
            AABB box = target.getBoundingBox().inflate(4.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                Vec3 pull = center.subtract(le.position());
                le.setDeltaMovement(le.getDeltaMovement().add(pull.x * 0.15, 0.5, pull.z * 0.15));
                le.hurtMarked = true;
                le.invulnerableTime = 0;
                le.hurt(level.damageSources().magic(), 3.0F + power * 0.25F);
            }
        }
    },

    // ── Reaction outputs, batch 3 (chemistry engine) ─────────────────────────
    /** 蒸爆 (heat + water): a scalding steam burst, soulburn and launch to all nearby. */
    STEAM_BURST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(ModMobEffects.SOULBURN, 80, 0));
                le.setRemainingFireTicks(30);
                Vec3 mv = le.getDeltaMovement();
                le.setDeltaMovement(mv.x, 0.55, mv.z);
                le.hurtMarked = true;
            }
        }
    },
    /** 溶解 (acid + metal): dissolves armor, heavy vulnerability plus armor-bypass damage. */
    CORRODE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 160, 1));
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + power * 0.2F);
        }
    },
    /** 動能爆 (force + arcane): a concussive blast, explosion plus a hard launch.
     *  Hits a vulnerable (cracked-armor) target harder. */
    KINETIC_BLAST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (target.hasEffect(ModMobEffects.VULNERABLE)) {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().magic(), 3.0F + power * 0.25F);
            }
            float radius = Math.min(7.0F, 3.0F + power * 0.06F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            target.setDeltaMovement(away.x * 1.6, 0.6, away.z * 1.6);
            target.hurtMarked = true;
        }
    },
    /** 內爆 (force + dark): a singularity, pulls everything inward then detonates. */
    IMPLOSION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 center = target.position();
            AABB box = target.getBoundingBox().inflate(4.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                Vec3 pull = center.subtract(le.position());
                le.setDeltaMovement(le.getDeltaMovement().add(pull.x * 0.25, 0.1, pull.z * 0.25));
                le.hurtMarked = true;
            }
            vacuumItems(level, target, 4.5); // 奇點:把掉落物也吸進來
            level.explode(caster, center.x, center.y + target.getBbHeight() * 0.5, center.z,
                    Math.min(7.0F, 3.0F + power * 0.06F), Level.ExplosionInteraction.NONE);
        }
    },
    /** 毒爆 (force + venom): a venom blast, knocks back and spreads poison around. */
    TOXIC_BURST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                le.setDeltaMovement(away.x, 0.35, away.z);
                le.hurtMarked = true;
            }
        }
    },
    /** 磁暴 (electric + force): a magnetic vortex that drags nearby foes together (then chains). */
    MAGNETIC_STORM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 center = target.position();
            AABB box = target.getBoundingBox().inflate(4.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                Vec3 pull = center.subtract(le.position());
                if (pull.lengthSqr() > 0.01) {
                    le.setDeltaMovement(le.getDeltaMovement().add(pull.normalize().scale(0.45)));
                    le.hurtMarked = true;
                }
            }
            vacuumItems(level, target, 4.5); // 磁力:也吸金屬掉落物
        }
    },
    /** 怨雷 (electric + dark): spiteful lightning, a soul-burning shock (then chains).
     *  Conductive targets (wet or chilled) take a heavy bonus. */
    SPITE_BOLT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(ModMobEffects.SOULBURN, 100, 1));
            float bonus = (target.isInWaterOrRain() || target.hasEffect(ModMobEffects.CHILL)) ? 4.0F + power * 0.3F : 0.0F;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().lightningBolt(), 2.0F + power * 0.2F + bonus);
        }
    },
    /** 光電 (electric + light): a photoelectric flash, blinds and dazes (then chains). */
    PHOTOELECTRIC {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            TurretControlHelper.applySkillControl(target, ModMobEffects.DAZE, 60, 0);
        }
    },
    /** 光合 (light + organic): photosynthesis, heals the caster and cleanses their debuffs. */
    PHOTOSYNTHESIS {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) {
                caster.heal(6.0F + power * 0.4F);
                caster.getActiveEffects().stream()
                        .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                        .map(MobEffectInstance::getEffect).toList()
                        .forEach(caster::removeEffect);
            }
        }
    },
    /** 聖療 (light + life): a sanctifying heal, a large heal plus regeneration. No damage. */
    SANCTIFY {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(10.0F + power * 0.6F);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 2));
        }
    },
    /** 聖能爆 (light + arcane): a radiant surge, a blinding light explosion. Holy light
     *  sears the undead for a bonus. */
    RADIANT_SURGE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (target.getType().is(EntityTypeTags.UNDEAD)) {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().magic(), 4.0F + power * 0.35F); // 聖光剋不死
            }
            float radius = Math.min(8.0F, 3.5F + power * 0.07F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
    },
    /** 瘟疫 (dark + venom): a plague that spreads deep poison and wither to all nearby. */
    PLAGUE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(4.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 2));
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
            lingerCloud(level, target, new MobEffectInstance(MobEffects.POISON, 80, 1), 3.0F, 140); // 留病雲
        }
    },
    /** 腐生 (dark + organic): rotting bloom, wither spreading over an area. */
    ROT_BLOOM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
                le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0));
            }
        }
    },
    /** 虛能爆 (dark + arcane): a void surge, a heavy dark detonation plus wither. */
    VOID_SURGE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float radius = Math.min(8.0F, 3.5F + power * 0.07F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        }
    },
    /** 孢爆 (organic + venom): a spore burst, a spreading poison cloud that weakens armor. */
    SPORE_BURST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                le.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 120, 0));
            }
        }
    },
    /** 繁茂 (organic + life): flourishing growth, regeneration over time. No damage. */
    FLOURISH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(4.0F + power * 0.3F);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
    },
    /** 超導 (cold + electric): zero-resistance, a hard chill (then a big chain). */
    SUPERCONDUCT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 100, 3);
        }
    },
    /** 稜光 (cold + light): light refracts through ice, a blinding flash (then chains). */
    PRISM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0));
            TurretControlHelper.applySkillControl(target, ModMobEffects.DAZE, 70, 0);
        }
    },
    /** 凋寒 (cold + dark): wither frost, a deep chill plus wither bleed. */
    WITHER_FROST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 100, 2);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        }
    },
    /** 霜縛 (cold + organic): frost-bound vines, roots and chills the target. */
    FROSTBIND {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, 80, 0);
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 100, 2);
        }
    },
    /** 寒晶 (cold + metal): ice crystals burst, a sharp hit plus bleeding. Shatters a
     *  chilled/frozen target for a bonus too (consumes the ice). */
    CRYOCRYSTAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float bonus = 0.0F;
            if (target.hasEffect(ModMobEffects.CHILL) || target.getTicksFrozen() > 0) {
                bonus = 4.0F + power * 0.4F;
                target.setTicksFrozen(0);
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 4.0F + power * 0.3F + bonus);
            target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 1));
        }
    },
    /** 熔毀 (heat + acid): meltdown, ignites and melts armor (heavy vulnerability). An
     *  already-burning target melts down further, for a bonus hit. */
    MELTDOWN {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (target.isOnFire() || target.getRemainingFireTicks() > 0) {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().magic(), 3.0F + power * 0.3F); // 已燃燒:加劇熔毀
            }
            target.setRemainingFireTicks((int) (100 + power * 20));
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 160, 1));
        }
    },
    /** 噴發 (heat + force): a volcanic eruption, ignites and launches the target up and away. */
    ERUPTION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks(80);
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            target.setDeltaMovement(away.x * 1.0, 0.9, away.z * 1.0);
            target.hurtMarked = true;
        }
    },
    /** 過熱電漿 (steam + electric): superheated plasma, a heavy searing hit (chains further). */
    SUPERHEATED_PLASMA {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 6.0F + power * 0.45F);
            target.setRemainingFireTicks(80);
        }
    },
    /** 氫爆 (volatile + heat): the dissolved-metal gas ignites, an explosion. */
    HYDROGEN_BLAST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float radius = Math.min(9.0F, 4.0F + power * 0.08F);
            level.explode(caster, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    radius, Level.ExplosionInteraction.NONE);
        }
    },
    /** 灰燼風暴 (ash + force): blowing ash, a blinding choking cloud over an area. */
    ASH_STORM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(4.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                TurretControlHelper.applySkillControl(le, ModMobEffects.DAZE, 80, 0);
            }
            lingerCloud(level, target, new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), 4.0F, 120); // 留灰雲
        }
    },

    // ── Reaction outputs, batch 4 (fill the trait matrix) ────────────────────
    /** 熱離子 (heat + electric): superheated ions, ignite plus an electric jolt.
     *  Conductive targets (wet or chilled) take a bonus. */
    THERMION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks(60);
            float bonus = (target.isInWaterOrRain() || target.hasEffect(ModMobEffects.CHILL)) ? 3.0F + power * 0.25F : 0.0F;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().lightningBolt(), 2.0F + power * 0.2F + bonus);
        }
    },
    /** 熔煉 (heat + metal): molten metal, ignite plus melted armor (heavy vulnerability). */
    SMELT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.setRemainingFireTicks((int) (120 + power * 20));
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 140, 1));
        }
    },
    /** 灼療 (heat + life): cauterize. Heals the struck ally and sears off bleed/poison. No damage. */
    CAUTERIZE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(6.0F + power * 0.4F);
            target.removeEffect(ModMobEffects.BLEED);
            target.removeEffect(MobEffects.POISON);
        }
    },
    /** 冰封 (cold + water): a deep freeze, a hard chill plus heavy frozen ticks. */
    DEEP_FREEZE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            boolean wet = target.isInWaterOrRain();
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, wet ? 200 : 140, 4);
            target.setTicksFrozen(target.getTicksRequiredToFreeze() + (wet ? 240 : 160)); // 潮濕:凍更久
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, wet ? 60 : 40, 0); // 凍住:短暫完全不能動
        }
    },
    /** 冰蝕 (cold + acid): frost-brittle armor, chill plus vulnerability. */
    FROST_ETCH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 100, 2);
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 120, 0));
        }
    },
    /** 魔凍 (cold + arcane): mana-freeze, a chill plus an arcane burst. */
    MANA_FREEZE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.CHILL, 100, 2);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + power * 0.2F);
        }
    },
    /** 冬眠 (cold + life): hibernation, a slow regenerating heal. No damage. */
    HIBERNATE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(5.0F + power * 0.3F);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
    },
    /** 污水 (water + venom): tainted water, spreads poison to everything nearby. */
    TAINTED_WATER {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
            }
            lingerCloud(level, target, new MobEffectInstance(MobEffects.POISON, 60, 0), 3.0F, 120); // 留污水池
        }
    },
    /** 稀釋酸 (water + acid): a washing acid splash, weakness over an area. */
    ACID_WASH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
            }
        }
    },
    /** 鏽蝕 (water + metal): rust, eats armor (vulnerability plus weakness). */
    RUST {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 140, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        }
    },
    /** 聖水 (water + light): holy water, reveals foes and heals the caster a little. */
    HOLY_WATER {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            }
            if (caster != null && caster.isAlive()) caster.heal(3.0F + power * 0.2F);
        }
    },
    /** 沼氣 (water + dark): a miasma swamp gas, darkness plus poison around the target. */
    MIASMA {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
    },
    /** 激流 (water + force): a torrent, a powerful water-cannon knockback. */
    TORRENT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            double s = 1.7 + power * 0.06;
            target.setDeltaMovement(away.x * s, 0.35, away.z * s);
            target.hurtMarked = true;
        }
    },
    /** 靈泉 (water + arcane): a mana spring, an arcane burst that reveals. */
    MANA_SPRING {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.5F + power * 0.2F);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        }
    },
    /** 活泉 (water + life): a living spring, a heal plus regeneration. No damage. */
    LIVING_SPRING {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(7.0F + power * 0.4F);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1));
        }
    },
    /** 電解毒 (electric + venom): electrolysis, poison that spreads along the current (chains). */
    ELECTROLYSIS {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1));
        }
    },
    /** 電蝕 (electric + acid): electro-corrosion, vulnerability that arcs onward (chains). */
    ELECTRO_CORROSION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 140, 1));
        }
    },
    /** 電磁脈衝 (electric + metal): an EMP, a disorienting jolt that dazes nearby foes. */
    EMP {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(4.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                TurretControlHelper.applySkillControl(le, ModMobEffects.DAZE, 60, 0);
                TurretControlHelper.applySkillControl(le, ModMobEffects.ROOT, 20, 0);
            }
        }
    },
    /** 生物電 (electric + organic): bioelectric shock, locks the nervous system (chains). */
    BIOELECTRIC {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, 50, 0);
        }
    },
    /** 去顫 (electric + life): defibrillate, a jolt that heals the struck ally. No damage. */
    DEFIBRILLATE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(8.0F + power * 0.5F);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        }
    },
    /** 重金屬毒 (venom + metal): heavy-metal poisoning, deep poison plus mining fatigue. */
    HEAVY_METAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 2));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 140, 1));
        }
    },
    /** 淨毒 (venom + light): a purge, light detonates the poison for a burst plus blind.
     *  A poisoned target's toxin is detonated for a heavy bonus (poison consumed). */
    PURGE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            float bonus = 0.0F;
            if (target.hasEffect(MobEffects.POISON)) {
                target.removeEffect(MobEffects.POISON);
                bonus = 4.0F + power * 0.4F;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 4.0F + power * 0.3F + bonus);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
        }
    },
    /** 魔毒 (venom + arcane): arcane venom, poison plus an arcane burst. */
    ARCANE_VENOM {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.0F + power * 0.2F);
        }
    },
    /** 劇毒 (venom + life): virulence, life feeds the toxin into a brutal poison. On an
     *  already-poisoned target the disease turns terminal (adds wither). */
    VIRULENCE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (target.hasEffect(MobEffects.POISON)) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1)); // 病入膏肓
            }
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 3));
        }
    },
    /** 焦蝕 (acid + light): searing etch, melts armor and blinds with a caustic flash. */
    SEARING_ETCH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 140, 1));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
        }
    },
    /** 腐朽 (acid + dark): decay, rotting vulnerability plus wither. */
    DECAY {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 140, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        }
    },
    /** 酸爆 (acid + force): an acid splash blast, knockback plus spreading weakness. */
    ACID_SPLASH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                le.setDeltaMovement(away.x, 0.3, away.z);
                le.hurtMarked = true;
            }
        }
    },
    /** 枯萎 (acid + organic): wilt, plants rot into weakness and poison over an area. */
    WILT {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
    },
    /** 魔蝕 (acid + arcane): mana corrosion, vulnerability plus an arcane burst. */
    MANA_CORROSION {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 120, 1));
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().magic(), 2.5F + power * 0.2F);
        }
    },
    /** 中和 (acid + life): neutralize, life cancels the acid into a cleansing heal. No damage. */
    NEUTRALIZE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(5.0F + power * 0.3F);
            target.removeEffect(MobEffects.WEAKNESS);
            target.removeEffect(MobEffects.POISON);
        }
    },
    /** 鏡光 (metal + light): a mirror flash, a reflective glare that blinds nearby foes. */
    MIRROR_FLASH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            AABB box = target.getBoundingBox().inflate(3.5);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                TurretControlHelper.applySkillControl(le, ModMobEffects.DAZE, 60, 0);
            }
        }
    },
    /** 暗金 (metal + dark): cursed metal, wither plus melted armor. */
    CURSED_METAL {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 120, 0));
        }
    },
    /** 礦化 (metal + organic): petrify, roots the target in stone plus vulnerability. */
    PETRIFY {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, 80, 0);
            target.addEffect(new MobEffectInstance(ModMobEffects.VULNERABLE, 120, 0));
        }
    },
    /** 鐵衛 (metal + life): an iron ward, shields the caster with resistance and absorption. */
    IRON_WARD {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) {
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            }
        }
    },
    /** 光壓 (light + force): radiation pressure, a glowing shove that blasts the target away. */
    LIGHT_PRESSURE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            double s = 1.5 + power * 0.05;
            target.setDeltaMovement(away.x * s, 0.4, away.z * s);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        }
    },
    /** 藤鞭 (force + organic): a vine lash, roots the target then whips it back. */
    VINE_LASH {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            TurretControlHelper.applySkillControl(target, ModMobEffects.ROOT, 60, 0);
            Vec3 away = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            target.setDeltaMovement(away.x * 1.2, 0.3, away.z * 1.2);
            target.hurtMarked = true;
        }
    },
    /** 震癒 (force + life): a resonant pulse, heals the caster and shoves nearby foes off. */
    RESONANT_PULSE {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) caster.heal(4.0F + power * 0.3F);
            AABB box = target.getBoundingBox().inflate(3.5);
            Vec3 from = target.position();
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
                Vec3 push = le.position().subtract(from);
                if (push.lengthSqr() > 0.01) {
                    le.setDeltaMovement(le.getDeltaMovement().add(push.normalize().scale(0.8)));
                    le.hurtMarked = true;
                }
            }
        }
    },
    /** 共生 (organic + arcane): symbiosis, mana-fed growth shields the caster over time. */
    SYMBIOSIS {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            if (caster != null && caster.isAlive()) {
                caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            }
        }
    },
    /** 奧術癒合 (arcane + life): arcane mending, a strong heal with regeneration and a shield. No damage. */
    ARCANE_MENDING {
        @Override public void apply(ServerLevel level, LivingEntity target, @Nullable LivingEntity caster, Vec3 dir, float power) {
            target.heal(9.0F + power * 0.5F);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 2));
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        }
    };

    public abstract void apply(ServerLevel level, LivingEntity target,
                               @Nullable LivingEntity caster, Vec3 dir, float power);

    /** The emergent reaction ops (never mapped from a single aspect). */
    private static final java.util.EnumSet<SkillEffectOp> REACTIONS = java.util.EnumSet.of(
            THERMAL_SHOCK, COMBUSTION, TOXIC_BURN, SHATTER, OVERLOAD, DEATH_SIPHON, WILDFIRE,
            STRONG_ACID, ANNIHILATION, SOULFIRE, CRYO_VENOM, SOLAR_FLARE, OVERGROWTH, SHRAPNEL,
            ELEMENTAL_STORM, THERMONUCLEAR, BIOHAZARD, HOLY_NOVA, BLIGHT, MAELSTROM,
            // batch 3 (chemistry engine)
            STEAM_BURST, CORRODE, KINETIC_BLAST, IMPLOSION, TOXIC_BURST, MAGNETIC_STORM, SPITE_BOLT,
            PHOTOELECTRIC, PHOTOSYNTHESIS, SANCTIFY, RADIANT_SURGE, PLAGUE, ROT_BLOOM, VOID_SURGE,
            SPORE_BURST, FLOURISH, SUPERCONDUCT, PRISM, WITHER_FROST, FROSTBIND, CRYOCRYSTAL,
            MELTDOWN, ERUPTION, SUPERHEATED_PLASMA, HYDROGEN_BLAST, ASH_STORM,
            // batch 4 (fill the trait matrix)
            THERMION, SMELT, CAUTERIZE, DEEP_FREEZE, FROST_ETCH, MANA_FREEZE, HIBERNATE,
            TAINTED_WATER, ACID_WASH, RUST, HOLY_WATER, MIASMA, TORRENT, MANA_SPRING, LIVING_SPRING,
            ELECTROLYSIS, ELECTRO_CORROSION, EMP, BIOELECTRIC, DEFIBRILLATE, HEAVY_METAL, PURGE,
            ARCANE_VENOM, VIRULENCE, SEARING_ETCH, DECAY, ACID_SPLASH, WILT, MANA_CORROSION, NEUTRALIZE,
            MIRROR_FLASH, CURSED_METAL, PETRIFY, IRON_WARD, LIGHT_PRESSURE, VINE_LASH, RESONANT_PULSE,
            SYMBIOSIS, ARCANE_MENDING);

    public boolean isReaction() {
        return REACTIONS.contains(this);
    }

    /** Translation key for this op's player-facing name (used by reaction feedback). */
    public String translationKey() {
        return "skillop.koniava." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Apply this op and, if it lands on a training dummy and is a reaction, log it on
     * the dummy so the HUD can show the player which reactions fired. Carriers should
     * call this instead of {@link #apply} so reaction feedback is captured centrally.
     */
    public void applyTo(ServerLevel level, LivingEntity target,
                        @Nullable LivingEntity caster, Vec3 dir, float power) {
        apply(level, target, caster, dir, power);
        if (isReaction()) {
            ReactionFx.spawn(level, target, this); // 每個反應的專屬粒子簽名
            if (target instanceof com.github.nalamodikk.common.entity.TrainingDummyEntity dummy) {
                dummy.recordReaction(translationKey());
            }
        }
    }

    // ── Tier C 共用機制 ──
    /** 留一團持續 AreaEffectCloud 在地上(雲類反應的「危險地形」)。 */
    private static void lingerCloud(ServerLevel level, LivingEntity at, MobEffectInstance effect,
                                    float radius, int duration) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.getX(), at.getY(), at.getZ());
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusOnUse(-0.1F);
        cloud.setWaitTime(5);
        cloud.setParticle(ParticleTypes.SNEEZE);
        cloud.addEffect(effect);
        level.addFreshEntity(cloud);
    }

    /** 把附近掉落物吸向中心(奇點機制)。 */
    private static void vacuumItems(ServerLevel level, LivingEntity center, double radius) {
        Vec3 c = center.position();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, center.getBoundingBox().inflate(radius))) {
            Vec3 pull = c.subtract(item.position());
            if (pull.lengthSqr() > 0.04) item.setDeltaMovement(pull.normalize().scale(0.5));
        }
    }
}
