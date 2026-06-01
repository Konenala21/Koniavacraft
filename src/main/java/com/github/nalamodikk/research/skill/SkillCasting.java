package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModItems;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Shared server-side cast path for the dual cost model.
 *
 * Aspects are a gate: the caster must own each one ({@link PlayerKnowledge}) but
 * nothing is consumed. Mana is the consumable: it is deducted from the casting
 * wand's {@link ModDataComponents#MANA_STORED} buffer. Only when both checks pass
 * does the skill run. Used by both the registry demos and (later) core-stored skills.
 */
public final class SkillCasting {

    /**
     * Attempts to cast {@code skill} using {@code wand} as the mana source.
     *
     * @return true if the skill was cast (mana spent + effect run), false if the
     *         aspect gate or mana check failed (a client message is shown).
     */
    public static boolean tryCast(ServerPlayer caster, ItemStack wand, SkillEffect skill) {
        if (!(caster.level() instanceof ServerLevel level)) return false;

        // 0) cooldown: rate-limit so skills can't be spammed every tick
        if (caster.getCooldowns().isOnCooldown(wand.getItem())) return false;

        SkillCost cost = skill.cost();
        ResearchSavedData data = ResearchSavedData.get(level);
        PlayerKnowledge knowledge = data.getOrCreate(caster.getUUID());

        // 1) aspect gate: must own each, never consumed
        for (Map.Entry<ResourceLocation, Integer> entry : cost.aspectGate().entrySet()) {
            if (knowledge.getAspectCount(entry.getKey()) < entry.getValue()) {
                caster.displayClientMessage(
                        Component.translatable("message.koniava.skill.not_enough_aspects"), true);
                return false;
            }
        }

        // 2) mana: apply the wand's Efficiency upgrades, then afford + consume.
        int mana = effectiveMana(wand, cost.mana());
        int stored = wand.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (stored < mana) {
            caster.displayClientMessage(
                    Component.translatable("message.koniava.skill.not_enough_mana"), true);
            return false;
        }
        wand.set(ModDataComponents.MANA_STORED, stored - mana);

        // 3) run, then cool down BOTH wand types so swapping hands can't bypass it
        skill.execute(new SkillContext(level, caster));
        int cd = cooldownTicks(cost.mana());
        caster.getCooldowns().addCooldown(ModItems.WAND_ROD.get(), cd);
        caster.getCooldowns().addCooldown(ModItems.WAND_ROD_ADVANCED.get(), cd);
        return true;
    }

    /** Cooldown in ticks from a skill's base mana cost (heavier skills = longer). */
    public static int cooldownTicks(int baseMana) {
        return Math.max(10, Math.min(100, baseMana / 15)); // ~0.5s .. 5s
    }

    /** Mana cost after the wand's Efficiency upgrades (each point is 1% off, floored at 40%). */
    public static int effectiveMana(ItemStack wand, int baseMana) {
        WandCoreData data = wand.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
        int efficiency = data.sumUpgradeBonus(WandUpgradeBehavior.EFFICIENCY);
        float mult = Math.max(0.4F, 1.0F - efficiency / 100.0F);
        return Math.max(1, Math.round(baseMana * mult));
    }

    private SkillCasting() {}
}
