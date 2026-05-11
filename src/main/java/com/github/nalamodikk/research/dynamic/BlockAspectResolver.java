package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.WorldAspectSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class BlockAspectResolver {

    public static List<Aspect> resolve(BlockState state, ServerLevel level) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        WorldAspectSavedData data = WorldAspectSavedData.get(level);
        List<Aspect> cached = data.getBlockMapping(id);
        if (cached != null) {
            return cached;
        }

        List<Aspect> aspects = inferFromState(state, id, data.getGenomeSeed());
        if (aspects.isEmpty()) {
            Item item = state.getBlock().asItem();
            if (item != Items.AIR) {
                aspects = RecipeInferenceEngine.resolve(item, level);
            }
        }
        if (aspects.isEmpty()) {
            aspects = AspectExpression.fallback(id, data.getGenomeSeed(), 2);
        }
        if (!aspects.isEmpty()) {
            data.putBlockMapping(id, aspects);
        }
        return aspects;
    }

    private static List<Aspect> inferFromState(BlockState state, ResourceLocation id, long genomeSeed) {
        List<Aspect> base = new ArrayList<>();
        List<Aspect> candidates = new ArrayList<>();

        if (state.isAir()) {
            return List.of();
        }
        if (state.getFluidState().is(FluidTags.WATER)) {
            AspectExpression.addUnique(base, ModAspects.WATER);
        }
        if (state.getFluidState().is(FluidTags.LAVA)) {
            AspectExpression.addUnique(base, ModAspects.FIRE);
            AspectExpression.addUnique(candidates, ModAspects.MAGMA);
            AspectExpression.addUnique(candidates, ModAspects.EARTH);
        }
        addTagAspects(state, base, candidates);
        addKeywordAspects(id, base, candidates);
        if (state.getLightEmission() > 0) {
            AspectExpression.addUnique(candidates, ModAspects.RADIANCE);
            AspectExpression.addUnique(candidates, ModAspects.ENERGY);
        }
        if (state.hasBlockEntity()) {
            AspectExpression.addUnique(candidates, ModAspects.MECHANISM);
        }
        if (KoniavacraftMod.MOD_ID.equals(id.getNamespace())) {
            AspectExpression.addUnique(candidates, ModAspects.MANA);
        }
        if (base.isEmpty() && !candidates.isEmpty()) {
            AspectExpression.addUnique(base, candidates.remove(0));
        }
        if (base.isEmpty()) {
            return List.of();
        }
        return AspectExpression.express(id, base, candidates, genomeSeed, capacity(id, state));
    }

    private static void addTagAspects(BlockState state, List<Aspect> base, List<Aspect> candidates) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) {
            AspectExpression.addUnique(base, ModAspects.WOOD);
            AspectExpression.addUnique(candidates, ModAspects.GROWTH);
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)) {
            AspectExpression.addUnique(base, ModAspects.WOOD);
            AspectExpression.addUnique(candidates, ModAspects.VITALITY);
            AspectExpression.addUnique(candidates, ModAspects.GROWTH);
        }
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            AspectExpression.addUnique(base, ModAspects.EARTH);
            AspectExpression.addUnique(candidates, ModAspects.GRAVITY);
        }
        if (state.is(BlockTags.IRON_ORES) || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.COPPER_ORES)) {
            AspectExpression.addUnique(base, ModAspects.EARTH);
            AspectExpression.addUnique(candidates, ModAspects.METAL);
        }
        if (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES) || state.is(BlockTags.LAPIS_ORES)) {
            AspectExpression.addUnique(base, ModAspects.EARTH);
            AspectExpression.addUnique(candidates, ModAspects.CRYSTAL);
            AspectExpression.addUnique(candidates, ModAspects.RADIANCE);
        }
        if (state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.RAILS) || state.is(BlockTags.BUTTONS) || state.is(BlockTags.PRESSURE_PLATES)) {
            AspectExpression.addUnique(base, ModAspects.MECHANISM);
            AspectExpression.addUnique(candidates, ModAspects.RESONANCE);
            AspectExpression.addUnique(candidates, ModAspects.ENERGY);
        }
        if (state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.CANDLES)) {
            AspectExpression.addUnique(base, ModAspects.FIRE);
            AspectExpression.addUnique(candidates, ModAspects.ENERGY);
            AspectExpression.addUnique(candidates, ModAspects.RADIANCE);
        }
        if (state.is(BlockTags.ICE) || state.is(BlockTags.SNOW)) {
            AspectExpression.addUnique(base, ModAspects.WATER);
            AspectExpression.addUnique(candidates, ModAspects.CRYSTAL);
        }
        if (state.is(BlockTags.PORTALS) || state.is(BlockTags.ENDERMAN_HOLDABLE)) {
            AspectExpression.addUnique(candidates, ModAspects.WU);
        }
        if (state.is(BlockTags.SCULK_REPLACEABLE) || state.is(BlockTags.VIBRATION_RESONATORS)) {
            AspectExpression.addUnique(candidates, ModAspects.WU);
            AspectExpression.addUnique(candidates, ModAspects.RESONANCE);
        }
    }

    private static void addKeywordAspects(ResourceLocation id, List<Aspect> base, List<Aspect> candidates) {
        String path = id.getPath();
        if (path.contains("stone") || path.contains("rock") || path.contains("slate")) {
            AspectExpression.addUnique(base, ModAspects.EARTH);
            AspectExpression.addUnique(candidates, ModAspects.GRAVITY);
        }
        if (path.contains("ore")) {
            AspectExpression.addUnique(base, ModAspects.EARTH);
            AspectExpression.addUnique(candidates, ModAspects.METAL);
        }
        if (path.contains("crystal") || path.contains("amethyst") || path.contains("glass")) {
            AspectExpression.addUnique(base, ModAspects.CRYSTAL);
            AspectExpression.addUnique(candidates, ModAspects.RADIANCE);
        }
        if (path.contains("redstone") || path.contains("sculk") || path.contains("sensor")) {
            AspectExpression.addUnique(base, ModAspects.RESONANCE);
            AspectExpression.addUnique(candidates, ModAspects.WU);
        }
        if (path.contains("soul") || path.contains("end") || path.contains("portal")) {
            AspectExpression.addUnique(candidates, ModAspects.WU);
            AspectExpression.addUnique(candidates, ModAspects.ANIMA);
        }
        if (path.contains("mana") || path.contains("magic") || path.contains("arcane")) {
            AspectExpression.addUnique(base, ModAspects.MANA);
        }
    }

    private static int capacity(ResourceLocation id, BlockState state) {
        if (KoniavacraftMod.MOD_ID.equals(id.getNamespace()) || state.hasBlockEntity()) {
            return 4;
        }
        if (state.getLightEmission() > 0 || id.getPath().contains("redstone")) {
            return 3;
        }
        return 2;
    }

    private BlockAspectResolver() {}
}
