package com.github.nalamodikk.biome.region.noise;

import com.github.nalamodikk.biome.region.SimpleBiomeRegion;

import java.util.Comparator;
import java.util.List;
import java.util.function.LongFunction;

/**
 * Assembles the full Zoom-Layer chain that maps world coordinates to region indices.
 *
 * <p>Chain structure (seedModifiers in parentheses):
 * <pre>
 *   InitialRegionLayer (2000)
 *     → ZoomLayer.FUZZY  (2001)
 *     → ZoomLayer.NORMAL (1001)          [fixed 1]
 *     → ZoomLayer.NORMAL (1002..1+N)     [zoomCount-1 additional]
 * </pre>
 *
 * <p>Index semantics:
 * <ul>
 *   <li>{@code 0} → vanilla (no custom biome injection)</li>
 *   <li>{@code 1..N} → custom region with {@code uniquenessIndex == index}</li>
 * </ul>
 *
 * <p>Coverage of a custom region: {@code region.weight() / (vanillaWeight + Σ region.weight())}.
 */
public final class RegionNoiseUtil {

    private RegionNoiseUtil() {}

    /**
     * Build a fully zoomed {@link Area} from the registered regions.
     *
     * @param regions       custom regions, sorted by {@code uniquenessIndex}
     * @param vanillaWeight weight for the vanilla index-0 slot
     * @param worldSeed     world seed used to initialise per-position randomness
     * @param zoomCount     total number of NORMAL zoom passes (1 fixed + zoomCount-1 extra);
     *                      clamped to [1, 8]
     * @return a thread-safe Area ready to be queried with {@code area.get(blockX, blockZ)}
     */
    public static Area build(List<SimpleBiomeRegion> regions, int vanillaWeight, long worldSeed, int zoomCount) {
        // Sort by uniquenessIndex for deterministic index→slot mapping
        List<SimpleBiomeRegion> sorted = regions.stream()
                .sorted(Comparator.comparingInt(SimpleBiomeRegion::uniquenessIndex))
                .toList();

        LongFunction<AreaContext> ctxFactory = sm -> new AreaContext(1024, worldSeed, sm);

        // 1. Leaf layer: weight-based random region selection
        AreaFactory factory = new InitialRegionLayer(sorted, vanillaWeight).run(2000L, ctxFactory);

        // 2. One FUZZY zoom (produces organic blob shapes at coarse scale)
        factory = ZoomLayer.FUZZY.run(2001L, ctxFactory, factory);

        // 3. Fixed first NORMAL zoom
        factory = ZoomLayer.NORMAL.run(1001L, ctxFactory, factory);

        // 4. Additional NORMAL zooms controlled by zoomCount
        int extra = Math.max(0, Math.min(7, zoomCount - 1));
        for (int i = 0; i < extra; i++) {
            factory = ZoomLayer.NORMAL.run(1002L + i, ctxFactory, factory);
        }

        return factory.make();
    }
}
