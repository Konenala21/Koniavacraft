package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 表現世界本源基因：主本源固定，副本源只從合理候選池中挑選。
 */
public final class AspectExpression {
    private static final List<Aspect> FALLBACK_POOL = List.of(
            ModAspects.WATER,
            ModAspects.FIRE,
            ModAspects.WOOD,
            ModAspects.METAL,
            ModAspects.EARTH,
            ModAspects.MANA,
            ModAspects.CRYSTAL,
            ModAspects.RESONANCE,
            ModAspects.ENERGY,
            ModAspects.VITALITY,
            ModAspects.GRAVITY,
            ModAspects.GROWTH
    );

    public static List<Aspect> express(ResourceLocation id, List<Aspect> base, List<Aspect> candidates, long genomeSeed, int maxAspects) {
        List<Aspect> result = new ArrayList<>();
        addAllUnique(result, base);
        if (result.size() >= maxAspects || candidates.isEmpty()) {
            return limit(result, maxAspects);
        }

        List<Aspect> sortedCandidates = candidates.stream()
                .filter(aspect -> !result.contains(aspect))
                .sorted(Comparator.comparingLong(aspect -> -score(id, aspect, genomeSeed)))
                .toList();

        for (Aspect candidate : sortedCandidates) {
            if (result.size() >= maxAspects) {
                break;
            }
            if (shouldExpress(id, candidate, genomeSeed, result.size())) {
                result.add(candidate);
            }
        }
        return result;
    }

    public static List<Aspect> fallback(ResourceLocation id, long genomeSeed, int maxAspects) {
        List<Aspect> ordered = FALLBACK_POOL.stream()
                .sorted(Comparator.comparingLong(aspect -> -score(id, aspect, genomeSeed)))
                .toList();
        int count = score(id, ordered.get(0), genomeSeed) % 4 == 0 ? 2 : 1;
        return ordered.stream().limit(Math.min(count, maxAspects)).toList();
    }

    /**
     * Deal {@code maxAspects} from a semantic candidate pool using the world seed:
     * the pool (from the word-vector matcher) is all semantically plausible, and the
     * seed re-ranks it so every world keeps sensible aspects but varies which one an
     * item carries. Always returns up to maxAspects when the pool is non-empty.
     */
    public static List<Aspect> seededPick(ResourceLocation id, List<Aspect> pool, long genomeSeed, int maxAspects) {
        if (pool.isEmpty() || maxAspects <= 0) return List.of();
        return pool.stream()
                .sorted(Comparator.comparingLong(aspect -> -score(id, aspect, genomeSeed)))
                .limit(maxAspects)
                .toList();
    }

    public static void addUnique(List<Aspect> aspects, Aspect aspect) {
        if (!aspects.contains(aspect)) {
            aspects.add(aspect);
        }
    }

    public static void addAllUnique(List<Aspect> aspects, List<Aspect> additions) {
        for (Aspect aspect : additions) {
            addUnique(aspects, aspect);
        }
    }

    private static boolean shouldExpress(ResourceLocation id, Aspect aspect, long genomeSeed, int resultSize) {
        if (resultSize == 0) {
            return true;
        }
        long score = score(id, aspect, genomeSeed);
        return Math.floorMod(score, 100) < 45;
    }

    private static List<Aspect> limit(List<Aspect> aspects, int maxAspects) {
        if (aspects.size() <= maxAspects) {
            return aspects;
        }
        return aspects.stream().limit(maxAspects).toList();
    }

    private static long score(ResourceLocation id, Aspect aspect, long genomeSeed) {
        long value = genomeSeed;
        value ^= id.toString().hashCode() * 0x9E3779B97F4A7C15L;
        value ^= aspect.getId().toString().hashCode() * 0xBF58476D1CE4E5B9L;
        return mix(value);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value & Long.MAX_VALUE;
    }

    private AspectExpression() {}
}
