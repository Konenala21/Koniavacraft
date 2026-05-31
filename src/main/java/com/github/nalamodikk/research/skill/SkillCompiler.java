package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.common.entity.SpellProjectileEntity;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    public static SkillEffect compile(Aspect carrier, List<Aspect> effects,
                                      List<Aspect> modifiers, Map<Aspect, Integer> fuel) {
        Map<ResourceLocation, Integer> cost = new LinkedHashMap<>();
        if (carrier != null) cost.merge(carrier.getId(), 1, Integer::sum);
        for (Aspect e : effects)   cost.merge(e.getId(), 1, Integer::sum);
        for (Aspect m : modifiers) cost.merge(m.getId(), 1, Integer::sum);
        for (Map.Entry<Aspect, Integer> fe : fuel.entrySet()) cost.merge(fe.getKey().getId(), fe.getValue(), Integer::sum);

        List<SkillEffectOp> ops = new ArrayList<>();
        for (Aspect e : effects) {
            SkillEffectOp op = opFor(e);
            if (op != null && !ops.contains(op)) ops.add(op);
        }

        int count = modifiers.contains(ModAspects.REFRACTION) ? 3 : 1;
        boolean pierce = modifiers.contains(ModAspects.BLADE);
        float power = 1.0F
                + (modifiers.contains(ModAspects.FORTIFY) ? 0.5F : 0.0F)
                + (modifiers.contains(ModAspects.QIAN) ? 1.0F : 0.0F);
        int fuelTotal = fuel.values().stream().mapToInt(Integer::intValue).sum();
        float damage = (4.0F + 2.0F * effects.size() + fuelTotal) * power;

        List<SkillEffectOp> finalOps = List.copyOf(ops);
        boolean isProjectile = carrier == null
                || carrier == ModAspects.MOMENTUM
                || carrier == ModAspects.MANA
                || carrier == ModAspects.XUN;

        Consumer<SkillContext> action = ctx -> {
            ServerPlayer caster = ctx.caster();
            ServerLevel level = ctx.level();
            if (isProjectile) {
                Vec3 look = caster.getLookAngle();
                for (int i = 0; i < count; i++) {
                    Vec3 dir = count == 1 ? look : spread(look, i, count);
                    level.addFreshEntity(
                            SpellProjectileEntity.shoot(level, caster, dir, damage, finalOps, pierce));
                }
                level.playSound(null, caster.blockPosition(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
            // other carrier families (self / area / beam) come later
        };

        return new CompiledSkill(Collections.unmodifiableMap(cost), action);
    }

    private static SkillEffectOp opFor(Aspect effect) {
        if (effect == ModAspects.PHLOGISTON || effect == ModAspects.MAGMA || effect == ModAspects.LI
                || effect == ModAspects.FURNACE || effect == ModAspects.VAPOR || effect == ModAspects.STEAM) {
            return SkillEffectOp.FIRE;
        }
        if (effect == ModAspects.FROST) return SkillEffectOp.FROST;
        if (effect == ModAspects.VENOM) return SkillEffectOp.POISON;
        if (effect == ModAspects.CORROSION || effect == ModAspects.FAMINE) return SkillEffectOp.WEAKEN;
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
