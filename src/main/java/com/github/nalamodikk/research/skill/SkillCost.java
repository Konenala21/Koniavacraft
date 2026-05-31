package com.github.nalamodikk.research.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * The price of a skill, split by the dual cost model the project settled on:
 *
 * <ul>
 *   <li>{@code aspectGate}: aspects are a permanent GATE. You must already own
 *       (have researched/discovered) each listed aspect, but casting does NOT
 *       consume them. Map value is the minimum amount you must own (usually 1).</li>
 *   <li>{@code mana}: the per-cast CONSUMABLE, deducted from the casting wand's
 *       internal mana buffer.</li>
 * </ul>
 */
public record SkillCost(Map<ResourceLocation, Integer> aspectGate, int mana) {

    public static SkillCost of(Map<ResourceLocation, Integer> aspectGate, int mana) {
        return new SkillCost(Map.copyOf(aspectGate), mana);
    }
}
