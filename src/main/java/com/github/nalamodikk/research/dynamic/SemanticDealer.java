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
 * The pass scans the whole item registry, so it runs ONCE per world on a background
 * daemon thread (it is pure + reads frozen registries, so it is safe off-thread).
 * Until it finishes, {@link #resolve} still works via the seeded pick alone
 * (accurate + per-world, just not yet completeness-guaranteed); guaranteed carriers
 * snap in once the build commits. This keeps the first scan from hitching the server.
 *
 * Abstract aspects with no plausible item carrier are not covered here by design:
 * they come from block / entity / player scans instead.
 */
public final class SemanticDealer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticDealer.class);
    private static final int POOL_N = 5;              // semantic candidates considered per item
    private static final int CARRIERS_PER_ASPECT = 4; // guaranteed source items per aspect

    private static volatile Map<ResourceLocation, List<Aspect>> guaranteed = Map.of();
    private static volatile long readySeed = Long.MIN_VALUE;   // seed the committed map is for
    private static long requestedSeed = Long.MIN_VALUE;
    private static boolean building = false;

    /** Guaranteed aspects this item must yield in the given world (empty until the pass finishes). */
    public static List<Aspect> guaranteedFor(ResourceLocation id, long genomeSeed) {
        if (readySeed != genomeSeed) startBuild(genomeSeed);
        return readySeed == genomeSeed ? guaranteed.getOrDefault(id, List.of()) : List.of();
    }

    /**
     * Full semantic resolution for an item: its guaranteed aspects first (completeness,
     * once the background pass is ready), then the seed-dealt picks from its semantic
     * pool, capped at {@code max}. Works immediately (seeded pick) even mid-build.
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

    // ── background build ──────────────────────────────────────────────────────

    private static synchronized void startBuild(long genomeSeed) {
        if (readySeed == genomeSeed) return;                    // already built for this seed
        if (building && requestedSeed == genomeSeed) return;    // a build for this seed is running
        requestedSeed = genomeSeed;
        building = true;
        Thread t = new Thread(() -> commit(genomeSeed, build(genomeSeed)), "koniava-aspect-dealer");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }

    private static synchronized void commit(long genomeSeed, Map<ResourceLocation, List<Aspect>> map) {
        building = false;
        if (requestedSeed == genomeSeed) {   // still the world we want
            guaranteed = map;
            readySeed = genomeSeed;
        }
    }

    /** Pure computation (no shared state): the item -> guaranteed aspects map for a seed. */
    private static Map<ResourceLocation, List<Aspect>> build(long genomeSeed) {
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
        LOGGER.info("Aspect dealing built (async): {} aspects covered, {} carrier items, {} ms",
                byAspect.size(), map.size(), (System.nanoTime() - start) / 1_000_000);
        return map;
    }

    private SemanticDealer() {}
}
