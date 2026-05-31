package com.github.nalamodikk.research.skill;

/**
 * The "socket" of the aspect skill system.
 *
 * Every castable skill is one implementation of this interface. To add a new
 * skill you write one class implementing {@code cost()} + {@code execute()} and
 * register it in {@link SkillRegistry}; nothing else (trigger, networking,
 * cost handling) needs to change.
 *
 * <p>{@code cost()} returns a {@link SkillCost}: an aspect gate (must be owned,
 * not consumed) plus a mana amount (consumed from the casting wand).
 */
public interface SkillEffect {

    /** The aspect gate + mana price of this skill. */
    SkillCost cost();

    /** Run the skill server-side. Cost has already been checked and the mana spent. */
    void execute(SkillContext ctx);
}
