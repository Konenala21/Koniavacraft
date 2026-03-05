package com.github.nalamodikk.biome.region;

import com.github.nalamodikk.biome.data.BiomeClimateConfigLoader;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Central registry for region-based biome injection.
 */
public final class BiomeRegionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, SimpleBiomeRegion> REGIONS = new ConcurrentHashMap<>();

    private BiomeRegionManager() {
    }

    public static SimpleBiomeRegion getOrCreateRegion(ResourceLocation id, int weight) {
        return REGIONS.computeIfAbsent(id, key -> new SimpleBiomeRegion(key, weight));
    }

    public static void registerRegion(SimpleBiomeRegion region) {
        REGIONS.put(region.id(), region);
    }

    public static void clearAllRegions() {
        REGIONS.clear();
    }

    public static int injectBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer) {
        List<SimpleBiomeRegion> regions = new ArrayList<>(REGIONS.values());
        regions.sort(Comparator
                .comparingInt(SimpleBiomeRegion::weight).reversed()
                .thenComparing(region -> region.id().toString()));

        Set<ResourceKey<Biome>> knownBiomes = new HashSet<>();
        BiomeParameterOverlayBuilder overlayBuilder = new BiomeParameterOverlayBuilder();

        for (SimpleBiomeRegion region : regions) {
            region.forEachEntry(entry -> {
                if (BiomeClimateConfigLoader.isDisabled(entry.biome())) {
                    return;
                }
                knownBiomes.add(entry.biome());
                overlayBuilder.addEntry(BiomeClimateConfigLoader.resolveOverride(entry));
            });
        }

        for (BiomeInjectionEntry dynamicEntry : BiomeClimateConfigLoader.getAdditionalEntries(knownBiomes)) {
            overlayBuilder.addEntry(dynamicEntry);
        }

        int emitted = overlayBuilder.emit(consumer);
        LOGGER.debug(
                "Injected {} biome parameter points from {} regions (replaced collisions={}, skipped collisions={})",
                emitted,
                regions.size(),
                overlayBuilder.getCollisionReplaced(),
                overlayBuilder.getCollisionSkipped()
        );
        return emitted;
    }
}
