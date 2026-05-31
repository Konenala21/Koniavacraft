package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
