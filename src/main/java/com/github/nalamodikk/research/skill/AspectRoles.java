package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The C dictionary: which {@link SkillRole}(s) each aspect can play in a skill.
 *
 * Roles are authored from each aspect's existing meaning (C rule: meaning -> role),
 * not derived algorithmically. Some aspects are multi-role (e.g. GRAVITY = carrier
 * field OR pull/slow modifier); the placement slot decides which applies.
 *
 * Aspects absent from this map have no skill role (abstract society/knowledge
 * aspects: civilization, order, law, automation, commerce, wealth, faith, wisdom,
 * language, humanity, instinct, desire, cognition, and pure economy/industry ones).
 * They stay research/worldview only, per design.
 *
 * Negative/decay aspects (death, void, undead, taint, eldritch, spiritus) are
 * tagged here as effects but flagged HIGH_TIER: the design call on gating them is
 * still open, so treat them as advanced/late content.
 */
public final class AspectRoles {

    private static final Map<Aspect, Set<SkillRole>> ROLES = new HashMap<>();

    /** High-tier / late aspects whose final gating is still undecided. */
    private static final Set<Aspect> HIGH_TIER = new java.util.HashSet<>();

    static {
        // ── Primary fuel (element + intensity) ───────────────────────────────
        put(ModAspects.WATER, SkillRole.FUEL);
        put(ModAspects.FIRE,  SkillRole.FUEL);
        put(ModAspects.WOOD,  SkillRole.FUEL);
        put(ModAspects.METAL, SkillRole.FUEL);
        put(ModAspects.EARTH, SkillRole.FUEL);
        put(ModAspects.WU,    SkillRole.FUEL);

        // ── Carriers (delivery) ──────────────────────────────────────────────
        put(ModAspects.MOMENTUM, SkillRole.CARRIER);   // 推進 -> projectile
        put(ModAspects.FLIGHT,   SkillRole.CARRIER);   // 飛行 -> self dash
        put(ModAspects.XUN,      SkillRole.CARRIER);   // 風 -> gust projectile
        put(ModAspects.MANA,     SkillRole.CARRIER);   // 魔力流 -> basic orb
        put(ModAspects.PIPELINE, SkillRole.CARRIER);   // 管道 -> channelled beam

        // ── Effects (payload) ────────────────────────────────────────────────
        put(ModAspects.PHLOGISTON, SkillRole.EFFECT);  // 燃燒/點燃
        put(ModAspects.MAGMA,      SkillRole.EFFECT);  // 岩漿灼燒
        put(ModAspects.LI,         SkillRole.EFFECT);  // 離 火傷
        put(ModAspects.VAPOR,      SkillRole.EFFECT);  // 蒸汽範圍
        put(ModAspects.STEAM,      SkillRole.EFFECT);  // 蒸汽
        put(ModAspects.FURNACE,    SkillRole.EFFECT);  // 爐火灼燒
        put(ModAspects.FROST,      SkillRole.EFFECT);  // 凍結/緩速
        put(ModAspects.ZHEN,       SkillRole.EFFECT);  // 雷擊
        put(ModAspects.VENOM,      SkillRole.EFFECT);  // 中毒
        put(ModAspects.CORROSION,  SkillRole.EFFECT);  // 酸蝕/破甲
        put(ModAspects.FAMINE,     SkillRole.EFFECT);  // 飢弱
        put(ModAspects.VITALITY,   SkillRole.EFFECT);  // 補血
        put(ModAspects.MENDING,    SkillRole.EFFECT);  // 補血(強)
        put(ModAspects.LIFEFLOW,   SkillRole.EFFECT);  // 補血/再生
        put(ModAspects.NOURISH,    SkillRole.EFFECT);  // 再生
        put(ModAspects.VITAE,      SkillRole.EFFECT);  // 血氣 -> 吸血
        put(ModAspects.KAN,        SkillRole.EFFECT);  // 坎 水傷
        put(ModAspects.DUI,        SkillRole.EFFECT);  // 兌 水
        put(ModAspects.GROWTH,     SkillRole.EFFECT);  // 藤蔓纏繞
        put(ModAspects.STORM,      SkillRole.EFFECT);  // 風暴範圍
        put(ModAspects.RADIANCE,   SkillRole.EFFECT);  // 光輝 -> 閃光彈/致盲
        put(ModAspects.CRYSTAL,    SkillRole.EFFECT);  // 晶刺傷

        // ── Modifiers (alter) ────────────────────────────────────────────────
        put(ModAspects.REFRACTION, SkillRole.MODIFIER); // 分裂多發
        put(ModAspects.RESONANCE,  SkillRole.MODIFIER); // 回響/重複
        put(ModAspects.BLADE,      SkillRole.MODIFIER); // 穿透
        put(ModAspects.BINDING,    SkillRole.MODIFIER); // 定身
        put(ModAspects.GEN,        SkillRole.MODIFIER); // 艮 緩停/釘住
        put(ModAspects.WARDING,    SkillRole.MODIFIER); // 護盾
        put(ModAspects.FORTIFY,    SkillRole.MODIFIER); // 增傷
        put(ModAspects.SHADOW,     SkillRole.MODIFIER); // 致盲/潛行
        put(ModAspects.KUN,        SkillRole.MODIFIER); // 坤 防禦/減傷
        put(ModAspects.QIAN,       SkillRole.MODIFIER); // 乾 大幅增威
        put(ModAspects.MECHANISM,  SkillRole.MODIFIER); // 精準/命中
        put(ModAspects.INSTRUMENT, SkillRole.MODIFIER); // 器具 精準
        put(ModAspects.ANIMA,      SkillRole.MODIFIER); // 靈魄 -> 追蹤/homing
        put(ModAspects.AURA,       SkillRole.MODIFIER); // 光暈 範圍增益
        put(ModAspects.ALCHEMY,    SkillRole.MODIFIER); // 煉化/轉化

        // ── Multi-role (slot decides) ────────────────────────────────────────
        put(ModAspects.GRAVITY, SkillRole.CARRIER, SkillRole.MODIFIER); // 範圍力場 / 拉緩
        put(ModAspects.ENERGY,  SkillRole.EFFECT,  SkillRole.MODIFIER); // 能量爆 / 增幅
        put(ModAspects.ARCANA,  SkillRole.EFFECT,  SkillRole.MODIFIER); // 奧術彈 / 奧術增幅
        put(ModAspects.PROPAGATION, SkillRole.EFFECT, SkillRole.MODIFIER); // 蔓延 / 連鎖
        putHighTier(ModAspects.ARC, SkillRole.EFFECT, SkillRole.MODIFIER); // 電弧 / 連鎖

        // ── High-tier negative / decay (gating undecided) ────────────────────
        putHighTier(ModAspects.DEATH,        SkillRole.EFFECT); // 凋零
        putHighTier(ModAspects.VOID_ASPECT,  SkillRole.EFFECT); // 虛空傷
        putHighTier(ModAspects.UNDEAD,       SkillRole.EFFECT); // 亡靈
        putHighTier(ModAspects.TAINT,        SkillRole.EFFECT); // 污染
        putHighTier(ModAspects.ELDRITCH,     SkillRole.EFFECT); // 異界
        putHighTier(ModAspects.SPIRITUS,     SkillRole.EFFECT); // 靈魂

        // ── Non-combat / special / system (categorised, NOT skill-usable) ────
        // These are tagged so they are explicitly "not a combat piece", per design:
        // they belong to research / worldview / Nara / economy, not the skill palette.
        put(ModAspects.CIVILIZATION, SkillRole.NON_COMBAT); // 文明 -> 研究/世界觀
        put(ModAspects.COMMERCE,     SkillRole.NON_COMBAT); // 商貿 -> 經濟系統
        put(ModAspects.LANGUAGE,     SkillRole.NON_COMBAT); // 語言 -> 符文/指南書/娜拉
        put(ModAspects.HUMANITY,     SkillRole.NON_COMBAT); // 人性 -> 核心敘事/娜拉
        put(ModAspects.PRIMORDIAL,   SkillRole.SPECIAL);    // 原初 -> 晚期萬用替身/變異
        put(ModAspects.WEALTH,       SkillRole.SYSTEM);     // 財富 -> 消耗物品換威力
    }

