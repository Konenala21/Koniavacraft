package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the semantic table loads from resources and classifies items sanely.
 * Skips gracefully if the table is absent (it is gitignored; run the
 * aspect-embedding skill's regenerate.py to produce it).
 */
class SemanticAspectMatcherTest {

    private static List<Aspect> match(String id) {
        return SemanticAspectMatcher.match(ResourceLocation.parse(id));
    }

    @Test
    void classifiesKnownItems() {
        List<Aspect> sword = match("minecraft:diamond_sword");
        Assumptions.assumeFalse(sword.isEmpty(), "semantic table not present, skipping");

        assertTrue(match("minecraft:diamond_sword").contains(ModAspects.BLADE), "sword -> blade");
        assertTrue(match("minecraft:ice").contains(ModAspects.FROST), "ice -> frost");
        assertTrue(match("koniava:mana_bloom").contains(ModAspects.MANA), "mana_bloom -> mana");
        assertTrue(match("mekanism:osmium_ingot").contains(ModAspects.METAL), "osmium_ingot -> metal (OOV)");
    }

    @Test
    void unknownTokensReturnEmpty() {
        Assumptions.assumeFalse(match("minecraft:ice").isEmpty(), "semantic table not present, skipping");
        // gibberish with no in-vocab tokens should yield no semantic guess
        assertTrue(match("koniava:zzqxv").isEmpty(), "gibberish -> empty");
    }

    @Test
    void seededPickStaysWithinTheSemanticPool() {
        ResourceLocation id = ResourceLocation.parse("minecraft:diamond_sword");
        List<Aspect> pool = SemanticAspectMatcher.candidates(id, 5);
        Assumptions.assumeFalse(pool.isEmpty(), "semantic table not present, skipping");

        assertTrue(pool.contains(ModAspects.BLADE), "blade is a plausible candidate for a sword");

        // different seeds may reorder, but the pick never invents anything outside the pool
        List<Aspect> a = AspectExpression.seededPick(id, pool, 111L, 2);
        List<Aspect> b = AspectExpression.seededPick(id, pool, 222L, 2);
        assertTrue(a.size() <= 2 && b.size() <= 2, "picks at most 2");
        assertTrue(pool.containsAll(a) && pool.containsAll(b), "picks stay within the semantic pool");
        // deterministic for a given seed (reproducible per world)
        assertEquals(a, AspectExpression.seededPick(id, pool, 111L, 2), "same seed -> same pick");
    }
}
