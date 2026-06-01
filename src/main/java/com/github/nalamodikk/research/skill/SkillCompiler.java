package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.common.entity.SpellProjectileEntity;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

        int baseCount = modifiers.contains(ModAspects.REFRACTION) ? 3 : 1;
        if (modifiers.contains(ModAspects.RESONANCE)) baseCount += 1; // echo: one extra shot
        final int count = baseCount;
        boolean pierce = modifiers.contains(ModAspects.BLADE);
        boolean warding = modifiers.contains(ModAspects.WARDING);
        boolean resist = modifiers.contains(ModAspects.KUN);
        boolean invis = modifiers.contains(ModAspects.SHADOW);
        boolean strength = modifiers.contains(ModAspects.AURA);
        boolean corpus = modifiers.contains(ModAspects.CORPUS);
        boolean faith = modifiers.contains(ModAspects.FAITH);
        boolean order = modifiers.contains(ModAspects.ORDER);
        boolean instinct = modifiers.contains(ModAspects.INSTINCT);
        boolean homing = modifiers.contains(ModAspects.ANIMA);
        int chainCount = (modifiers.contains(ModAspects.ARC) || modifiers.contains(ModAspects.PROPAGATION)) ? 2 : 0;
        float power = 1.0F
                + (modifiers.contains(ModAspects.FORTIFY) ? 0.5F : 0.0F)
                + (modifiers.contains(ModAspects.QIAN) ? 1.0F : 0.0F)
                + (modifiers.contains(ModAspects.MECHANISM) ? 0.3F : 0.0F)
                + (modifiers.contains(ModAspects.INSTRUMENT) ? 0.3F : 0.0F)
                + (modifiers.contains(ModAspects.ALCHEMY) ? 0.4F : 0.0F)
                + (modifiers.contains(ModAspects.ENERGY) ? 0.5F : 0.0F)
                + (modifiers.contains(ModAspects.ARCANA) ? 0.5F : 0.0F);
        int fuelTotal = fuel.values().stream().mapToInt(Integer::intValue).sum();
        float carrierDmgMult = carrier == ModAspects.MANA ? 0.85F : 1.0F; // 魔力較弱(換便宜)
        float damage = (4.0F + 2.0F * effects.size() + fuelTotal) * power * carrierDmgMult;

        List<SkillEffectOp> finalOps = List.copyOf(ops);
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

            // INSTINCT: a chance to crit this cast (rolled per cast, not baked into the cost)
            float dmg = damage;
            if (instinct && level.getRandom().nextFloat() < 0.25F) dmg *= 1.5F;

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
                for (SkillEffectOp op : finalOps) op.apply(level, caster, caster, look, dmg);
                level.playSound(null, caster.blockPosition(),
                        SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
            } else if (carrier == ModAspects.PIPELINE) {
                castBeam(level, caster, dmg, finalOps);
            } else if (carrier == ModAspects.GRAVITY) {
                castField(level, caster, dmg, finalOps);
            } else if (carrier == ModAspects.BLADE) {
                castSlash(level, caster, dmg, finalOps);
            }
        };

        return new CompiledSkill(cost, action);
    }

    /** BLADE carrier: a melee-range blade wave that slashes everything in a forward arc. */
    private static void castSlash(ServerLevel level, ServerPlayer caster, float damage, List<SkillEffectOp> ops) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double range = 4.5;
        double coneCos = 0.4; // ~132 degree arc in front
        DamageSource src = caster.damageSources().playerAttack(caster);
        for (Entity e : level.getEntities(caster, caster.getBoundingBox().inflate(range),
                x -> x instanceof LivingEntity && x.isAlive())) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.length() > range || to.normalize().dot(look) < coneCos) continue;
            e.invulnerableTime = 0;
            e.hurt(src, damage);
            if (e instanceof LivingEntity le) for (SkillEffectOp op : ops) op.apply(level, le, caster, look, damage);
        }
        // sweep arc visual
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = look.cross(up).normalize();
        for (int i = 0; i <= 14; i++) {
            double a = (i / 14.0 - 0.5) * Math.PI * 0.75;
            Vec3 dir = look.scale(Math.cos(a)).add(side.scale(Math.sin(a))).normalize();
            Vec3 p = eye.add(dir.scale(2.5));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
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
    private static void castBeam(ServerLevel level, ServerPlayer caster, float damage, List<SkillEffectOp> ops) {
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double range = 16.0;
        Vec3 end = start.add(look.scale(range));
        for (int s = 0; s <= 24; s++) {
            Vec3 m = start.lerp(end, s / 24.0);
            level.sendParticles(ParticleTypes.END_ROD, m.x, m.y, m.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        DamageSource src = caster.damageSources().indirectMagic(caster, caster);
        AABB box = new AABB(start, end).inflate(1.5);
        for (Entity e : level.getEntities(caster, box, x -> x instanceof LivingEntity && x.isAlive())) {
            Vec3 c = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            double t = c.subtract(start).dot(look);
            if (t < 0.0 || t > range) continue;
            if (start.add(look.scale(t)).distanceTo(c) > 1.3) continue;
            e.invulnerableTime = 0;
            e.hurt(src, damage);
            if (e instanceof LivingEntity le) for (SkillEffectOp op : ops) op.apply(level, le, caster, look, damage);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    /** GRAVITY carrier: a singularity a few blocks ahead that pulls + damages. */
    private static void castField(ServerLevel level, ServerPlayer caster, float damage, List<SkillEffectOp> ops) {
        Vec3 center = caster.getEyePosition().add(caster.getLookAngle().scale(6.0));
        double radius = 4.0;
        for (int i = 0; i < 40; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = radius * Math.sqrt(level.random.nextDouble());
            Vec3 p = center.add(Math.cos(a) * r, level.random.nextDouble() * 2 - 1, Math.sin(a) * r);
            level.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 1,
                    (center.x - p.x) * 0.2, (center.y - p.y) * 0.2, (center.z - p.z) * 0.2, 0.3);
        }
        DamageSource src = caster.damageSources().indirectMagic(caster, caster);
        AABB box = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            Vec3 pull = center.subtract(le.position());
            if (pull.lengthSqr() > radius * radius) continue;
            le.setDeltaMovement(le.getDeltaMovement().add(pull.normalize().scale(0.5)));
            le.hurtMarked = true;
            le.invulnerableTime = 0;
            le.hurt(src, damage * 0.6F);
            for (SkillEffectOp op : ops) op.apply(level, le, caster, pull.normalize().scale(-1.0), damage * 0.6F);
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    private static SkillEffectOp opFor(Aspect effect) {
        if (effect == ModAspects.PHLOGISTON || effect == ModAspects.MAGMA || effect == ModAspects.LI
                || effect == ModAspects.FURNACE || effect == ModAspects.VAPOR || effect == ModAspects.STEAM) {
            return SkillEffectOp.FIRE;
        }
        if (effect == ModAspects.FROST) return SkillEffectOp.FROST;
        if (effect == ModAspects.VENOM) return SkillEffectOp.POISON;
        if (effect == ModAspects.CORROSION || effect == ModAspects.FAMINE) return SkillEffectOp.WEAKEN;
        if (effect == ModAspects.ZHEN || effect == ModAspects.ARC) return SkillEffectOp.LIGHTNING;
        if (effect == ModAspects.STORM || effect == ModAspects.KAN || effect == ModAspects.DUI) return SkillEffectOp.KNOCKBACK;
        if (effect == ModAspects.RADIANCE) return SkillEffectOp.BLIND;
        if (effect == ModAspects.GROWTH) return SkillEffectOp.ROOT;
        if (effect == ModAspects.VITAE) return SkillEffectOp.LIFESTEAL;
        if (effect == ModAspects.VITALITY || effect == ModAspects.MENDING
                || effect == ModAspects.LIFEFLOW || effect == ModAspects.NOURISH) return SkillEffectOp.HEAL;
        if (effect == ModAspects.CRYSTAL || effect == ModAspects.ENERGY || effect == ModAspects.ARCANA) return SkillEffectOp.SHARD;
        if (effect == ModAspects.DEATH || effect == ModAspects.UNDEAD || effect == ModAspects.TAINT
                || effect == ModAspects.ELDRITCH || effect == ModAspects.SPIRITUS
                || effect == ModAspects.VOID_ASPECT) {
            return SkillEffectOp.WITHER;
        }
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
