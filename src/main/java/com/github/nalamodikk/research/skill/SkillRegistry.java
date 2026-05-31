package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.skill.effect.FireballEffect;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "socket panel": every castable skill lives here.
 *
 * To add a skill: write a {@link SkillEffect} class, then add one
 * {@code register(...)} line in the static block. The trigger, networking and
 * aspect consumption are all generic and do not change.
 */
public final class SkillRegistry {

    public static final ResourceLocation FIREBALL = id("fireball");

    private static final Map<ResourceLocation, SkillEffect> SKILLS = new LinkedHashMap<>();

    static {
        register(FIREBALL, new FireballEffect());
    }

    public static void register(ResourceLocation skillId, SkillEffect effect) {
        SKILLS.put(skillId, effect);
    }

    public static SkillEffect get(ResourceLocation skillId) {
        return SKILLS.get(skillId);
    }

    public static Map<ResourceLocation, SkillEffect> all() {
        return Collections.unmodifiableMap(SKILLS);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
    }

    private SkillRegistry() {}
}
