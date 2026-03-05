package com.github.nalamodikk.biome.region;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * One biome injection request contributed by a region or a datapack override.
 */
public record BiomeInjectionEntry(
        ResourceKey<Biome> biome,
        BiomeClimateDefinition climate,
        int weight,
        String description,
        int priority,
        String sourceNamespace
) {
    public BiomeInjectionEntry {
        if (biome == null) {
            throw new IllegalArgumentException("biome cannot be null");
        }
        if (climate == null) {
            throw new IllegalArgumentException("climate cannot be null");
        }
        weight = Math.max(1, Math.min(20, weight));
        description = description == null ? "" : description;
        sourceNamespace = sourceNamespace == null ? "" : sourceNamespace;
    }

    public int injectionCount() {
        return Math.max(1, Math.min(8, (int) Math.ceil(weight / 2.0D)));
    }

    public BiomeInjectionEntry withClimate(BiomeClimateDefinition newClimate) {
        return new BiomeInjectionEntry(biome, newClimate, weight, description, priority, sourceNamespace);
    }

    public BiomeInjectionEntry withWeight(int newWeight) {
        return new BiomeInjectionEntry(biome, climate, newWeight, description, priority, sourceNamespace);
    }

    public BiomeInjectionEntry withDescription(String newDescription) {
        return new BiomeInjectionEntry(biome, climate, weight, newDescription, priority, sourceNamespace);
    }

    public BiomeInjectionEntry withPriority(int newPriority) {
        return new BiomeInjectionEntry(biome, climate, weight, description, newPriority, sourceNamespace);
    }

    public BiomeInjectionEntry withNamespace(String newNamespace) {
        return new BiomeInjectionEntry(biome, climate, weight, description, priority, newNamespace);
    }

    // ==================== Static Factory Methods ====================

    /**
     * 從 {@link ParameterPointListBuilder} 的笛卡爾積結果建立多個 entries。
     * 每個 ParameterPoint 對應一個獨立的 BiomeInjectionEntry，
     * 取代舊的 injectionCount + offsetJitter 偽多點機制。
     */
    public static List<BiomeInjectionEntry> fromBuilder(
            ResourceKey<Biome> biome,
            ParameterPointListBuilder builder,
            int weight,
            String description,
            int priority,
            String sourceNamespace) {
        return builder.buildAsDefinitions().stream()
                .map(def -> new BiomeInjectionEntry(biome, def, weight, description, priority, sourceNamespace))
                .collect(Collectors.toList());
    }

    /**
     * 從單一 {@link Climate.ParameterPoint} 建立一個 entry。
     * 供 {@link VanillaBiomeParameterReader#addBiomeSimilar} 使用。
     */
    public static BiomeInjectionEntry fromPoint(
            ResourceKey<Biome> biome,
            Climate.ParameterPoint point,
            int weight,
            String description,
            int priority,
            String sourceNamespace) {
        BiomeClimateDefinition def = new BiomeClimateDefinition(
                point.temperature(), point.humidity(), point.continentalness(),
                point.erosion(), point.depth(), point.weirdness(), point.offset());
        return new BiomeInjectionEntry(biome, def, weight, description, priority, sourceNamespace);
    }
}
