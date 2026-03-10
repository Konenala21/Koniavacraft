package com.github.nalamodikk.biome.region;

import com.github.nalamodikk.biome.data.BiomeClimateConfigLoader;
import com.github.nalamodikk.biome.region.noise.Area;
import com.github.nalamodikk.biome.region.noise.RegionNoiseUtil;
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
 *
 * <p>Each {@link SimpleBiomeRegion} is assigned a unique integer index (starting at 1;
 * 0 is reserved for vanilla).  {@link #initForWorld(long)} builds a Zoom-Layer {@link Area}
 * from all registered regions and the world seed.  {@link #getRegionIndex(int, int)} then
 * maps any block position to {@code 0} (vanilla) or {@code 1..N} (custom region).
 */
public final class BiomeRegionManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, SimpleBiomeRegion> REGIONS = new ConcurrentHashMap<>();
    private static final Map<Integer, SimpleBiomeRegion> INDEX_TO_REGION = new ConcurrentHashMap<>();

    // Index counter — protected by the class monitor via synchronized getOrCreateRegion
    private static int nextRegionIndex = 1;

    // Tuneable parameters (set before world load)
    private static volatile int vanillaWeight = 10;
    private static volatile int zoomCount = 4;

    // Built on LevelEvent.Load; null while no world is loaded (main menu, etc.)
    private static volatile Area regionArea = null;

    // Cached once any region has entries — avoids allocations in hot biome-query path
    private static volatile boolean hasCustomRegionsCached = false;

    private BiomeRegionManager() {}

    // ===================== configuration =====================

    /** Set the weight of vanilla biomes relative to custom regions (default 10). */
    public static void setVanillaWeight(int w) {
        vanillaWeight = Math.max(1, w);
    }

    /**
     * Set the number of NORMAL zoom passes (clamped to [1, 8]; default 4).
     * Higher → larger patches.  Approximate patch size: {@code 2^(zoomCount+2)} blocks.
     */
    public static void setZoomCount(int z) {
        zoomCount = Math.max(1, Math.min(8, z));
    }

    // ===================== region registration =====================

    /**
     * Return an existing region by id, or create a new one with {@code weight} and an
     * auto-assigned uniqueness index.
     */
    public static synchronized SimpleBiomeRegion getOrCreateRegion(ResourceLocation id, int weight) {
        SimpleBiomeRegion existing = REGIONS.get(id);
        if (existing != null) return existing;

        int idx = nextRegionIndex++;
        SimpleBiomeRegion region = new SimpleBiomeRegion(id, weight, idx);
        REGIONS.put(id, region);
        INDEX_TO_REGION.put(idx, region);
        return region;
    }

    public static synchronized void registerRegion(SimpleBiomeRegion region) {
        REGIONS.put(region.id(), region);
        INDEX_TO_REGION.put(region.uniquenessIndex(), region);
    }

    public static synchronized void clearAllRegions() {
        REGIONS.clear();
        INDEX_TO_REGION.clear();
        nextRegionIndex = 1;
        regionArea = null;
        hasCustomRegionsCached = false;
    }

    // ===================== world init =====================

    /**
     * Build (or rebuild) the zoom-layer Area for the given world seed.
     * Must be called from the server thread on {@code LevelEvent.Load} for the Overworld.
     */
    public static void initForWorld(long seed) {
        List<SimpleBiomeRegion> regions = new ArrayList<>(REGIONS.values());
        regions.sort(Comparator.comparingInt(SimpleBiomeRegion::uniquenessIndex));
        regionArea = RegionNoiseUtil.build(regions, vanillaWeight, seed, zoomCount);
        LOGGER.info("Koniava: built region area ({} custom regions, vanillaWeight={}, zoomCount={}, seed={})",
                regions.size(), vanillaWeight, zoomCount, seed);
    }

    // ===================== runtime queries =====================

    /**
     * Return the region index at block position (blockX, blockZ).
     *
     * @return {@code 0} if vanilla (or if {@link #initForWorld} has not been called yet),
     *         {@code 1..N} for a custom region
     */
    public static int getRegionIndex(int blockX, int blockZ) {
        Area area = regionArea;
        if (area == null) return 0;
        return area.get(blockX, blockZ);
    }

    /** Look up a region by its uniqueness index; returns {@code null} if unknown. */
    public static SimpleBiomeRegion getRegionByIndex(int idx) {
        return INDEX_TO_REGION.get(idx);
    }

    // ===================== helpers used by other systems =====================

    /** Returns true if any custom region has registered entries. */
    public static boolean hasCustomRegions() {
        if (hasCustomRegionsCached) return true;
        boolean result = REGIONS.values().stream().anyMatch(SimpleBiomeRegion::hasEntries);
        if (result) hasCustomRegionsCached = true;
        return result;
    }

    /** Iterates every {@link BiomeInjectionEntry} across all regions. */
    public static void forEachEntry(Consumer<BiomeInjectionEntry> consumer) {
        REGIONS.values().forEach(region -> region.forEachEntry(consumer));
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
