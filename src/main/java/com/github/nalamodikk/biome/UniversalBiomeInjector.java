package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.region.BiomeClimateDefinition;
import com.github.nalamodikk.biome.region.BiomeInjectionEntry;
import com.github.nalamodikk.biome.region.BiomeRegionManager;
import com.github.nalamodikk.biome.region.SimpleBiomeRegion;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用生物群落注入器（相容層）
 * 目前委派到 BiomeRegionManager，保留舊 API 以避免破壞既有呼叫。
 */
public class UniversalBiomeInjector {

    private static final ResourceLocation DEFAULT_REGION_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "default_overworld");
    private static final int DEFAULT_REGION_WEIGHT = 10;

    private static final SimpleBiomeRegion DEFAULT_REGION =
            BiomeRegionManager.getOrCreateRegion(DEFAULT_REGION_ID, DEFAULT_REGION_WEIGHT);

    // 保留舊有統計用途
    private static final List<BiomeEntry> CUSTOM_BIOMES = new ArrayList<>();

    /**
     * 生物群落條目
     */
    public static class BiomeEntry {
        public final ResourceKey<Biome> biome;
        public final ClimateConfig climate;
        public final int weight;
        public final String description;
        public final int priority;

        public BiomeEntry(ResourceKey<Biome> biome, ClimateConfig climate, int weight, String description, int priority) {
            this.biome = biome;
            this.climate = climate;
            this.weight = Math.max(1, Math.min(20, weight));
            this.description = description;
            this.priority = priority;
        }
    }

    /**
     * 氣候配置類
     */
    public static class ClimateConfig {
        public final Climate.Parameter temperature;
        public final Climate.Parameter humidity;
        public final Climate.Parameter continentalness;
        public final Climate.Parameter erosion;
        public final Climate.Parameter depth;
        public final Climate.Parameter weirdness;
        public final float offset;

        private ClimateConfig(Climate.Parameter temperature, Climate.Parameter humidity,
                              Climate.Parameter continentalness, Climate.Parameter erosion,
                              Climate.Parameter depth, Climate.Parameter weirdness, float offset) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.depth = depth;
            this.weirdness = weirdness;
            this.offset = offset;
        }

        public static Builder builder() {
            return new Builder();
        }

        public BiomeClimateDefinition toDefinition() {
            return BiomeClimateDefinition.builder()
                    .temperature(Climate.unquantizeCoord(temperature.min()), Climate.unquantizeCoord(temperature.max()))
                    .humidity(Climate.unquantizeCoord(humidity.min()), Climate.unquantizeCoord(humidity.max()))
                    .continentalness(Climate.unquantizeCoord(continentalness.min()), Climate.unquantizeCoord(continentalness.max()))
                    .erosion(Climate.unquantizeCoord(erosion.min()), Climate.unquantizeCoord(erosion.max()))
                    .depth(Climate.unquantizeCoord(depth.min()), Climate.unquantizeCoord(depth.max()))
                    .weirdness(Climate.unquantizeCoord(weirdness.min()), Climate.unquantizeCoord(weirdness.max()))
                    .offset(offset)
                    .build();
        }

        public static ClimateConfig fromDefinition(BiomeClimateDefinition definition) {
            return ClimateConfig.builder()
                    .temperature(Climate.unquantizeCoord(definition.temperature().min()), Climate.unquantizeCoord(definition.temperature().max()))
                    .humidity(Climate.unquantizeCoord(definition.humidity().min()), Climate.unquantizeCoord(definition.humidity().max()))
                    .continentalness(Climate.unquantizeCoord(definition.continentalness().min()), Climate.unquantizeCoord(definition.continentalness().max()))
                    .erosion(Climate.unquantizeCoord(definition.erosion().min()), Climate.unquantizeCoord(definition.erosion().max()))
                    .depth(Climate.unquantizeCoord(definition.depth().min()), Climate.unquantizeCoord(definition.depth().max()))
                    .weirdness(Climate.unquantizeCoord(definition.weirdness().min()), Climate.unquantizeCoord(definition.weirdness().max()))
                    .offset((float) definition.offset() / 1000.0F)
                    .build();
        }

        public static class Builder {
            private Climate.Parameter temperature = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter humidity = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter continentalness = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter erosion = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter depth = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter weirdness = Climate.Parameter.span(-2.0F, 2.0F);
            private float offset = 0.0F;

            public Builder temperature(float min, float max) {
                this.temperature = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder humidity(float min, float max) {
                this.humidity = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder continentalness(float min, float max) {
                this.continentalness = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder erosion(float min, float max) {
                this.erosion = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder depth(float min, float max) {
                this.depth = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder weirdness(float min, float max) {
                this.weirdness = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder offset(float offset) {
                this.offset = offset;
                return this;
            }

            public ClimateConfig build() {
                return new ClimateConfig(temperature, humidity, continentalness, erosion, depth, weirdness, offset);
            }
        }
    }

    /**
     * 預設氣候配置
     */
    public static class ClimatePresets {
        public static final ClimateConfig TEMPERATE_PLAINS = ClimateConfig.builder()
                .temperature(0.6F, 0.8F)
                .humidity(0.4F, 0.7F)
                .continentalness(-0.1F, 0.3F)
                .erosion(-0.2F, 0.2F)
                .depth(0.0F, 0.4F)
                .weirdness(-0.5F, 0.5F)
                .build();

        public static final ClimateConfig MYSTICAL_FOREST = ClimateConfig.builder()
                .temperature(0.4F, 0.7F)
                .humidity(0.6F, 1.0F)
                .continentalness(0.1F, 0.5F)
                .erosion(-0.3F, 0.1F)
                .depth(-0.2F, 0.2F)
                .weirdness(0.3F, 1.0F)
                .build();

        public static final ClimateConfig COLD_HIGHLANDS = ClimateConfig.builder()
                .temperature(-0.5F, 0.2F)
                .humidity(0.2F, 0.6F)
                .continentalness(0.3F, 0.8F)
                .erosion(-0.1F, 0.3F)
                .depth(0.2F, 0.8F)
                .weirdness(-0.3F, 0.3F)
                .build();

        public static final ClimateConfig DESERT_OASIS = ClimateConfig.builder()
                .temperature(0.8F, 1.2F)
                .humidity(0.1F, 0.4F)
                .continentalness(-0.2F, 0.2F)
                .erosion(-0.4F, -0.1F)
                .depth(-0.3F, 0.0F)
                .weirdness(0.1F, 0.6F)
                .build();
    }

    public static void registerBiome(ResourceKey<Biome> biome, ClimateConfig climate, int weight, String description) {
        registerBiome(biome, climate, weight, description, 0);
    }

    public static void registerBiome(ResourceKey<Biome> biome, ClimateConfig climate, String description) {
        registerBiome(biome, climate, 5, description, 0);
    }

    public static void registerBiome(ResourceKey<Biome> biome, ClimateConfig climate, int weight, String description, int priority) {
        registerBiomeToRegion(DEFAULT_REGION_ID, DEFAULT_REGION_WEIGHT, biome, climate, weight, description, priority);
    }

    public static void registerBiomeToRegion(ResourceLocation regionId, int regionWeight, ResourceKey<Biome> biome,
                                             ClimateConfig climate, int weight, String description, int priority) {
        BiomeEntry entry = new BiomeEntry(biome, climate, weight, description, priority);
        CUSTOM_BIOMES.add(entry);

        SimpleBiomeRegion region = BiomeRegionManager.getOrCreateRegion(regionId, regionWeight);
        BiomeInjectionEntry injectedEntry = new BiomeInjectionEntry(
                biome,
                climate.toDefinition(),
                weight,
                description,
                priority,
                regionId.getNamespace()
        );
        region.registerEntry(injectedEntry);

        KoniavacraftMod.LOGGER.info("註冊生物群落: {} (region: {}, 權重: {}, 優先級: {}) - {}",
                biome.location(), regionId, weight, priority, description);
    }

    public static void injectBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer) {
        KoniavacraftMod.LOGGER.info("UniversalBiomeInjector: 開始注入 {} 個自訂生物群落...", CUSTOM_BIOMES.size());
        int injectedCount = BiomeRegionManager.injectBiomes(consumer);
        KoniavacraftMod.LOGGER.info("生物群落注入完成，共輸出 {} 個 climate points", injectedCount);
    }

    public static List<BiomeEntry> getRegisteredBiomes() {
        return new ArrayList<>(CUSTOM_BIOMES);
    }

    public static void clearAll() {
        CUSTOM_BIOMES.clear();
        DEFAULT_REGION.clear();
        BiomeRegionManager.clearAllRegions();
        BiomeRegionManager.registerRegion(DEFAULT_REGION);
        KoniavacraftMod.LOGGER.info("清空所有自訂生物群落註冊");
    }
}
