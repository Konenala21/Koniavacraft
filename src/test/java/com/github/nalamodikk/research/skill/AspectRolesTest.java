package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the skill-role assignments. Pure logic. Mainly guards the 14 carriers
 * (the original 8 + the 6 trigram/anima aspects that gained the CARRIER role this
 * round) so a future role edit can't silently drop a carrier from the encoder.
 */
class AspectRolesTest {

    private static void assertCarrier(Aspect a) {
        assertTrue(AspectRoles.hasRole(a, SkillRole.CARRIER), a.getId() + " should be a CARRIER");
    }

    @Test
    void originalEightCarriers() {
        assertCarrier(ModAspects.MOMENTUM);
        assertCarrier(ModAspects.MANA);
        assertCarrier(ModAspects.XUN);
        assertCarrier(ModAspects.FLIGHT);
        assertCarrier(ModAspects.PIPELINE);
        assertCarrier(ModAspects.GRAVITY);
        assertCarrier(ModAspects.BLADE);
        assertCarrier(ModAspects.MACHINE);
    }

    @Test
    void newMultiRoleCarriers() {
        assertCarrier(ModAspects.DUI);   // Nova
        assertCarrier(ModAspects.KUN);   // Shockwave
        assertCarrier(ModAspects.GEN);   // Earthwave
        assertCarrier(ModAspects.KAN);   // Lob
        assertCarrier(ModAspects.LI);    // Meteor
        assertCarrier(ModAspects.ANIMA); // Orbital
    }

    @Test
    void multiRoleAspectsKeepTheirOtherRole() {
        // adding CARRIER must not drop the original role
        assertTrue(AspectRoles.hasRole(ModAspects.DUI, SkillRole.EFFECT), "DUI still an effect");
        assertTrue(AspectRoles.hasRole(ModAspects.KUN, SkillRole.MODIFIER), "KUN still a modifier");
        assertTrue(AspectRoles.hasRole(ModAspects.ANIMA, SkillRole.MODIFIER), "ANIMA still a modifier (homing)");
    }

    @Test
    void pureEffectIsNotACarrier() {
        assertFalse(AspectRoles.hasRole(ModAspects.VENOM, SkillRole.CARRIER));
        assertFalse(AspectRoles.hasRole(ModAspects.PHLOGISTON, SkillRole.CARRIER));
    }
}
