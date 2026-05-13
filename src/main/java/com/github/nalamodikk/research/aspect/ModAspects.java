package com.github.nalamodikk.research.aspect;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all 80 aspects in Koniavacraft.
 *
 * Primary (6): 水 火 木 金 土 烏
 * First-order compounds (15): one entry for every primary pair
 * Second-order compounds (3): composed of two first-order compounds
 * Third-order / Eight Trigrams (8): 乾坤震巽坎離艮兌
 * Extended compounds (48): natural, knowledge, magic, life, economy, industry, civilization
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

    // ── Third-order compounds: Eight Trigrams (八卦) ──────────────────────────

    /** 乾 (Heaven) ☰ — Pure Yang, absolute energy. */
    public static final Aspect QIAN = reg("qian", 0xFFFFE0, List.of(RADIANCE, MOMENTUM));
    /** 坤 (Earth) ☷ — Pure Yin, absolute stability. */
    public static final Aspect KUN = reg("kun", 0x5D4037, List.of(EARTH, NOURISH));
    /** 震 (Thunder) ☳ — Dynamic power, resonance. */
    public static final Aspect ZHEN = reg("zhen", 0x00FF00, List.of(ENERGY, RESONANCE));
    /** 巽 (Wind) ☴ — Phase change, movement. */
    public static final Aspect XUN = reg("xun", 0x40E0D0, List.of(VAPOR, MOMENTUM));
    /** 坎 (Water) ☵ — Depth, flowing life. */
    public static final Aspect KAN = reg("kan", 0x000080, List.of(WATER, LIFEFLOW));
    /** 離 (Fire) ☲ — Illumination, heat. */
    public static final Aspect LI = reg("li", 0xFF0000, List.of(FIRE, REFRACTION));
    /** 艮 (Mountain) ☶ — Weight, stillness. */
    public static final Aspect GEN = reg("gen", 0x808000, List.of(EARTH, GRAVITY));
    /** 兌 (Lake) ☱ — Refracted water, joy. */
    public static final Aspect DUI = reg("dui", 0xADD8E6, List.of(WATER, CRYSTAL));

    // ── Extended: Tier A — depends only on the original 32 ───────────────────

    /** 暗影 — Darkness, shadow. 烏+光輝 */
    public static final Aspect SHADOW       = reg("shadow",       0x1C1C2E, List.of(WU,         RADIANCE));
    /** 冰寒 — Ice, cold. 水+晶化 */
    public static final Aspect FROST        = reg("frost",        0xB0E8FF, List.of(WATER,       CRYSTAL));
    /** 知識 — Knowledge, cognition. 靈魄+光輝 */
    public static final Aspect COGNITION    = reg("cognition",    0x4169E1, List.of(ANIMA,       RADIANCE));
    /** 毒素 — Venom, poison. 腐蝕+生機 */
    public static final Aspect VENOM        = reg("venom",        0x39FF14, List.of(CORROSION,   VITALITY));
    /** 亡靈 — Undead, animated corpse. 靈魄+腐蝕 */
    public static final Aspect UNDEAD       = reg("undead",       0x8B9467, List.of(ANIMA,       CORROSION));
    /** 療癒 — Mending, healing. 生機+靈流 */
    public static final Aspect MENDING      = reg("mending",      0x00FF7F, List.of(VITALITY,    LIFEFLOW));
    /** 污染 — Taint, magical corruption. 腐蝕+魔力 */
    public static final Aspect TAINT        = reg("taint",        0x6A0DAD, List.of(CORROSION,   MANA));
    /** 束縛 — Binding, restraint. 重力+魔力 */
    public static final Aspect BINDING      = reg("binding",      0x708090, List.of(GRAVITY,     MANA));
    /** 飛翔 — Flight, aerial movement. 蒸騰+能量 */
    public static final Aspect FLIGHT       = reg("flight",       0x87CEEB, List.of(VAPOR,       ENERGY));
    /** 豐收 — Harvest, abundance. 孕育+機巧 */
    public static final Aspect HARVEST      = reg("harvest",      0xDAA520, List.of(GROWTH,      MECHANISM));
    /** 器具 — Instrument, tool. 機巧+金 */
    public static final Aspect INSTRUMENT   = reg("instrument",   0xA9A9A9, List.of(MECHANISM,   METAL));
    /** 異界 — Eldritch, alien resonance. 烏+共鳴 */
    public static final Aspect ELDRITCH     = reg("eldritch",     0xDA70D6, List.of(WU,          RESONANCE));
    /** 財富 — Wealth, fortune. 光輝+機巧 */
    public static final Aspect WEALTH       = reg("wealth",       0xFFD700, List.of(RADIANCE,    MECHANISM));
    /** 奧術 — Arcane magic. 魔力+共鳴 */
    public static final Aspect ARCANA       = reg("arcana",       0x8A2BE2, List.of(MANA,        RESONANCE));
    /** 飢渴 — Famine, hunger. 滋養+腐蝕 */
    public static final Aspect FAMINE       = reg("famine",       0xA0522D, List.of(NOURISH,     CORROSION));
    /** 虛空 — Absolute void. 烏+重力 */
    public static final Aspect VOID_ASPECT  = reg("void",         0x0D0D0D, List.of(WU,          GRAVITY));
    /** 死滅 — Death, cessation. 靈魄+重力 */
    public static final Aspect DEATH        = reg("death",        0x2F4F4F, List.of(ANIMA,       GRAVITY));
    /** 肉身 — Body, flesh. 生機+土 */
    public static final Aspect CORPUS       = reg("corpus",       0xFF9999, List.of(VITALITY,    EARTH));
    /** 原質 — Primordial ooze, proto-life. 水+生機 */
    public static final Aspect PRIMORDIAL   = reg("primordial",   0x3B7A57, List.of(WATER,       VITALITY));
    /** 繁衍 — Propagation, breeding. 生機+孕育 */
    public static final Aspect PROPAGATION  = reg("propagation",  0x32CD32, List.of(VITALITY,    GROWTH));
    /** 血氣 — Vitae, blood energy. 生機+能量 */
    public static final Aspect VITAE        = reg("vitae",        0xCC0000, List.of(VITALITY,    ENERGY));
    /** 蒸汽 — Steam power. 蒸騰+動力 */
    public static final Aspect STEAM        = reg("steam",        0xD8D8D8, List.of(VAPOR,       MOMENTUM));

    // ── Extended: Tier B — depends on Tier A ─────────────────────────────────

    /** 感知 — Sense, perception. 知識+生機 */
    public static final Aspect SENSUS       = reg("sensus",       0x00BCD4, List.of(COGNITION,   VITALITY));
    /** 鋒刃 — Blade, weapon edge. 器具+火 */
    public static final Aspect BLADE        = reg("blade",        0xC0C0C0, List.of(INSTRUMENT,  FIRE));
    /** 防護 — Warding, protection. 器具+土 */
    public static final Aspect WARDING      = reg("warding",      0x5F9EA0, List.of(INSTRUMENT,  EARTH));
    /** 掘進 — Excavation, mining. 器具+晶化 */
    public static final Aspect EXCAVATION   = reg("excavation",   0x8B4513, List.of(INSTRUMENT,  CRYSTAL));
    /** 機械 — Machine, mechanism. 器具+動力 */
    public static final Aspect MACHINE      = reg("machine",      0x708090, List.of(INSTRUMENT,  MOMENTUM));
    /** 爐火 — Furnace, forge fire. 燃素+器具 */
    public static final Aspect FURNACE      = reg("furnace",      0xFF4500, List.of(PHLOGISTON,  INSTRUMENT));
    /** 煉化 — Alchemy, transmutation. 奧術+火 */
    public static final Aspect ALCHEMY      = reg("alchemy",      0xBDB76B, List.of(ARCANA,      FIRE));
    /** 光暈 — Aura, radiant field. 奧術+蒸騰 */
    public static final Aspect AURA         = reg("aura",         0xFFEA00, List.of(ARCANA,      VAPOR));
    /** 靈魂 — Soul, spirit. 靈魄+奧術 */
    public static final Aspect SPIRITUS     = reg("spiritus",     0xE0FFFF, List.of(ANIMA,       ARCANA));
    /** 獸性 — Beast nature. 生機+飛翔 */
    public static final Aspect BESTIA       = reg("bestia",       0x8B6914, List.of(VITALITY,    FLIGHT));
    /** 慾望 — Desire, longing. 知識+暗影 */
    public static final Aspect DESIRE       = reg("desire",       0xC71585, List.of(COGNITION,   SHADOW));
    /** 風暴 — Storm, tempest. 飛翔+震 */
    public static final Aspect STORM        = reg("storm",        0x4682B4, List.of(FLIGHT,      ZHEN));

    // ── Extended: Tier C — depends on Tier B ─────────────────────────────────

    /** 人性 — Humanity, human nature. 感知+靈魄 */
    public static final Aspect HUMANITY     = reg("humanity",     0xFFAB8C, List.of(SENSUS,      ANIMA));
    /** 本能 — Instinct, animal drive. 獸性+感知 */
    public static final Aspect INSTINCT     = reg("instinct",     0xFF8C00, List.of(BESTIA,      SENSUS));
    /** 強化 — Fortify, reinforce. 防護+動力 */
    public static final Aspect FORTIFY      = reg("fortify",      0x1E90FF, List.of(WARDING,     MOMENTUM));
    /** 齒輪 — Gear, cog. 機械+晶化 */
    public static final Aspect GEAR         = reg("gear",         0x8B7355, List.of(MACHINE,     CRYSTAL));
    /** 管道 — Pipeline, conduit flow. 機械+束縛 */
    public static final Aspect PIPELINE     = reg("pipeline",     0x5B8DB8, List.of(MACHINE,     BINDING));
    /** 電弧 — Arc, electric discharge. 能量+鋒刃 */
    public static final Aspect ARC          = reg("arc",          0x00FFFF, List.of(ENERGY,      BLADE));
    /** 信仰 — Faith, belief. 靈魂+奧術 */
    public static final Aspect FAITH        = reg("faith",        0xFFFAF0, List.of(SPIRITUS,    ARCANA));
    /** 知慧 — Wisdom, deep understanding. 知識+靈魂 */
    public static final Aspect WISDOM       = reg("wisdom",       0x483D8B, List.of(COGNITION,   SPIRITUS));
    /** 語言 — Language, communication. 知識+感知 */
    public static final Aspect LANGUAGE     = reg("language",     0x6495ED, List.of(COGNITION,   SENSUS));

    // ── Extended: Tier D — depends on Tier C ─────────────────────────────────

    /** 秩序 — Order, social structure. 人性+器具 */
    public static final Aspect ORDER        = reg("order",        0x191970, List.of(HUMANITY,    INSTRUMENT));
    /** 交易 — Commerce, trade. 財富+人性 */
    public static final Aspect COMMERCE     = reg("commerce",     0xDAA520, List.of(WEALTH,      HUMANITY));

    // ── Extended: Tier E — depends on Tier D ─────────────────────────────────

    /** 律法 — Law, rule. 秩序+束縛 */
    public static final Aspect LAW          = reg("law",          0x696969, List.of(ORDER,       BINDING));
    /** 自動 — Automation, self-operation. 機械+秩序 */
    public static final Aspect AUTOMATION   = reg("automation",   0x00FF41, List.of(MACHINE,     ORDER));
    /** 文明 — Civilization, society. 秩序+知慧 */
    public static final Aspect CIVILIZATION = reg("civilization", 0x800080, List.of(ORDER,       WISDOM));

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
