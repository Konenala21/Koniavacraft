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
        } else if (entity.getType().is(EntityTypeTags.ARTHROPOD)) {
            aspects.add(ModAspects.VITALITY);
            aspects.add(ModAspects.CORROSION);
            candidates.add(ModAspects.WOOD);
            candidates.add(ModAspects.EARTH);
        } else if (entity.getType().is(EntityTypeTags.AQUATIC)) {
            aspects.add(ModAspects.WATER);
            aspects.add(ModAspects.VITALITY);
            candidates.add(ModAspects.GROWTH);
        } else if (entity.getType().is(EntityTypeTags.ILLAGER)) {
            aspects.add(ModAspects.ANIMA);
            candidates.add(ModAspects.ENERGY);
            candidates.add(ModAspects.WU);
        } else if (entity instanceof Creeper) {
            aspects.add(ModAspects.ENERGY);
            aspects.add(ModAspects.PHLOGISTON);
            candidates.add(ModAspects.RESONANCE);
        } else if (entity instanceof EnderMan) {
            aspects.add(ModAspects.WU);
            aspects.add(ModAspects.GRAVITY);
            candidates.add(ModAspects.ANIMA);
        } else {
            aspects.add(ModAspects.VITALITY);
            aspects.add(ModAspects.EARTH);
            candidates.add(ModAspects.WOOD);
            candidates.add(ModAspects.GROWTH);
        }
    }

    private static void addKeywordCandidates(ResourceLocation id, List<Aspect> aspects, List<Aspect> candidates) {
        String path = id.getPath();
        if (path.contains("blaze") || path.contains("magma")) {
            AspectExpression.addUnique(aspects, ModAspects.FIRE);
            AspectExpression.addUnique(candidates, ModAspects.ENERGY);
        }
        if (path.contains("slime")) {
            AspectExpression.addUnique(candidates, ModAspects.WATER);
            AspectExpression.addUnique(candidates, ModAspects.VITALITY);
        }
        if (path.contains("warden") || path.contains("sculk")) {
            AspectExpression.addUnique(aspects, ModAspects.WU);
            AspectExpression.addUnique(candidates, ModAspects.RESONANCE);
        }
        if (path.contains("dragon") || path.contains("ender")) {
            AspectExpression.addUnique(aspects, ModAspects.WU);
            AspectExpression.addUnique(candidates, ModAspects.GRAVITY);
        }
        if (path.contains("golem")) {
            AspectExpression.addUnique(aspects, ModAspects.METAL);
            AspectExpression.addUnique(candidates, ModAspects.MECHANISM);
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
