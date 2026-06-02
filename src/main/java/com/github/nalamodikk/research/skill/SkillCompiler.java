package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.entity.SpellProjectileEntity;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.skill.reaction.ReactionEngine;
import com.github.nalamodikk.research.skill.fx.CarrierFx;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Turns an aspect combination (carrier + effects + modifiers + fuel) into an
 * executable {@link SkillEffect}, using the {@link AspectRoles} dictionary.
 *
 * This is the heart of B: the same compiler handles any valid combination, so new
 * skills come from new aspect mixes, not new code. MVP coverage: MOMENTUM-style
 * carriers (projectile), fire/frost/poison/weaken effects, split/pierce/amplify
 * modifiers. Unmapped aspects contribute cost + base power but no special behavior.
 */
public final class SkillCompiler {

    /** Per-cast mana price building blocks (tuned later in balancing). */
    private static final int MANA_BASE = 100;
    private static final int MANA_PER_CARRIER = 80;
    private static final int MANA_PER_EFFECT = 60;
    private static final int MANA_PER_MODIFIER = 50;
    private static final int MANA_PER_FUEL = 20;

    public static SkillEffect compile(Aspect carrier, List<Aspect> effects,
                                      List<Aspect> modifiers, Map<Aspect, Integer> fuel) {
        // Aspect gate: every aspect used must be OWNED (gate, not consumed). Owning
        // one is enough, so each maps to 1 regardless of how it is used.
        Map<ResourceLocation, Integer> gate = new LinkedHashMap<>();
        if (carrier != null) gate.put(carrier.getId(), 1);
        for (Aspect e : effects)   gate.put(e.getId(), 1);
        for (Aspect m : modifiers) gate.put(m.getId(), 1);
        for (Aspect a : fuel.keySet()) gate.put(a.getId(), 1);

        int fuelTotalForMana = fuel.values().stream().mapToInt(Integer::intValue).sum();
        int carrierMana = carrier == ModAspects.MANA ? 50 : (carrier != null ? MANA_PER_CARRIER : 0); // 魔力:便宜基礎彈
        int rawMana = MANA_BASE
                + carrierMana
                + MANA_PER_EFFECT * effects.size()
                + MANA_PER_MODIFIER * modifiers.size()
                + MANA_PER_FUEL * fuelTotalForMana;
        // cooldown follows complexity (from the undiscounted cost); GEAR speeds it up.
        int cooldown = Math.max(10, Math.min(100, rawMana / 15));
        if (modifiers.contains(ModAspects.GEAR)) cooldown = Math.max(5, Math.round(cooldown * 0.65F));
        int mana = modifiers.contains(ModAspects.WISDOM) ? Math.round(rawMana * 0.8F) : rawMana; // 智慧:省魔力
        SkillCost cost = new SkillCost(Collections.unmodifiableMap(gate), mana, cooldown);

        List<SkillEffectOp> ops = new ArrayList<>();
        for (Aspect e : effects) {
            SkillEffectOp op = opFor(e);
            if (op != null && !ops.contains(op)) ops.add(op);
        }
        // BINDING / GEN (艮) modifiers add a root payload on hit
        if ((modifiers.contains(ModAspects.BINDING) || modifiers.contains(ModAspects.GEN))
                && !ops.contains(SkillEffectOp.ROOT)) {
            ops.add(SkillEffectOp.ROOT);
        }
        // Aspect reactions (chemistry): reacting pairs append a special reaction op.
        int reactionChain = applyReactions(effects, ops);

        int baseCount = modifiers.contains(ModAspects.REFRACTION) ? 3 : 1;
        if (modifiers.contains(ModAspects.RESONANCE)) baseCount += 1; // echo: one extra shot
        if (modifiers.contains(ModAspects.AUTOMATION)) baseCount += 1; // automated repeater: extra shot
        final int count = baseCount;
        boolean pierce = modifiers.contains(ModAspects.BLADE);
        boolean warding = modifiers.contains(ModAspects.WARDING);
        boolean resist = modifiers.contains(ModAspects.KUN);
        boolean invis = modifiers.contains(ModAspects.SHADOW);
        boolean strength = modifiers.contains(ModAspects.AURA);
        boolean corpus = modifiers.contains(ModAspects.CORPUS);
        boolean faith = modifiers.contains(ModAspects.FAITH);
        boolean order = modifiers.contains(ModAspects.ORDER);
        boolean civilization = modifiers.contains(ModAspects.CIVILIZATION);
        boolean humanity = modifiers.contains(ModAspects.HUMANITY);
        boolean instinct = modifiers.contains(ModAspects.INSTINCT);
        boolean homing = modifiers.contains(ModAspects.ANIMA);
        int chainCount = ((modifiers.contains(ModAspects.ARC) || modifiers.contains(ModAspects.PROPAGATION)) ? 2 : 0)
                + reactionChain; // conduction reaction also chains
        float power = 1.0F
                + (modifiers.contains(ModAspects.FORTIFY) ? 0.5F : 0.0F)
                + (modifiers.contains(ModAspects.QIAN) ? 1.0F : 0.0F)
                + (modifiers.contains(ModAspects.MECHANISM) ? 0.4F : 0.0F)
                + (modifiers.contains(ModAspects.INSTRUMENT) ? 0.4F : 0.0F)
                + (modifiers.contains(ModAspects.ALCHEMY) ? 0.5F : 0.0F)
                + (modifiers.contains(ModAspects.ENERGY) ? 0.6F : 0.0F)
                + (modifiers.contains(ModAspects.ARCANA) ? 0.6F : 0.0F)
                + (instinct ? 0.2F : 0.0F); // 本能:永遠小增傷 + 下面還有暴擊機率
        int fuelTotal = fuel.values().stream().mapToInt(Integer::intValue).sum();
        float carrierDmgMult = carrier == ModAspects.MANA ? 0.85F
                : (carrier == ModAspects.MACHINE ? 1.1F : 1.0F); // 魔力較弱(換便宜)；機械砲彈略強
        // 技能是 boss 後、走完整條工業鏈才解鎖的本源核心脊椎，威力要配得上投資。
        // 基礎高 + 每效果重加成 → 組合越多本源越強，獎勵深度組合（對應「可組合的要變強」）。
        float damage = (6.0F + 4.0F * effects.size() + 2.0F * fuelTotal) * power * carrierDmgMult;

        List<SkillEffectOp> finalOps = List.copyOf(ops);
        // 支援型技能:含補血效果 → 載體不造成基礎傷害,免得幫隊友補血卻一發把人打死。
        // (補血是補命中對象,配投射可以射隊友補血;血氣是吸血、不算支援。)
        boolean support = ops.contains(SkillEffectOp.VITALITY) || ops.contains(SkillEffectOp.MENDING)
                || ops.contains(SkillEffectOp.LIFEFLOW) || ops.contains(SkillEffectOp.NOURISH);
        boolean isProjectile = carrier == null
                || carrier == ModAspects.MOMENTUM
                || carrier == ModAspects.MANA
                || carrier == ModAspects.XUN;
        boolean isDash = carrier == ModAspects.FLIGHT;

        // per-carrier projectile feel: 巽=快+遠+輕推, 動力=衝擊擊退, 魔力=基礎無推
        final double projSpeed = carrier == ModAspects.XUN ? 1.7 : 1.2;
        final int projLife = carrier == ModAspects.XUN ? 70 : 40;
        final float projKnock = carrier == ModAspects.MANA ? 0.0F : (carrier == ModAspects.XUN ? 0.25F : 0.5F);

        Consumer<SkillContext> action = ctx -> {
            ServerPlayer caster = ctx.caster();
            ServerLevel level = ctx.level();
            if (warding)  caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            if (resist)   caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            if (invis)    caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
            if (strength) caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            if (corpus)   caster.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 600, 1)); // 肉體強化
            if (faith)    caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));  // 撐住
            if (order)    cleanse(caster); // 淨化自身負面
            if (civilization) caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1)); // 堡壘:抗性 II
            if (humanity) { // 決意:大吸收 + 速度
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3));
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            }

            // 支援技能不造成傷害(只補血/上狀態);否則 INSTINCT 有機率本次暴擊
            float dmg = support ? 0.0F : damage;
            if (!support && instinct && level.getRandom().nextFloat() < 0.25F) dmg *= 1.5F;

            if (isProjectile) {
                Vec3 look = caster.getLookAngle();
                for (int i = 0; i < count; i++) {
                    Vec3 dir = count == 1 ? look : spread(look, i, count);
                    level.addFreshEntity(SpellProjectileEntity.shoot(
                            level, caster, dir, dmg, finalOps, pierce, homing, chainCount,
                            projSpeed, projLife, projKnock));
                }
                level.playSound(null, caster.blockPosition(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.4F);
            } else if (isDash) {
                Vec3 look = caster.getLookAngle();
                caster.setDeltaMovement(look.scale(1.6).add(0.0, 0.25, 0.0));
                caster.hurtMarked = true; // sync the impulse to the client
                caster.fallDistance = 0.0F;
                // FLIGHT is the self carrier: effects land on the caster (heal/buff dash)
                for (SkillEffectOp op : finalOps) op.applyTo(level, caster, caster, look, dmg);
                level.playSound(null, caster.blockPosition(),
                        SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
            } else if (carrier == ModAspects.PIPELINE) {
                // count(折射/共鳴/自動)= 多打幾道; chainCount(連鎖/導電)= 命中後連鎖到附近
                for (int i = 0; i < count; i++) castBeam(level, caster, dmg, finalOps, chainCount);
            } else if (carrier == ModAspects.GRAVITY) {
                for (int i = 0; i < count; i++) castField(level, caster, dmg, finalOps, chainCount);
            } else if (carrier == ModAspects.BLADE) {
                for (int i = 0; i < count; i++) castSlash(level, caster, dmg, finalOps, chainCount);
            } else if (carrier == ModAspects.MACHINE) {
                // 機械載體：發射浮游砲子彈，帶技能傷害 + 命中效果 ops。
                // 機械+火=火砲彈、機械+霜=冰砲彈、機械+凋零=腐蝕砲彈，效果本源決定砲彈變種。
                Vec3 look = caster.getLookAngle();
                Vec3 spawnPos = caster.getEyePosition().add(look.scale(0.6));
                for (int i = 0; i < count; i++) {
                    FloatingTurretProjectile bolt = FloatingTurretProjectile.shoot(level, caster, spawnPos);
                    bolt.setSkillPayload(dmg, finalOps);
                    level.addFreshEntity(bolt);
                }
                level.playSound(null, caster.blockPosition(),
                        SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 0.9F, 1.0F);
            }
        };

        return new CompiledSkill(cost, action);
    }

    /** BLADE carrier: a melee-range blade wave that slashes everything in a forward arc. */
    private static void castSlash(ServerLevel level, ServerPlayer caster, float damage, List<SkillEffectOp> ops, int chainCount) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double range = 4.5;
        double coneCos = 0.4; // ~132 degree arc in front
        DamageSource src = caster.damageSources().playerAttack(caster);
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (Entity e : level.getEntities(caster, caster.getBoundingBox().inflate(range),
                x -> x instanceof LivingEntity && x.isAlive())) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.length() > range || to.normalize().dot(look) < coneCos) continue;
            e.invulnerableTime = 0;
            e.hurt(src, damage);
            if (e instanceof LivingEntity le) {
                hit.add(le);
                for (SkillEffectOp op : ops) op.applyTo(level, le, caster, look, damage);
            }
        }
        chainOps(level, caster, hit, ops, damage, chainCount);
        CarrierFx.slash(level, eye, look); // 視覺:扇形劍氣
        level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.1F);
    }

    /** ORDER: remove all harmful status effects from the caster. */
    private static void cleanse(LivingEntity entity) {
        List<Holder<MobEffect>> harmful = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(h -> h.value().getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        harmful.forEach(entity::removeEffect);
    }

    /** PIPELINE carrier: an instant hitscan beam from the caster's eyes. */
    private static void castBeam(ServerLevel level, ServerPlayer caster, float damage, List<SkillEffectOp> ops, int chainCount) {
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double range = 16.0;
        Vec3 end = start.add(look.scale(range));
        CarrierFx.beam(level, start, end); // 視覺:光束
        DamageSource src = caster.damageSources().indirectMagic(caster, caster);
        AABB box = new AABB(start, end).inflate(1.5);
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (Entity e : level.getEntities(caster, box, x -> x instanceof LivingEntity && x.isAlive())) {
            Vec3 c = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            double t = c.subtract(start).dot(look);
            if (t < 0.0 || t > range) continue;
            if (start.add(look.scale(t)).distanceTo(c) > 1.3) continue;
            e.invulnerableTime = 0;
            e.hurt(src, damage);
            if (e instanceof LivingEntity le) {
                hit.add(le);
                for (SkillEffectOp op : ops) op.applyTo(level, le, caster, look, damage);
            }
        }
        chainOps(level, caster, hit, ops, damage, chainCount);
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    /** Chain reaction / arc: from each already-hit target, jump to nearby foes, up to chainCount. */
    private static void chainOps(ServerLevel level, ServerPlayer caster, java.util.Set<LivingEntity> hit,
                                 List<SkillEffectOp> ops, float damage, int chainCount) {
        if (chainCount <= 0 || hit.isEmpty()) return;
        DamageSource src = caster.damageSources().indirectMagic(caster, caster);
        int chained = 0;
        for (LivingEntity from : new java.util.ArrayList<>(hit)) {
            if (chained >= chainCount) break;
            AABB box = from.getBoundingBox().inflate(5.0);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != caster && e.isAlive() && !hit.contains(e))) {
                le.invulnerableTime = 0;
                le.hurt(src, damage * 0.5F);
                Vec3 d = le.position().subtract(from.position());
                Vec3 dir = d.lengthSqr() > 1.0E-4 ? d.normalize() : new Vec3(0, 0, 1);
                for (SkillEffectOp op : ops) op.applyTo(level, le, caster, dir, damage * 0.5F);
                CarrierFx.chainSpark(level, le); // 視覺:連鎖火花
                hit.add(le);
                if (++chained >= chainCount) break;
            }
        }
    }

    /** GRAVITY carrier: a singularity a few blocks ahead that pulls + damages. */
    private static void castField(ServerLevel level, ServerPlayer caster, float damage, List<SkillEffectOp> ops, int chainCount) {
        Vec3 center = caster.getEyePosition().add(caster.getLookAngle().scale(6.0));
        double radius = 4.0;
        CarrierFx.field(level, center, radius); // 視覺:重力漩渦
        DamageSource src = caster.damageSources().indirectMagic(caster, caster);
        AABB box = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            Vec3 pull = center.subtract(le.position());
            if (pull.lengthSqr() > radius * radius) continue;
            le.setDeltaMovement(le.getDeltaMovement().add(pull.normalize().scale(0.5)));
            le.hurtMarked = true;
            le.invulnerableTime = 0;
            le.hurt(src, damage * 0.6F);
            hit.add(le);
            for (SkillEffectOp op : ops) op.applyTo(level, le, caster, pull.normalize().scale(-1.0), damage * 0.6F);
        }
        chainOps(level, caster, hit, ops, damage * 0.6F, chainCount);
        level.playSound(null, BlockPos.containing(center), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    /**
     * Aspect reactions (chemistry): when the effect set contains a reacting pair,
     * append the emergent reaction op. Returns extra chain count (conduction).
     * Reaction ops stack on top of each aspect's own effect, so the combination
     * does something neither aspect does alone. More reactions can be added here.
     */
    /**
     * Runs the chemistry reaction engine over the effect aspects and appends the
     * resulting reaction ops (in cascade order) to {@code ops}. Returns the extra
     * chain jumps the reactions add. The actual trait table, rules and cascade
     * (products feeding back) live in {@link ReactionEngine}.
     */
    private static int applyReactions(List<Aspect> effects, List<SkillEffectOp> ops) {
        ReactionEngine.Result result = ReactionEngine.run(effects);
        for (SkillEffectOp op : result.ops()) addOnce(ops, op);
        return result.chain();
    }

    private static void addOnce(List<SkillEffectOp> ops, SkillEffectOp op) {
        if (!ops.contains(op)) ops.add(op);
    }

    /**
     * Maps an effect aspect to its OWN op: every aspect is distinct, so two
     * different aspects never produce the same on-hit behavior. Aspects with no
     * combat op (abstract/society) return null and contribute only cost + power.
     */
    private static SkillEffectOp opFor(Aspect effect) {
        // Fire family (each burns differently)
        if (effect == ModAspects.PHLOGISTON) return SkillEffectOp.PHLOGISTON;
        if (effect == ModAspects.MAGMA)      return SkillEffectOp.MAGMA;
        if (effect == ModAspects.LI)         return SkillEffectOp.LI;
        if (effect == ModAspects.FURNACE)    return SkillEffectOp.FURNACE;
        if (effect == ModAspects.VAPOR)      return SkillEffectOp.VAPOR;
        if (effect == ModAspects.STEAM)      return SkillEffectOp.STEAM;
        // Cold / nature
        if (effect == ModAspects.FROST)      return SkillEffectOp.FROST;
        if (effect == ModAspects.GROWTH)     return SkillEffectOp.ROOT;
        if (effect == ModAspects.VENOM)      return SkillEffectOp.VENOM;
        if (effect == ModAspects.CORROSION)  return SkillEffectOp.CORROSION;
        if (effect == ModAspects.FAMINE)     return SkillEffectOp.FAMINE;
        // Electric
        if (effect == ModAspects.ZHEN)       return SkillEffectOp.ZHEN;
        if (effect == ModAspects.ARC)        return SkillEffectOp.ARC;
        // Force
        if (effect == ModAspects.STORM)      return SkillEffectOp.STORM;
        if (effect == ModAspects.KAN)        return SkillEffectOp.KAN;
        if (effect == ModAspects.DUI)        return SkillEffectOp.DUI;
        // Light
        if (effect == ModAspects.RADIANCE)   return SkillEffectOp.RADIANCE;
        // Dark / death family
        if (effect == ModAspects.DEATH)      return SkillEffectOp.DEATH;
        if (effect == ModAspects.UNDEAD)     return SkillEffectOp.UNDEAD;
        if (effect == ModAspects.TAINT)      return SkillEffectOp.TAINT;
        if (effect == ModAspects.ELDRITCH)   return SkillEffectOp.ELDRITCH;
        if (effect == ModAspects.SPIRITUS)   return SkillEffectOp.SPIRITUS;
        if (effect == ModAspects.VOID_ASPECT) return SkillEffectOp.VOID;
        // Life family
        if (effect == ModAspects.VITAE)      return SkillEffectOp.LIFESTEAL;
        if (effect == ModAspects.VITALITY)   return SkillEffectOp.VITALITY;
        if (effect == ModAspects.MENDING)    return SkillEffectOp.MENDING;
        if (effect == ModAspects.LIFEFLOW)   return SkillEffectOp.LIFEFLOW;
        if (effect == ModAspects.NOURISH)    return SkillEffectOp.NOURISH;
        // Bonus damage family
        if (effect == ModAspects.CRYSTAL)    return SkillEffectOp.CRYSTAL;
        if (effect == ModAspects.ENERGY)     return SkillEffectOp.ENERGY;
        if (effect == ModAspects.ARCANA)     return SkillEffectOp.ARCANA;
        // Abstract aspects, imaginative effects
        if (effect == ModAspects.EXCAVATION) return SkillEffectOp.EXCAVATION;
        if (effect == ModAspects.BESTIA)     return SkillEffectOp.BESTIA;
        if (effect == ModAspects.HARVEST)    return SkillEffectOp.HARVEST;
        if (effect == ModAspects.SENSUS)     return SkillEffectOp.SENSUS;
        if (effect == ModAspects.COGNITION)  return SkillEffectOp.COGNITION;
        if (effect == ModAspects.DESIRE)     return SkillEffectOp.DESIRE;
        if (effect == ModAspects.LAW)        return SkillEffectOp.LAW;
        // Society / special aspects, each its own variant
        if (effect == ModAspects.COMMERCE)   return SkillEffectOp.COMMERCE;
        if (effect == ModAspects.LANGUAGE)   return SkillEffectOp.LANGUAGE;
        if (effect == ModAspects.PRIMORDIAL) return SkillEffectOp.PRIMORDIAL;
        if (effect == ModAspects.WEALTH)     return SkillEffectOp.WEALTH;
        return null;
    }

    /** Spread {@code count} directions evenly around {@code look} (15 degrees apart). */
    private static Vec3 spread(Vec3 look, int index, int count) {
        double step = Math.toRadians(15.0);
        double angle = (index - (count - 1) / 2.0) * step;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        // rotate the horizontal component around the Y axis
        return new Vec3(look.x * cos - look.z * sin, look.y, look.x * sin + look.z * cos).normalize();
    }

    private SkillCompiler() {}
}
