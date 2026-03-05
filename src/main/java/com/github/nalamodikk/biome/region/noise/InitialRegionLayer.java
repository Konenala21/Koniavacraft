package com.github.nalamodikk.biome.region.noise;

import com.github.nalamodikk.biome.region.SimpleBiomeRegion;

import java.util.List;

/**
 * Leaf layer that outputs a region index based on weighted random selection.
 *
 * <p>Index {@code 0} is reserved for vanilla (weight = {@code vanillaWeight}).
 * Each custom {@link SimpleBiomeRegion} occupies index {@code region.uniquenessIndex()}
 * with weight {@code region.weight()}.
 *
 * <p>Coverage of region {@code i}: {@code weight_i / totalWeight}.
 */
public class InitialRegionLayer implements AreaTransformer0 {

    private final int[] cumulativeWeights;
    private final int[] indices;
    private final int totalWeight;

    /**
     * @param regions      custom regions, each with a pre-assigned {@code uniquenessIndex}
     * @param vanillaWeight weight for the vanilla (index 0) slot
     */
    public InitialRegionLayer(List<SimpleBiomeRegion> regions, int vanillaWeight) {
        int n = regions.size() + 1; // +1 for vanilla
        cumulativeWeights = new int[n];
        indices = new int[n];

        int total = vanillaWeight;
        indices[0] = 0;
        cumulativeWeights[0] = total;

        for (int i = 0; i < regions.size(); i++) {
            SimpleBiomeRegion region = regions.get(i);
            total += region.weight();
            cumulativeWeights[i + 1] = total;
            indices[i + 1] = region.uniquenessIndex();
        }
        this.totalWeight = total;
    }

    @Override
    public int apply(AreaContext ctx, int x, int z) {
        // ctx.initRandom(x, z) is already called by Area.get() before this method
        int r = ctx.nextRandom(totalWeight);
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (r < cumulativeWeights[i]) return indices[i];
        }
        return 0; // fallback — should never be reached
    }
}
