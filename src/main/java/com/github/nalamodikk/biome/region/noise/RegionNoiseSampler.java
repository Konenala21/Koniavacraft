package com.github.nalamodikk.biome.region.noise;

import com.github.nalamodikk.biome.region.SimpleBiomeRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Maps block coordinates to a region index using a low-frequency {@link NormalNoise},
 * producing large, continent-scale biome regions with smooth, organic boundaries.
 *
 * <p>This replaces the Zoom-Layer approach for {@link com.github.nalamodikk.biome.region.PlacementMode#SMOOTH_NOISE}
 * regions. It requires no external dependencies — only vanilla's built-in noise infrastructure.
 *
 * <h3>Region size tuning</h3>
 * Feature size ≈ {@code 2^7 / REGION_SCALE} blocks.
 * <ul>
 *   <li>{@code 0.04} (default) → ~3200-block regions</li>
 *   <li>{@code 0.02}           → ~6400-block regions (continent scale)</li>
 *   <li>{@code 0.08}           → ~1600-block regions (smaller, more frequent)</li>
 * </ul>
 *
 * <h3>Weight semantics</h3>
 * Coverage of a region = {@code region.weight() / (vanillaWeight + Σ region.weight())}.
 * Positions not assigned to any custom region return {@code 0} (vanilla / fall-through to
 * Zoom-Layer system).
 */
public class RegionNoiseSampler {

    /**
     * Spatial scale applied to block coordinates before sampling.
     * Decrease to enlarge regions; increase to shrink them.
     * Default produces ~3200-block smooth regions.
     */
    public static final double REGION_SCALE = 0.04;

    private final NormalNoise noise;
    private final int vanillaWeight;
    private final List<SimpleBiomeRegion> regions; // sorted by uniquenessIndex
    private final int totalWeight;

    /**
     * @param seed          world seed
     * @param regions       all SMOOTH_NOISE regions, in any order
     * @param vanillaWeight weight representing "not a custom noise region"
     */
    public RegionNoiseSampler(long seed, List<SimpleBiomeRegion> regions, int vanillaWeight) {
        this.vanillaWeight = vanillaWeight;
        this.regions = new ArrayList<>(regions);
        this.regions.sort(Comparator.comparingInt(SimpleBiomeRegion::uniquenessIndex));
        this.totalWeight = vanillaWeight + regions.stream().mapToInt(SimpleBiomeRegion::weight).sum();

        // Single-octave NormalNoise at firstOctave=-7 for smooth, large-scale shapes.
        // XOR with a constant to keep the region noise decorrelated from other world noises.
        RandomSource random = RandomSource.create(seed ^ 0x6A7D_3B9C_F1E2_4D8AL);
        this.noise = NormalNoise.create(random, new NormalNoise.NoiseParameters(-7, 1.0));
    }

    /**
     * Return the region index at the given block position.
     *
     * @return {@code 0} if the position belongs to vanilla / no custom noise region,
     *         {@code 1..N} for a custom SMOOTH_NOISE region
     */
    public int getRegionIndex(int blockX, int blockZ) {
        double value = noise.getValue(blockX * REGION_SCALE, 0, blockZ * REGION_SCALE);
        // NormalNoise output is roughly in [-1, 1]; clamp for safety
        double normalized = Math.clamp((value + 1.0) * 0.5, 0.0, 1.0);

        int slot = (int) (normalized * totalWeight);

        if (slot < vanillaWeight) return 0;

        int remaining = slot - vanillaWeight;
        for (SimpleBiomeRegion region : regions) {
            if (remaining < region.weight()) return region.uniquenessIndex();
            remaining -= region.weight();
        }
        return 0; // safety fallback
    }
}
