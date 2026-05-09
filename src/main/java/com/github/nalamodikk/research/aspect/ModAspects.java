package com.github.nalamodikk.research.aspect;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all aspects in Koniavacraft.
 *
 * Primary aspects (主元素): 水 火 木 金 土 烏
 *   - 水火木金土: observable natural phenomena
 *   - 烏: abstract chaotic turbulence, the underlying layer — cannot be scanned directly
 *
 * Compound aspects (複合面向): composed of exactly two primaries or compounds.
 * Defined AFTER primaries to ensure correct static initialisation order.
 */
public class ModAspects {

    private static final Map<ResourceLocation, Aspect> REGISTRY = new LinkedHashMap<>();

    // ── Primary aspects (主元素) ──────────────────────────────────────────────

    /** 水 — Water. Flow, adaptation, downward movement. */
    public static final Aspect WATER = register("water", 0x3A9BD5, List.of());

    /** 火 — Fire. Transformation, energy release, upward movement. */
    public static final Aspect FIRE = register("fire", 0xE25822, List.of());

    /** 木 — Wood. Growth, expansion, ordered structure. */
    public static final Aspect WOOD = register("wood", 0x4A7C3F, List.of());

    /** 金 — Metal. Order, refinement, convergence. Counterpart to 烏. */
    public static final Aspect METAL = register("metal", 0xC9A84C, List.of());

    /** 土 — Earth. Stability, grounding, balance. */
    public static final Aspect EARTH = register("earth", 0x8B5E3C, List.of());

    /** 烏 — Wu. Chaotic turbulence, the primordial substrate.
     *  Cannot be obtained by scanning — must be derived through research.
     *  Narrative: eventually revealed to be raw energy itself. */
    public static final Aspect WU = register("wu", 0x2D1B4E, List.of());

    // ── Compound aspects (複合面向) ───────────────────────────────────────────

    /** 魔力 (Mana) = 烏 + 水 — Flowing primordial energy. First compound the player encounters. */
    public static final Aspect MANA = register("mana", 0x7B2FBE, List.of(WU, WATER));

    /** 晶化 (Crystal) = 金 + 土 — Structured matter, mana plains resources. */
    public static final Aspect CRYSTAL = register("crystal", 0xA8D8EA, List.of(METAL, EARTH));

    /** 燃素 (Phlogiston) = 火 + 木 — Combustible life-energy, fuel-related. */
    public static final Aspect PHLOGISTON = register("phlogiston", 0xFF6B35, List.of(FIRE, WOOD));

    /** 共鳴 (Resonance) = 烏 + 金 — Chaos captured by order. Mid-game transition aspect. */
    public static final Aspect RESONANCE = register("resonance", 0x9B59B6, List.of(WU, METAL));

    /** 能量 (Energy) = 烏 + 火 — Pure energy. Late-game revelation aspect, intentionally hard to reach. */
    public static final Aspect ENERGY = register("energy", 0xF39C12, List.of(WU, FIRE));

    // ─────────────────────────────────────────────────────────────────────────

    private static Aspect register(String path, int color, List<Aspect> components) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        Aspect aspect = new Aspect(id, color, components);
        REGISTRY.put(id, aspect);
        return aspect;
    }

    public static Aspect get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static Collection<Aspect> all() {
        return REGISTRY.values();
    }

    public static boolean contains(ResourceLocation id) {
        return REGISTRY.containsKey(id);
    }

    private ModAspects() {}
}
