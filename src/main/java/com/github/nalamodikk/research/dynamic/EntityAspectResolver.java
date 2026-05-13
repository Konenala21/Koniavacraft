package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.WorldAspectSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import java.util.ArrayList;
import java.util.List;

public class EntityAspectResolver {

    public static List<Aspect> resolve(Entity entity, ServerLevel level) {
        if (entity == null) {
            return List.of();
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        WorldAspectSavedData data = WorldAspectSavedData.get(level);
        List<Aspect> cached = data.getEntityMapping(id);
        if (cached != null) {
            return cached;
        }

        List<Aspect> aspects = new ArrayList<>();
        List<Aspect> candidates = new ArrayList<>();
        addClassificationAspects(entity, aspects, candidates);
        addKeywordCandidates(id, aspects, candidates);

        List<Aspect> result = AspectExpression.express(id, aspects, candidates, data.getGenomeSeed(), capacity(id));
        if (result.isEmpty()) {
            result = AspectExpression.fallback(id, data.getGenomeSeed(), 3);
        }
        data.putEntityMapping(id, result);
        return result;
    }

    private static void addClassificationAspects(Entity entity, List<Aspect> aspects, List<Aspect> candidates) {
        if (entity instanceof LivingEntity livingEntity) {
            addLivingEntityAspects(livingEntity, aspects, candidates);
            return;
        }

        if (entity instanceof AbstractMinecart) {
            aspects.add(ModAspects.METAL);
            aspects.add(ModAspects.MECHANISM);
            candidates.add(ModAspects.MOMENTUM);
            candidates.add(ModAspects.EARTH);
            return;
        }

        if (entity instanceof Projectile) {
            aspects.add(ModAspects.ENERGY);
            aspects.add(ModAspects.MOMENTUM);
            candidates.add(ModAspects.WOOD);
            return;
        }

        aspects.add(ModAspects.EARTH);
        candidates.add(ModAspects.MECHANISM);
    }

    private static void addLivingEntityAspects(LivingEntity entity, List<Aspect> aspects, List<Aspect> candidates) {
        if (entity.getType().is(EntityTypeTags.UNDEAD)) {
            aspects.add(ModAspects.ANIMA);
            aspects.add(ModAspects.WU);
            candidates.add(ModAspects.CORROSION);
            candidates.add(ModAspects.EARTH);
            candidates.add(ModAspects.UNDEAD);
            candidates.add(ModAspects.DEATH);
        } else if (entity.getType().is(EntityTypeTags.ARTHROPOD)) {
            aspects.add(ModAspects.VITALITY);
            aspects.add(ModAspects.CORROSION);
            candidates.add(ModAspects.WOOD);
            candidates.add(ModAspects.EARTH);
            candidates.add(ModAspects.VENOM);
            candidates.add(ModAspects.INSTINCT);
        } else if (entity.getType().is(EntityTypeTags.AQUATIC)) {
            aspects.add(ModAspects.WATER);
            aspects.add(ModAspects.VITALITY);
            candidates.add(ModAspects.GROWTH);
            candidates.add(ModAspects.PRIMORDIAL);
        } else if (entity.getType().is(EntityTypeTags.ILLAGER)) {
            aspects.add(ModAspects.ANIMA);
            candidates.add(ModAspects.ENERGY);
            candidates.add(ModAspects.WU);
            candidates.add(ModAspects.COGNITION);
            candidates.add(ModAspects.DESIRE);
        } else if (entity instanceof Creeper) {
            aspects.add(ModAspects.ENERGY);
            aspects.add(ModAspects.PHLOGISTON);
            candidates.add(ModAspects.RESONANCE);
            candidates.add(ModAspects.STORM);
        } else if (entity instanceof EnderMan) {
            aspects.add(ModAspects.WU);
            aspects.add(ModAspects.GRAVITY);
            candidates.add(ModAspects.ANIMA);
            candidates.add(ModAspects.ELDRITCH);
            candidates.add(ModAspects.VOID_ASPECT);
        } else {
            aspects.add(ModAspects.VITALITY);
            aspects.add(ModAspects.EARTH);
            candidates.add(ModAspects.WOOD);
            candidates.add(ModAspects.GROWTH);
            candidates.add(ModAspects.BESTIA);
            candidates.add(ModAspects.CORPUS);
        }
    }

    private static void addKeywordCandidates(ResourceLocation id, List<Aspect> aspects, List<Aspect> candidates) {
        String path = id.getPath();
        if (path.contains("blaze") || path.contains("magma")) {
            AspectExpression.addUnique(aspects, ModAspects.FIRE);
            AspectExpression.addUnique(candidates, ModAspects.ENERGY);
            AspectExpression.addUnique(candidates, ModAspects.ARCANA);
        }
        if (path.contains("slime")) {
            AspectExpression.addUnique(candidates, ModAspects.WATER);
            AspectExpression.addUnique(candidates, ModAspects.VITALITY);
            AspectExpression.addUnique(candidates, ModAspects.PRIMORDIAL);
        }
        if (path.contains("warden") || path.contains("sculk")) {
            AspectExpression.addUnique(aspects, ModAspects.WU);
            AspectExpression.addUnique(candidates, ModAspects.RESONANCE);
            AspectExpression.addUnique(candidates, ModAspects.BINDING);
        }
        if (path.contains("dragon") || path.contains("ender")) {
            AspectExpression.addUnique(aspects, ModAspects.WU);
            AspectExpression.addUnique(candidates, ModAspects.GRAVITY);
            AspectExpression.addUnique(candidates, ModAspects.ELDRITCH);
            AspectExpression.addUnique(candidates, ModAspects.VOID_ASPECT);
        }
        if (path.contains("golem")) {
            AspectExpression.addUnique(aspects, ModAspects.METAL);
            AspectExpression.addUnique(candidates, ModAspects.MECHANISM);
            AspectExpression.addUnique(candidates, ModAspects.FORTIFY);
        }
        if (path.contains("witch")) {
            AspectExpression.addUnique(candidates, ModAspects.ALCHEMY);
            AspectExpression.addUnique(candidates, ModAspects.VENOM);
        }
        if (path.contains("villager") || path.contains("trader")) {
            AspectExpression.addUnique(candidates, ModAspects.HUMANITY);
            AspectExpression.addUnique(candidates, ModAspects.COMMERCE);
        }
        if (path.contains("phantom")) {
            AspectExpression.addUnique(candidates, ModAspects.FLIGHT);
            AspectExpression.addUnique(candidates, ModAspects.SHADOW);
        }
        if (path.contains("ghast") || path.contains("shulker")) {
            AspectExpression.addUnique(candidates, ModAspects.FLIGHT);
            AspectExpression.addUnique(candidates, ModAspects.BINDING);
        }
    }

    private static int capacity(ResourceLocation id) {
        String path = id.getPath();
        if (path.contains("dragon") || path.contains("wither") || path.contains("warden")) {
            return 5;
        }
        if (path.contains("blaze") || path.contains("ender") || path.contains("golem")) {
            return 4;
        }
        return 3;
    }
}