    // ── API ──────────────────────────────────────────────────────────────────

    public static Set<SkillRole> rolesOf(Aspect aspect) {
        return ROLES.getOrDefault(aspect, Set.of());
    }

    public static boolean hasRole(Aspect aspect, SkillRole role) {
        return rolesOf(aspect).contains(role);
    }

    /**
     * True if the aspect can actually be placed into a skill: it must hold a
     * combat role (carrier / effect / modifier). FUEL-only primaries and the
     * NON_COMBAT / SPECIAL / SYSTEM aspects are tagged but not skill-usable, so
     * they never appear in the encoder palette.
     */
    public static boolean isUsable(Aspect aspect) {
        Set<SkillRole> roles = rolesOf(aspect);
        return roles.contains(SkillRole.CARRIER)
                || roles.contains(SkillRole.EFFECT)
                || roles.contains(SkillRole.MODIFIER);
    }

    /** True for late/advanced aspects whose unlock gating is still being decided. */
    public static boolean isHighTier(Aspect aspect) {
        return HIGH_TIER.contains(aspect);
    }

    public static Map<Aspect, Set<SkillRole>> all() {
        return Collections.unmodifiableMap(ROLES);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static void put(Aspect aspect, SkillRole... roles) {
        ROLES.put(aspect, Collections.unmodifiableSet(EnumSet.of(roles[0], roles)));
    }

    private static void putHighTier(Aspect aspect, SkillRole... roles) {
        put(aspect, roles);
        HIGH_TIER.add(aspect);
    }

    private AspectRoles() {}
}
