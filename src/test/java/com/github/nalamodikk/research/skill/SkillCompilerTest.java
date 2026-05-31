package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.research.aspect.ModAspects;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the dual cost model: aspects become a gate (own = 1 each, never the
 * consumed amount) and mana is derived from the combination's shape. Pure logic,
 * no Minecraft context (no execute() call, so no entity spawn).
 */
class SkillCompilerTest {

    @Test
    void aspectGateListsEveryUsedAspectOnce() {
        SkillCost cost = SkillCompiler.compile(
                ModAspects.MOMENTUM,
                List.of(ModAspects.PHLOGISTON),
                List.of(),
                Map.of(ModAspects.FIRE, 3)   // fuel amount 3, but gate must still be 1
        ).cost();

        assertEquals(1, cost.aspectGate().get(ModAspects.MOMENTUM.getId()), "carrier gate = 1");
        assertEquals(1, cost.aspectGate().get(ModAspects.PHLOGISTON.getId()), "effect gate = 1");
        assertEquals(1, cost.aspectGate().get(ModAspects.FIRE.getId()), "fuel gate = 1 (not the amount)");
        assertEquals(3, cost.aspectGate().size(), "exactly the three distinct aspects");
    }

    @Test
    void manaScalesWithCombinationShape() {
        // base 100 + carrier 80 + 1 effect 60 + 0 modifier + fuel 1 * 20 = 260
        int fireball = SkillCompiler.compile(
                ModAspects.MOMENTUM, List.of(ModAspects.PHLOGISTON), List.of(),
                Map.of(ModAspects.FIRE, 1)).cost().mana();
        assertEquals(260, fireball, "fireball mana");

        // adding a REFRACTION modifier adds 50
        int split = SkillCompiler.compile(
                ModAspects.MOMENTUM, List.of(ModAspects.PHLOGISTON), List.of(ModAspects.REFRACTION),
                Map.of(ModAspects.FIRE, 1)).cost().mana();
        assertEquals(310, split, "fire_split mana = fireball + one modifier");
    }

    @Test
    void aspectGateIsNeverNegativeOrConsumedShape() {
        // the gate map value is the ownership threshold, always >= 1
        SkillCost cost = SkillCompiler.compile(
                ModAspects.MOMENTUM, List.of(ModAspects.FROST), List.of(),
                Map.of(ModAspects.WATER, 5)).cost();
        cost.aspectGate().values().forEach(v -> assertEquals(1, v.intValue()));
        assertNull(cost.aspectGate().get(ModAspects.PHLOGISTON.getId()), "unused aspect absent from gate");
    }
}
