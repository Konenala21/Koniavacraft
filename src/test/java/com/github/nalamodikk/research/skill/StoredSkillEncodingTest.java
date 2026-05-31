package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.research.aspect.ModAspects;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for the stored-skill recipe and its role validation. The
 * ItemStack read/write paths (setSlot/getSelected) need a Minecraft context and
 * are exercised in game tests, not here.
 */
class StoredSkillEncodingTest {

    private static StoredSkill fireball() {
        return new StoredSkill("Fireball",
                ModAspects.MOMENTUM.getId(),
                List.of(ModAspects.PHLOGISTON.getId()),
                List.of());
    }

    @Test
    void recipeCompilesToDualCost() {
        SkillCost cost = fireball().compile().cost();
        // gate: carrier + one effect, each owned 1; no fuel in the new model
        assertEquals(1, cost.aspectGate().get(ModAspects.MOMENTUM.getId()));
        assertEquals(1, cost.aspectGate().get(ModAspects.PHLOGISTON.getId()));
        assertEquals(2, cost.aspectGate().size());
        // mana = base 100 + carrier 80 + 1 effect 60 + 0 modifier = 240
        assertEquals(240, cost.mana());
    }

    @Test
    void validRecipeRespectsRoles() {
        StoredSkill ok = new StoredSkill("Split Fire",
                ModAspects.MOMENTUM.getId(),
                List.of(ModAspects.PHLOGISTON.getId()),
                List.of(ModAspects.REFRACTION.getId()));
        assertTrue(SkillEncoding.isValidRecipe(ok), "carrier+effect+modifier all legal");
    }

    @Test
    void carrierInEffectSlotIsRejected() {
        // MOMENTUM is a carrier, not an effect: using it as the effect is invalid
        StoredSkill bad = new StoredSkill("Bad",
                ModAspects.MOMENTUM.getId(),
                List.of(ModAspects.MOMENTUM.getId()),
                List.of());
        assertFalse(SkillEncoding.isValidRecipe(bad), "carrier cannot be an effect");
    }

    @Test
    void effectInCarrierSlotIsRejected() {
        StoredSkill bad = new StoredSkill("Bad",
                ModAspects.PHLOGISTON.getId(),  // effect aspect as carrier
                List.of(ModAspects.PHLOGISTON.getId()),
                List.of());
        assertFalse(SkillEncoding.isValidRecipe(bad), "effect cannot carry");
    }

    @Test
    void emptyEffectsIsRejected() {
        StoredSkill bad = new StoredSkill("Empty",
                ModAspects.MOMENTUM.getId(), List.of(), List.of());
        assertFalse(SkillEncoding.isValidRecipe(bad), "a skill needs at least one effect");
    }
}
