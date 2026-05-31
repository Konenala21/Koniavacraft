package com.github.nalamodikk.research.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * The "socket" of the aspect skill system.
 *
 * Every castable skill is one implementation of this interface. To add a new
 * skill you write one class implementing {@code cost()} + {@code execute()} and
 * register it in {@link SkillRegistry}; nothing else (trigger, networking,
 * aspect consumption) needs to change.
 *
 * <p>{@code cost()} maps each required aspect id to how many are consumed.
 * Only the aspects in this map are touched; amounts can differ per aspect
 * (e.g. fire 1, water 3).
 */
public interface SkillEffect {

    /** Aspect id -> amount consumed. Only listed aspects are spent. */
    Map<ResourceLocation, Integer> cost();

    /** Run the skill server-side. Cost has already been checked and consumed. */
    void execute(SkillContext ctx);
}
