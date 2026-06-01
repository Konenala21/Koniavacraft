package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.common.network.packet.client.skill.SkillCooldownPacket;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Shared server-side cast path.
 *
 * Aspects are spent earlier, when the skill is encoded onto the core (see
 * {@code SkillEncoding.consumeAspects}). Casting a baked skill therefore costs
 * only mana: it is deducted from the casting wand's
 * {@link ModDataComponents#MANA_STORED} buffer. Used by core-stored skills.
 */
public final class SkillCasting {

    /**
     * Attempts to cast {@code skill} using {@code wand} as the mana source.
     *
     * @return true if the skill was cast (mana spent + effect run), false if the
     *         cooldown or mana check failed (a client message is shown).
     */
    public static boolean tryCast(ServerPlayer caster, ItemStack wand, SkillEffect skill, int slot) {
        if (!(caster.level() instanceof ServerLevel level)) return false;

        // 0) cooldown: this skill slot (and a short global gap) must be ready, so the
        // player rotates skills instead of spamming one. See SkillCooldowns.
        if (!SkillCooldowns.ready(caster, slot, level.getGameTime())) return false;

        SkillCost cost = skill.cost();

        // Aspects were already spent when the skill was encoded onto the core, so
        // casting a baked skill no longer needs them: it only costs mana.
        // 1) mana: apply the wand's Efficiency upgrades, then afford + consume.
        int mana = effectiveMana(wand, cost.mana());
        int stored = wand.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (stored < mana) {
            caster.displayClientMessage(
                    Component.translatable("message.koniava.skill.not_enough_mana"), true);
            return false;
        }
        wand.set(ModDataComponents.MANA_STORED, stored - mana);

        // 3) run, then start this slot's cooldown + a short global gap, and tell the
        // client (for the HUD). Per-slot CD means other slots stay castable.
        skill.execute(new SkillContext(level, caster));
        int cd = effectiveCooldown(wand, cost.cooldown());
        SkillCooldowns.start(caster, slot, level.getGameTime(), cd);
        SkillCooldownPacket.send(caster, slot, cd, SkillCooldowns.GLOBAL_COOLDOWN);
        return true;
    }

    /** The skill's base cooldown after the wand's Cooldown upgrades (one tick off each, floored at 5). */
    public static int effectiveCooldown(ItemStack wand, int baseCooldown) {
        WandCoreData data = wand.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
        int reduce = data.sumUpgradeBonus(WandUpgradeBehavior.COOLDOWN);
        return Math.max(5, baseCooldown - reduce);
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
