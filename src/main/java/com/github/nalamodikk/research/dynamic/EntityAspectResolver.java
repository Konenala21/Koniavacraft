package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Procedurally resolves aspects for living entities based on their classification.
 */
public class EntityAspectResolver {

    public static List<Aspect> resolve(LivingEntity entity, ServerLevel level) {
        List<Aspect> aspects = new ArrayList<>();
        long seed = level.getSeed() ^ entity.getType().hashCode();
        Random random = new Random(seed);

        // 1. Core Classification (Logic)
        if (entity.getMobType() == MobType.UNDEAD) {
            aspects.add(ModAspects.ANIMA);
            aspects.add(ModAspects.WU);
        } else if (entity.getMobType() == MobType.ARTHROPOD) {
            aspects.add(ModAspects.VITALITY);
            aspects.add(ModAspects.CORROSION);
        } else if (entity instanceof Creeper) {
            aspects.add(ModAspects.ENERGY);
            aspects.add(ModAspects.PHLOGISTON);
        } else if (entity instanceof EnderMan) {
            aspects.add(ModAspects.WU);
            aspects.add(ModAspects.GRAVITY);
        } else {
            // Default to Animal/Living
            aspects.add(ModAspects.VITALITY);
            aspects.add(ModAspects.EARTH);
        }

        // 2. Add a seed-based "Environmental Variation"
        Aspect extra = getRandomMinorAspect(random);
        if (!aspects.contains(extra)) {
            aspects.add(extra);
        }

        return aspects.stream().limit(3).toList();
    }

    private static Aspect getRandomMinorAspect(Random random) {
        Aspect[] all = ModAspects.all().toArray(new Aspect[0]);
        return all[random.nextInt(all.length)];
    }
}
