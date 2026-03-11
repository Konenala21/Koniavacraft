package com.github.nalamodikk.biome.region;

/**
 * Controls how a {@link SimpleBiomeRegion} is spatially placed in the world.
 *
 * <ul>
 *   <li>{@link #ZOOM_LAYER} — legacy Zoom-Layer grid system; produces distinct patches.
 *       Good for biomes that should feel like isolated "islands".</li>
 *   <li>{@link #SMOOTH_NOISE} — low-frequency NormalNoise; produces large, continent-scale
 *       regions with organic boundaries. Good for biomes that should feel native to the world.</li>
 * </ul>
 */
public enum PlacementMode {
    ZOOM_LAYER,
    SMOOTH_NOISE
}
