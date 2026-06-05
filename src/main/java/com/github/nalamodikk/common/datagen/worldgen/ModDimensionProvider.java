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

        // 月球：灰色月壤，純黑天空，無大氣無降水無生物
        context.register(ModDimensions.MOON_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.0f)
                .downfall(0.0f)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(0x383844)
                        .waterFogColor(0x202028)
                        .fogColor(0x000000)
                        .skyColor(0x000000)
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

        // 月球：固定為夜晚（看得到星空與地球），無天光，coordinateScale=1
        context.register(ModDimensions.MOON_TYPE, new DimensionType(
                OptionalLong.of(18000),   // 固定時間（黑天）
                false,  // hasSkyLight：無太陽光照，全靠方塊光
                false,  // hasCeiling
                false,  // ultraWarm
                false,  // natural（false → 不能用床、指南針旋轉等）
                1.0,    // coordinateScale
                false,  // bedWorks
                false,  // respawnAnchorWorks
                -64, 384, 384,
                BlockTags.INFINIBURN_OVERWORLD,
                ModDimensions.MOON_EFFECTS,
                0.05f,  // ambientLight：微光，地表不全黑
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

        // ── 月球：noise 丘陵 + 隕石坑（MoonChunkGenerator）──────────────
        var moonType  = dimTypes.getOrThrow(ModDimensions.MOON_TYPE);
        var moonBiome = biomes.getOrThrow(ModDimensions.MOON_BIOME);
        var moonBiomeSource = new net.minecraft.world.level.biome.FixedBiomeSource(moonBiome);
        context.register(ModDimensions.MOON_STEM,
                new LevelStem(moonType, new com.github.nalamodikk.dimension.MoonChunkGenerator(moonBiomeSource)));
    }
}
