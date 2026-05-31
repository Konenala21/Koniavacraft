package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side rules for encoding a {@link StoredSkill} onto a spell core and for
 * reading back the core's selected skill at cast time.
 *
 * Role legality comes from the {@link AspectRoles} C dictionary: a recipe is valid
 * only if the carrier can carry, every effect can be an effect, and every modifier
 * can modify. Ownership (the aspect gate) is checked separately against the
 * player's {@link PlayerKnowledge}: you may only encode aspects you own.
 */
public final class SkillEncoding {

    /** How many skills a single spell core can hold. */
    public static final int MAX_SLOTS = 6;

    // ── validation ────────────────────────────────────────────────────────────

    /** True if the recipe respects the role dictionary (carrier/effect/modifier). */
    public static boolean isValidRecipe(StoredSkill skill) {
        Aspect carrier = ModAspects.get(skill.carrier());
        if (carrier == null || !AspectRoles.hasRole(carrier, SkillRole.CARRIER)) return false;
        if (skill.effects().isEmpty()) return false; // a skill needs at least one effect
        for (ResourceLocation id : skill.effects()) {
            Aspect a = ModAspects.get(id);
            if (a == null || !AspectRoles.hasRole(a, SkillRole.EFFECT)) return false;
        }
        for (ResourceLocation id : skill.modifiers()) {
            Aspect a = ModAspects.get(id);
            if (a == null || !AspectRoles.hasRole(a, SkillRole.MODIFIER)) return false;
        }
        return true;
    }

    /** True if the player owns (has discovered) every aspect the recipe uses. */
    public static boolean playerOwns(PlayerKnowledge knowledge, StoredSkill skill) {
        if (!owns(knowledge, skill.carrier())) return false;
        for (ResourceLocation id : skill.effects())   if (!owns(knowledge, id)) return false;
        for (ResourceLocation id : skill.modifiers()) if (!owns(knowledge, id)) return false;
        return true;
    }

    private static boolean owns(PlayerKnowledge knowledge, ResourceLocation aspectId) {
        return knowledge.getAspectCount(aspectId) > 0;
    }

    // ── core read / write ───────────────────────────────────────────────────

    public static List<StoredSkill> getSkills(ItemStack core) {
        return core.getOrDefault(ModDataComponents.STORED_SKILLS, List.of());
    }

    /**
     * Writes {@code skill} into {@code core} at {@code slot}: replaces an existing
     * entry, or appends if the slot is the next free one (within {@link #MAX_SLOTS}).
     * No-op if the slot is out of range.
     */
    public static void setSlot(ItemStack core, int slot, StoredSkill skill) {
        if (slot < 0 || slot >= MAX_SLOTS) return;
        List<StoredSkill> current = new ArrayList<>(getSkills(core));
        if (slot < current.size()) {
            current.set(slot, skill);
        } else if (slot == current.size()) {
            current.add(skill);
        } else {
            return; // can't leave gaps
        }
        core.set(ModDataComponents.STORED_SKILLS, List.copyOf(current));
    }

    /** The currently selected stored skill on a core, or null if none/invalid index. */
    public static StoredSkill getSelected(ItemStack core) {
        List<StoredSkill> skills = getSkills(core);
        if (skills.isEmpty()) return null;
        int index = core.getOrDefault(ModDataComponents.SELECTED_SKILL_INDEX, 0);
        if (index < 0 || index >= skills.size()) return null;
        return skills.get(index);
    }

    // ── cast resolution ───────────────────────────────────────────────────────

    /**
     * Resolves the skill a wand should cast from its installed spell core's selected
     * stored skill. Returns null if the wand has no core, no stored skills, or an
     * invalid selection (the caller decides any fallback).
     */
    public static SkillEffect resolveCastSkill(ItemStack wand) {
        WandCoreData data = wand.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
        if (!data.hasCore()) return null;
        StoredSkill selected = getSelected(data.core());
        return selected == null ? null : selected.compile();
    }

    private SkillEncoding() {}
}
