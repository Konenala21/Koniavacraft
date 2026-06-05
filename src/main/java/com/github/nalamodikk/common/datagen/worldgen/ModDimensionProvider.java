package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import com.github.nalamodikk.dimension.BoundedFlatChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public class ModDimensionProvider {

    public static void bootstrapBiome(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> worldCarvers = context.lookup(Registries.CONFIGURED_CARVER);

        context.register(ModDimensions.SPACE_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.0f)
                .downfall(0.0f)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(0x000010)
                        .waterFogColor(0x000010)
                        .fogColor(0x000000)
                        .skyColor(0x000000)
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers).build())
                .build());

        context.register(ModDimensions.VOID_MIRROR_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5f)
                .downfall(0.0f)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(0x808080)
                        .waterFogColor(0x606060)
                        .fogColor(0x404040)
                        .skyColor(0x202020)
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers).build())
                .build());
    }

    public static void bootstrapDimensionType(BootstrapContext<DimensionType> context) {
        context.register(ModDimensions.SPACE_TYPE, new DimensionType(
                OptionalLong.of(18000),
                false, false, false, false,
                1.0, false, false,
                -64, 384, 384,
                BlockTags.INFINIBURN_OVERWORLD,
                ModDimensions.SPACE_EFFECTS,
                0.0f,
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));
        context.register(ModDimensions.VOID_MIRROR_TYPE, new DimensionType(
                OptionalLong.of(6000),
                false,
                false,
                false,
                false,
                1.0,
                false,
                false,
                0,
                256,
                256,
                BlockTags.INFINIBURN_OVERWORLD,
                ModDimensions.VOID_MIRROR_EFFECTS,
                1.0f,
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));
    }

    public static void bootstrapLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        var spaceType  = dimTypes.getOrThrow(ModDimensions.SPACE_TYPE);
        var spaceBiome = biomes.getOrThrow(ModDimensions.SPACE_BIOME);
        // 純虛空：不加任何層，不呼叫 updateLayers()，讓太空維度沒有地板
        FlatLevelGeneratorSettings spaceFlat = new FlatLevelGeneratorSettings(Optional.empty(), spaceBiome, List.of());
        context.register(ModDimensions.SPACE_STEM, new LevelStem(spaceType, new BoundedFlatChunkGenerator(spaceFlat)));

        var dimType = dimTypes.getOrThrow(ModDimensions.VOID_MIRROR_TYPE);
        var biome = biomes.getOrThrow(ModDimensions.VOID_MIRROR_BIOME);

        FlatLevelGeneratorSettings flat = new FlatLevelGeneratorSettings(Optional.empty(), biome, List.of());
        flat.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));
        flat.getLayersInfo().add(new FlatLayerInfo(59, Blocks.STONE));
        flat.getLayersInfo().add(new FlatLayerInfo(3, Blocks.DIRT));
        flat.getLayersInfo().add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));
        flat.updateLayers();

        context.register(ModDimensions.VOID_MIRROR_STEM, new LevelStem(dimType, new BoundedFlatChunkGenerator(flat)));
    }
}
