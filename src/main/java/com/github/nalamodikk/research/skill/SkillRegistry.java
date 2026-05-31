package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "socket panel": every castable skill lives here.
 *
 * Demo skills are built by {@link SkillCompiler} from aspect combinations, proving
 * the same compiler handles any mix. Later the Skill Core Encoding Bench writes
 * player-made combinations into spell cores; this static list is just the seed set.
 */
public final class SkillRegistry {

    public static final ResourceLocation FIREBALL  = id("fireball");
    public static final ResourceLocation FROSTBOLT = id("frostbolt");
    public static final ResourceLocation FIRE_SPLIT = id("fire_split");

    private static final Map<ResourceLocation, SkillEffect> SKILLS = new LinkedHashMap<>();

    static {
        // 動力 + 燃素 + 火料 -> 火球
        register(FIREBALL, SkillCompiler.compile(
                ModAspects.MOMENTUM, List.of(ModAspects.PHLOGISTON), List.of(),
                Map.of(ModAspects.FIRE, 1)));
        // 動力 + 冰寒 + 水料 -> 冰彈
        register(FROSTBOLT, SkillCompiler.compile(
                ModAspects.MOMENTUM, List.of(ModAspects.FROST), List.of(),
                Map.of(ModAspects.WATER, 1)));
        // 火球 + 折射 -> 三連散射火
        register(FIRE_SPLIT, SkillCompiler.compile(
                ModAspects.MOMENTUM, List.of(ModAspects.PHLOGISTON), List.of(ModAspects.REFRACTION),
                Map.of(ModAspects.FIRE, 1)));
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
