package com.github.nalamodikk.research.aspect;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all 24 aspects in Koniavacraft.
 *
 * Primary (6): 水 火 木 金 土 烏
 * First-order compounds (15): one entry for every primary pair
 * Second-order compounds (3): composed of two first-order compounds
 */
public class ModAspects {

    private static final Map<ResourceLocation, Aspect> REGISTRY = new LinkedHashMap<>();

    // ── Primary aspects ───────────────────────────────────────────────────────

    public static final Aspect WATER  = reg("water",  0x3A9BD5, List.of());
    public static final Aspect FIRE   = reg("fire",   0xE25822, List.of());
    public static final Aspect WOOD   = reg("wood",   0x4A7C3F, List.of());
    public static final Aspect METAL  = reg("metal",  0xC9A84C, List.of());
    public static final Aspect EARTH  = reg("earth",  0x8B5E3C, List.of());
    /** 烏 — chaotic turbulence, primordial substrate. Cannot be scanned directly. */
    public static final Aspect WU     = reg("wu",     0x2D1B4E, List.of());

    // ── First-order compounds (all 15 primary pairs) ──────────────────────────

    /** 魔力 — Flowing primordial energy. 水+烏 */
    public static final Aspect MANA        = reg("mana",        0x7B2FBE, List.of(WU,    WATER));
    /** 晶化 — Structured matter. 金+土 */
    public static final Aspect CRYSTAL     = reg("crystal",     0xA8D8EA, List.of(METAL, EARTH));
    /** 燃素 — Combustible life-energy. 火+木 */
    public static final Aspect PHLOGISTON  = reg("phlogiston",  0xFF6B35, List.of(FIRE,  WOOD));
    /** 共鳴 — Chaos captured by order. 烏+金 */
    public static final Aspect RESONANCE   = reg("resonance",   0x9B59B6, List.of(WU,    METAL));
    /** 能量 — Pure energy. 烏+火 */
    public static final Aspect ENERGY      = reg("energy",      0xF39C12, List.of(WU,    FIRE));
    /** 生機 — Life, organic vitality. 水+木 */
    public static final Aspect VITALITY    = reg("vitality",    0x5DBB63, List.of(WATER, WOOD));
    /** 熔岩 — Molten earth, smelting. 火+土 */
    public static final Aspect MAGMA       = reg("magma",       0xFF4500, List.of(FIRE,  EARTH));
    /** 機巧 — Machinery, crafting, precision. 木+金 */
    public static final Aspect MECHANISM   = reg("mechanism",   0xB8860B, List.of(WOOD,  METAL));
    /** 滋養 — Nutrients, soil, nourishment. 水+土 */
    public static final Aspect NOURISH     = reg("nourish",     0x8FBC8F, List.of(WATER, EARTH));
    /** 光輝 — Light, radiance, precision energy. 火+金 */
    public static final Aspect RADIANCE    = reg("radiance",    0xFFD700, List.of(FIRE,  METAL));
    /** 靈魄 — Consciousness, spirit, enchanting. 木+烏 */
    public static final Aspect ANIMA       = reg("anima",       0x9400D3, List.of(WOOD,  WU));
    /** 重力 — Weight, mass, density. 土+烏 */
    public static final Aspect GRAVITY     = reg("gravity",     0x4B0082, List.of(EARTH, WU));
    /** 腐蝕 — Chemical corrosion, rust. 水+金 */
    public static final Aspect CORROSION   = reg("corrosion",   0x6B8E23, List.of(WATER, METAL));
    /** 蒸騰 — Phase change, steam, vapor. 水+火 */
    public static final Aspect VAPOR       = reg("vapor",       0xAFD7E0, List.of(WATER, FIRE));
    /** 孕育 — Growth environment, cultivation. 木+土 */
    public static final Aspect GROWTH      = reg("growth",      0x228B22, List.of(WOOD,  EARTH));

    // ── Second-order compounds ────────────────────────────────────────────────

    /** 靈流 — Magical life essence. 生機+魔力 */
    public static final Aspect LIFEFLOW    = reg("lifeflow",    0x00CED1, List.of(VITALITY,   MANA));
    /** 動力 — Mechanical power, mana propulsion. 燃素+機巧 */
    public static final Aspect MOMENTUM    = reg("momentum",    0xFF8C00, List.of(PHLOGISTON, MECHANISM));
    /** 折射 — Crystal light, prismatic energy. 晶化+光輝 */
    public static final Aspect REFRACTION  = reg("refraction",  0x87CEEB, List.of(CRYSTAL,    RADIANCE));

    // ── Registry helpers ──────────────────────────────────────────────────────

    private static Aspect reg(String path, int color, List<Aspect> components) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        Aspect aspect = new Aspect(id, color, components);
        REGISTRY.put(id, aspect);
        return aspect;
    }

    public static Aspect get(ResourceLocation id)       { return REGISTRY.get(id); }
    public static Collection<Aspect> all()              { return REGISTRY.values(); }
    public static boolean contains(ResourceLocation id) { return REGISTRY.containsKey(id); }

    private ModAspects() {}
}
