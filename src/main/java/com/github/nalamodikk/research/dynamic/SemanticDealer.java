package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global completeness pass for the A+B scanning: guarantees every aspect that any
 * item could plausibly carry actually has a few "guaranteed carrier" items in this
 * world, so no aspect (and so no skill needing it) is unobtainable by bad seed luck.
 *
 * Once per world (keyed by genome seed, deterministic so it is the same every load):
 * for each item, collect the aspects its semantic pool plausibly contains; then for
 * each aspect, seed-pick {@link #CARRIERS_PER_ASPECT} of those items and mark them as
 * guaranteed to yield it. Resolution forces a carrier item's guaranteed aspects in.
 *
 * Abstract aspects with no plausible item carrier are not covered here by design:
 * they come from block / entity / player scans instead.
 */
public final class SemanticDealer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticDealer.class);
    private static final int POOL_N = 5;             // semantic candidates considered per item
    private static final int CARRIERS_PER_ASPECT = 4; // guaranteed source items per aspect

    private static long cachedSeed;
    private static boolean built = false;
    private static Map<ResourceLocation, List<Aspect>> guaranteed = Map.of();

    /** Guaranteed aspects this item must yield in the given world (may be empty). */
    public static List<Aspect> guaranteedFor(ResourceLocation id, long genomeSeed) {
        ensure(genomeSeed);
        return guaranteed.getOrDefault(id, List.of());
    }

    /**
     * Full semantic resolution for an item: its guaranteed aspects first (completeness),
     * then the seed-dealt picks from its semantic pool, capped at {@code max}. Empty if
     * the item has no plausible aspects at all.
     */
    public static List<Aspect> resolve(ResourceLocation id, long genomeSeed, int max) {
        List<Aspect> out = new ArrayList<>(guaranteedFor(id, genomeSeed));
        if (out.size() > max) return new ArrayList<>(out.subList(0, max));
        for (Aspect a : AspectExpression.seededPick(id, SemanticAspectMatcher.candidates(id, POOL_N), genomeSeed, max)) {
            if (out.size() >= max) break;
            if (!out.contains(a)) out.add(a);
        }
        return out;
    }

    private static synchronized void ensure(long genomeSeed) {
        if (built && cachedSeed == genomeSeed) return;

        long start = System.nanoTime();
        // aspect -> items whose semantic pool plausibly contains it
        Map<Aspect, List<ResourceLocation>> byAspect = new HashMap<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            for (Aspect a : SemanticAspectMatcher.candidates(id, POOL_N)) {
                byAspect.computeIfAbsent(a, k -> new ArrayList<>()).add(id);
            }
        }

        // for each aspect, seed-pick a few carriers and mark item -> guaranteed aspect
        Map<ResourceLocation, List<Aspect>> map = new HashMap<>();
        for (Map.Entry<Aspect, List<ResourceLocation>> e : byAspect.entrySet()) {
            Aspect aspect = e.getKey();
            List<ResourceLocation> carriers = e.getValue();
            carriers.sort(Comparator.comparingLong(id -> -AspectExpression.score(id, aspect, genomeSeed)));
            for (int i = 0; i < Math.min(CARRIERS_PER_ASPECT, carriers.size()); i++) {
                map.computeIfAbsent(carriers.get(i), k -> new ArrayList<>()).add(aspect);
            }
        }

        guaranteed = map;
        cachedSeed = genomeSeed;
        built = true;
        LOGGER.info("Aspect dealing built: {} aspects covered, {} guaranteed-carrier items, {} ms",
                byAspect.size(), map.size(), (System.nanoTime() - start) / 1_000_000);
    }

    private SemanticDealer() {}
}
