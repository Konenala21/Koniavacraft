package com.github.nalamodikk.research.skill;

/**
 * The role an aspect plays inside a skill combination (the "C dictionary").
 *
 * A skill = 1 CARRIER + 1..N EFFECT + 0..N MODIFIER, fuelled by primary aspects.
 * Roles follow from each aspect's authored meaning (e.g. momentum/propulsion is a
 * carrier, binding/restraint is a modifier). An aspect may hold several roles;
 * which one applies depends on the slot it is placed in.
 */
public enum SkillRole {
    /** Primary aspects: raw element / intensity fuel fed into a skill. */
    FUEL,
    /** How the skill is delivered: projectile, beam, self, area. Exactly 1 per skill. */
    CARRIER,
    /** The payload on hit: damage, burn, slow, heal. 1..N per skill. */
    EFFECT,
    /** Alters carrier or effect: pierce, split, root, amplify. 0..N per skill. */
    MODIFIER,
    /** When it fires: on-hit, on-timer, chain. Reserved, late-tier. */
    TRIGGER
}
