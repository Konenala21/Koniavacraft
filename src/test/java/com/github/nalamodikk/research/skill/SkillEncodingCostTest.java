package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Encode-time aspect cost (the consume model). Pure logic. Covers the case behind
 * the "not_enough" message: a multi-role aspect (e.g. DUI = carrier Nova + effect
 * launch) used in two slots costs double, so owning one is not enough.
 */
class SkillEncodingCostTest {

    /** DUI as both carrier and an effect — valid (it has both roles) but costs 2x DUI. */
    private static StoredSkill dualDui() {
        return new StoredSkill("DualDui",
                ModAspects.DUI.getId(),
                List.of(ModAspects.DUI.getId()),
                List.of());
    }

    @Test
    void sameAspectInTwoSlotsCostsDouble() {
        var cost = SkillEncoding.aspectCost(dualDui());
        assertEquals(2, cost.get(ModAspects.DUI.getId()), "DUI used as carrier + effect costs 2");
    }

    @Test
    void canAffordRequiresTheDoubledAmount() {
        StoredSkill skill = dualDui();
        PlayerKnowledge k = new PlayerKnowledge();

        k.setAspectAmount(ModAspects.DUI.getId(), 1);
        assertFalse(SkillEncoding.canAfford(k, skill), "owning 1 DUI is not enough for a 2x recipe");

        k.setAspectAmount(ModAspects.DUI.getId(), 2);
        assertTrue(SkillEncoding.canAfford(k, skill), "owning 2 DUI is enough");
    }

    @Test
    void distinctAspectsEachCostOne() {
        StoredSkill skill = new StoredSkill("Fireball",
                ModAspects.MOMENTUM.getId(),
                List.of(ModAspects.PHLOGISTON.getId()),
                List.of());
        var cost = SkillEncoding.aspectCost(skill);
        assertEquals(1, cost.get(ModAspects.MOMENTUM.getId()));
        assertEquals(1, cost.get(ModAspects.PHLOGISTON.getId()));
    }
}
