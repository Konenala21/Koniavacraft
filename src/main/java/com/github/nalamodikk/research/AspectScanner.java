package com.github.nalamodikk.research;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.dynamic.BlockAspectResolver;
import com.github.nalamodikk.research.dynamic.EntityAspectResolver;
import com.github.nalamodikk.research.dynamic.RecipeInferenceEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Proxy for the new Dynamic Aspect System.
 * Bridging the gap between the old static scanner and the new inference engine.
 */
public final class AspectScanner {

    public static List<Aspect> getAspectsFor(BlockState state, ServerLevel level) {
        return BlockAspectResolver.resolve(state, level);
    }

    public static List<Aspect> getAspectsForFluid(FluidState state, ServerLevel level) {
        return BlockAspectResolver.resolveFluid(state, level);
    }

    public static List<Aspect> getAspectsForItem(Item item, ServerLevel level) {
        return RecipeInferenceEngine.resolve(item, level);
    }

    public static List<Aspect> getAspectsForEntity(Entity entity, ServerLevel level) {
        return EntityAspectResolver.resolve(entity, level);
    }

    private AspectScanner() {}
}
