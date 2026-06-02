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
        put(ModAspects.MACHINE,  SkillRole.CARRIER);   // 機械 -> 發射浮游砲子彈

        // ── Effects (payload) ────────────────────────────────────────────────
        put(ModAspects.PHLOGISTON, SkillRole.EFFECT);  // 燃燒/點燃
        put(ModAspects.MAGMA,      SkillRole.EFFECT);  // 岩漿灼燒
        put(ModAspects.LI,         SkillRole.EFFECT, SkillRole.CARRIER);  // 離 火傷 / 載體=天降 Meteor
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
        put(ModAspects.KAN,        SkillRole.EFFECT, SkillRole.CARRIER);  // 坎 水傷 / 載體=拋物榴彈 Lob
        put(ModAspects.DUI,        SkillRole.EFFECT, SkillRole.CARRIER);  // 兌 水 / 載體=環爆 Nova
        put(ModAspects.GROWTH,     SkillRole.EFFECT);  // 藤蔓纏繞
        put(ModAspects.STORM,      SkillRole.EFFECT);  // 風暴範圍
        put(ModAspects.RADIANCE,   SkillRole.EFFECT);  // 光輝 -> 閃光彈/致盲
        put(ModAspects.CRYSTAL,    SkillRole.EFFECT);  // 晶刺傷
        // 抽象本源效果（腦洞）
        put(ModAspects.EXCAVATION, SkillRole.EFFECT);  // 挖掘 -> 破甲 + 挖掘疲勞
        put(ModAspects.BESTIA,     SkillRole.EFFECT);  // 獸性 -> 撕咬 + 流血
        put(ModAspects.HARVEST,    SkillRole.EFFECT);  // 收割 -> 範圍橫掃
        put(ModAspects.SENSUS,     SkillRole.EFFECT);  // 感知 -> 標記發光 + 緩速
        put(ModAspects.COGNITION,  SkillRole.EFFECT);  // 認知 -> 心智干擾(暈+弱)
        put(ModAspects.DESIRE,     SkillRole.EFFECT);  // 慾望 -> 牽引拉近 + 緩速
        put(ModAspects.LAW,        SkillRole.EFFECT);  // 法則 -> 制裁(定身 + 依負面層數加傷)

        // ── Modifiers (alter) ────────────────────────────────────────────────
        put(ModAspects.AUTOMATION, SkillRole.MODIFIER); // 自動化 -> 自動連射(+1 發)
        put(ModAspects.REFRACTION, SkillRole.MODIFIER); // 分裂多發
        put(ModAspects.RESONANCE,  SkillRole.MODIFIER); // 回響/重複
        put(ModAspects.BLADE,      SkillRole.CARRIER, SkillRole.MODIFIER); // 載體=劍氣斬 / 修飾=穿透
        put(ModAspects.BINDING,    SkillRole.MODIFIER); // 定身
        put(ModAspects.GEN,        SkillRole.MODIFIER, SkillRole.CARRIER); // 艮 緩停/釘住 / 載體=震波 Earthwave
        put(ModAspects.WARDING,    SkillRole.MODIFIER); // 護盾
        put(ModAspects.FORTIFY,    SkillRole.MODIFIER); // 增傷
        put(ModAspects.SHADOW,     SkillRole.MODIFIER); // 致盲/潛行
        put(ModAspects.KUN,        SkillRole.MODIFIER, SkillRole.CARRIER); // 坤 防禦/減傷 / 載體=衝擊波環 Shockwave
        put(ModAspects.QIAN,       SkillRole.MODIFIER); // 乾 大幅增威
        put(ModAspects.MECHANISM,  SkillRole.MODIFIER); // 精準/命中
        put(ModAspects.INSTRUMENT, SkillRole.MODIFIER); // 器具 精準
        put(ModAspects.ANIMA,      SkillRole.MODIFIER, SkillRole.CARRIER); // 靈魄 -> 追蹤/homing / 載體=軌道靈球 Orbital
        put(ModAspects.AURA,       SkillRole.MODIFIER); // 光暈 範圍增益
        put(ModAspects.ALCHEMY,    SkillRole.MODIFIER); // 煉化/轉化
        put(ModAspects.CORPUS,     SkillRole.MODIFIER); // 軀體 -> 肉體強化(最大生命)
        put(ModAspects.FAITH,      SkillRole.MODIFIER); // 信仰 -> 再生/撐住
        put(ModAspects.ORDER,      SkillRole.MODIFIER); // 秩序 -> 淨化自身負面
        put(ModAspects.INSTINCT,   SkillRole.MODIFIER); // 本能 -> 暴擊(隨機增傷)
        put(ModAspects.WISDOM,     SkillRole.MODIFIER); // 智慧 -> 降魔力花費
        put(ModAspects.GEAR,       SkillRole.MODIFIER); // 齒輪 -> 縮短冷卻(加速)

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
        // 社會/特殊本源也各自獨立成戰鬥變種（用戶要每個本源都能用），同時保留世界觀意義。
        put(ModAspects.CIVILIZATION, SkillRole.MODIFIER);   // 文明 -> 堡壘(抗性 II 自我)
        put(ModAspects.HUMANITY,     SkillRole.MODIFIER);   // 人性 -> 決意(大吸收+速度自我)
        put(ModAspects.COMMERCE,     SkillRole.EFFECT);     // 商貿 -> 掠奪(傷害+自己得護盾)
        put(ModAspects.LANGUAGE,     SkillRole.EFFECT);     // 語言 -> 詛咒之言(虛弱 II+挖掘疲勞)
        put(ModAspects.PRIMORDIAL,   SkillRole.EFFECT);     // 原初 -> 突變(每次隨機 debuff)
        put(ModAspects.WEALTH,       SkillRole.EFFECT);     // 財富 -> 點金一擊(最大單發加傷)
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
