package com.github.nalamodikk.research.skill.reaction;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.skill.SkillEffectOp;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.github.nalamodikk.research.skill.reaction.ReactionTrait.*;

/**
 * 化學元素表式的反應引擎。把技能裡效果本源的「性質」丟進反應池,反覆套用規則:
 * 每觸發一條規則就套上它的效果 op,並把產物性質丟回池子,下一輪可再符合別的規則
 * → 連鎖級聯。取代原本 {@code SkillCompiler.applyReactions} 的手寫死配方。
 *
 * 防呆:規則的需求性質用 SDR(系統相異代表)配對,必須由「不同來源」提供,所以單一
 * 帶兩個性質的本源(如熔岩=熱+力)不會自己觸發需要熱+力的反應。產物算獨立來源,
 * 所以能合法地驅動下一段連鎖。
 */
public final class ReactionEngine {

    private ReactionEngine() {}

    /** 級聯深度上限(防無限迴圈;每條規則一場戰鬥也只觸發一次)。 */
    private static final int MAX_DEPTH = 4;

    /** 反應結果:要附加的效果 op(依級聯順序)+ 額外連鎖跳數。 */
    public record Result(List<SkillEffectOp> ops, int chain) {}

    // ── 本源 → 性質表 ───────────────────────────────────────────────────────
    private static final Map<Aspect, EnumSet<ReactionTrait>> TRAITS = new java.util.HashMap<>();
    private static void t(Aspect a, ReactionTrait... traits) { TRAITS.put(a, EnumSet.of(traits[0], traits)); }
    static {
        // 火系
        t(ModAspects.PHLOGISTON, HEAT);
        t(ModAspects.MAGMA, HEAT, FORCE);
        t(ModAspects.LI, HEAT, LIGHT);
        t(ModAspects.FURNACE, HEAT, METAL);
        t(ModAspects.VAPOR, HEAT, WATER);
        t(ModAspects.STEAM, HEAT, WATER);
        // 冷
        t(ModAspects.FROST, COLD, WATER);
        // 電
        t(ModAspects.ZHEN, ELECTRIC);
        t(ModAspects.ARC, ELECTRIC);
        // 毒 / 酸
        t(ModAspects.VENOM, TOXIC);
        t(ModAspects.FAMINE, TOXIC);
        t(ModAspects.CORROSION, ACID);
        // 力 / 水
        t(ModAspects.STORM, FORCE);
        t(ModAspects.KAN, WATER, FORCE);
        t(ModAspects.DUI, WATER, FORCE);
        t(ModAspects.DESIRE, FORCE);
        t(ModAspects.EXCAVATION, METAL, FORCE);
        // 光
        t(ModAspects.RADIANCE, LIGHT);
        // 暗 / 死
        t(ModAspects.DEATH, DARK);
        t(ModAspects.UNDEAD, DARK);
        t(ModAspects.TAINT, DARK, TOXIC);
        t(ModAspects.ELDRITCH, DARK);
        t(ModAspects.SPIRITUS, DARK);
        t(ModAspects.VOID_ASPECT, DARK, FORCE);
        // 生機
        t(ModAspects.GROWTH, ORGANIC);
        t(ModAspects.BESTIA, ORGANIC);
        t(ModAspects.HARVEST, ORGANIC);
        // 奧能 / 金屬
        t(ModAspects.CRYSTAL, METAL, ARCANE);
        t(ModAspects.ENERGY, ARCANE);
        t(ModAspects.ARCANA, ARCANE);
        // 生命
        t(ModAspects.VITAE, LIFE);
        t(ModAspects.VITALITY, LIFE);
        t(ModAspects.MENDING, LIFE);
        t(ModAspects.LIFEFLOW, LIFE);
        t(ModAspects.NOURISH, LIFE);
    }

    /** 各性質的「代表本源」,給 JEI 規則表用色塊/可點查詢。 */
    private static final Map<ReactionTrait, Aspect> REP = new EnumMap<>(ReactionTrait.class);
    static {
        REP.put(HEAT, ModAspects.PHLOGISTON);
        REP.put(COLD, ModAspects.FROST);
        REP.put(WATER, ModAspects.KAN);
        REP.put(ELECTRIC, ModAspects.ZHEN);
        REP.put(TOXIC, ModAspects.VENOM);
        REP.put(ACID, ModAspects.CORROSION);
        REP.put(METAL, ModAspects.CRYSTAL);
        REP.put(LIGHT, ModAspects.RADIANCE);
        REP.put(DARK, ModAspects.DEATH);
        REP.put(FORCE, ModAspects.STORM);
        REP.put(ORGANIC, ModAspects.GROWTH);
        REP.put(ARCANE, ModAspects.ENERGY);
        REP.put(LIFE, ModAspects.VITAE);
        REP.put(STEAM, ModAspects.VAPOR);
        REP.put(ASH, ModAspects.FURNACE);
        REP.put(VOLATILE, ModAspects.ENERGY);
        REP.put(PLASMA, ModAspects.ZHEN);
        REP.put(ACID_CLOUD, ModAspects.CORROSION);
    }

    public static Aspect representative(ReactionTrait trait) { return REP.get(trait); }

    // ── 規則集 ──────────────────────────────────────────────────────────────
    // 命名鍵:有 op 的用該 op 的 translationKey;純連鎖/特殊的用 reaction.koniava.<id>。
    private static final SkillEffectOp NONE = null;
    private static String opName(SkillEffectOp op) { return op.translationKey(); }
    private static String desc(String id) { return "reaction.koniava." + id + ".desc"; }

    public static final List<ReactionRule> RULES = List.of(
            // ── 基礎兩性質反應 ──
            ReactionRule.producing(opName(SkillEffectOp.THERMAL_SHOCK), desc("thermal_shock"), SkillEffectOp.THERMAL_SHOCK, STEAM, HEAT, COLD),
            ReactionRule.of(opName(SkillEffectOp.COMBUSTION), desc("combustion"), SkillEffectOp.COMBUSTION, HEAT, ARCANE),
            ReactionRule.producing(opName(SkillEffectOp.TOXIC_BURN), desc("toxic_burn"), SkillEffectOp.TOXIC_BURN, ACID_CLOUD, HEAT, TOXIC),
            ReactionRule.of(opName(SkillEffectOp.SHATTER), desc("shatter"), SkillEffectOp.SHATTER, COLD, FORCE),
            ReactionRule.chaining("reaction.koniava.conduction", desc("conduction"), NONE, 2, ELECTRIC, WATER),
            ReactionRule.of(opName(SkillEffectOp.OVERLOAD), desc("overload"), SkillEffectOp.OVERLOAD, ARCANE, METAL),
            ReactionRule.of(opName(SkillEffectOp.DEATH_SIPHON), desc("death_siphon"), SkillEffectOp.DEATH_SIPHON, DARK, LIFE),
            ReactionRule.producing(opName(SkillEffectOp.WILDFIRE), desc("wildfire"), SkillEffectOp.WILDFIRE, ASH, HEAT, ORGANIC),
            ReactionRule.producing(opName(SkillEffectOp.STRONG_ACID), desc("strong_acid"), SkillEffectOp.STRONG_ACID, ACID_CLOUD, TOXIC, ACID),
            ReactionRule.of(opName(SkillEffectOp.ANNIHILATION), desc("annihilation"), SkillEffectOp.ANNIHILATION, LIGHT, DARK),
            ReactionRule.of(opName(SkillEffectOp.SOULFIRE), desc("soulfire"), SkillEffectOp.SOULFIRE, DARK, HEAT),
            ReactionRule.of(opName(SkillEffectOp.CRYO_VENOM), desc("cryo_venom"), SkillEffectOp.CRYO_VENOM, COLD, TOXIC),
            new ReactionRule(List.of(ELECTRIC, ARCANE), SkillEffectOp.OVERLOAD, List.of(PLASMA), 2, "reaction.koniava.plasma", desc("plasma")),
            ReactionRule.of(opName(SkillEffectOp.SOLAR_FLARE), desc("solar_flare"), SkillEffectOp.SOLAR_FLARE, LIGHT, HEAT),
            ReactionRule.of(opName(SkillEffectOp.OVERGROWTH), desc("overgrowth"), SkillEffectOp.OVERGROWTH, ORGANIC, WATER),
            ReactionRule.of(opName(SkillEffectOp.SHRAPNEL), desc("shrapnel"), SkillEffectOp.SHRAPNEL, METAL, FORCE),

            // ── 新增兩性質反應 ──
            ReactionRule.producing(opName(SkillEffectOp.STEAM_BURST), desc("steam_burst"), SkillEffectOp.STEAM_BURST, STEAM, HEAT, WATER),
            ReactionRule.producing(opName(SkillEffectOp.CORRODE), desc("corrode"), SkillEffectOp.CORRODE, VOLATILE, ACID, METAL),
            ReactionRule.of(opName(SkillEffectOp.KINETIC_BLAST), desc("kinetic_blast"), SkillEffectOp.KINETIC_BLAST, FORCE, ARCANE),
            ReactionRule.of(opName(SkillEffectOp.IMPLOSION), desc("implosion"), SkillEffectOp.IMPLOSION, FORCE, DARK),
            ReactionRule.producing(opName(SkillEffectOp.TOXIC_BURST), desc("toxic_burst"), SkillEffectOp.TOXIC_BURST, ACID_CLOUD, FORCE, TOXIC),
            ReactionRule.chaining(opName(SkillEffectOp.MAGNETIC_STORM), desc("magnetic_storm"), SkillEffectOp.MAGNETIC_STORM, 1, ELECTRIC, FORCE),
            ReactionRule.chaining(opName(SkillEffectOp.SPITE_BOLT), desc("spite_bolt"), SkillEffectOp.SPITE_BOLT, 1, ELECTRIC, DARK),
            ReactionRule.chaining(opName(SkillEffectOp.PHOTOELECTRIC), desc("photoelectric"), SkillEffectOp.PHOTOELECTRIC, 1, ELECTRIC, LIGHT),
            ReactionRule.of(opName(SkillEffectOp.PHOTOSYNTHESIS), desc("photosynthesis"), SkillEffectOp.PHOTOSYNTHESIS, LIGHT, ORGANIC),
            ReactionRule.of(opName(SkillEffectOp.SANCTIFY), desc("sanctify"), SkillEffectOp.SANCTIFY, LIGHT, LIFE),
            ReactionRule.of(opName(SkillEffectOp.RADIANT_SURGE), desc("radiant_surge"), SkillEffectOp.RADIANT_SURGE, LIGHT, ARCANE),
            ReactionRule.producing(opName(SkillEffectOp.PLAGUE), desc("plague"), SkillEffectOp.PLAGUE, ACID_CLOUD, DARK, TOXIC),
            ReactionRule.producing(opName(SkillEffectOp.ROT_BLOOM), desc("rot_bloom"), SkillEffectOp.ROT_BLOOM, ASH, DARK, ORGANIC),
            ReactionRule.of(opName(SkillEffectOp.VOID_SURGE), desc("void_surge"), SkillEffectOp.VOID_SURGE, DARK, ARCANE),
            ReactionRule.producing(opName(SkillEffectOp.SPORE_BURST), desc("spore_burst"), SkillEffectOp.SPORE_BURST, ACID_CLOUD, ORGANIC, TOXIC),
            ReactionRule.of(opName(SkillEffectOp.FLOURISH), desc("flourish"), SkillEffectOp.FLOURISH, ORGANIC, LIFE),
            ReactionRule.chaining(opName(SkillEffectOp.SUPERCONDUCT), desc("superconduct"), SkillEffectOp.SUPERCONDUCT, 2, COLD, ELECTRIC),
            ReactionRule.chaining(opName(SkillEffectOp.PRISM), desc("prism"), SkillEffectOp.PRISM, 1, COLD, LIGHT),
            ReactionRule.of(opName(SkillEffectOp.WITHER_FROST), desc("wither_frost"), SkillEffectOp.WITHER_FROST, COLD, DARK),
            ReactionRule.of(opName(SkillEffectOp.FROSTBIND), desc("frostbind"), SkillEffectOp.FROSTBIND, COLD, ORGANIC),
            ReactionRule.of(opName(SkillEffectOp.CRYOCRYSTAL), desc("cryocrystal"), SkillEffectOp.CRYOCRYSTAL, COLD, METAL),
            ReactionRule.of(opName(SkillEffectOp.MELTDOWN), desc("meltdown"), SkillEffectOp.MELTDOWN, HEAT, ACID),
            ReactionRule.of(opName(SkillEffectOp.ERUPTION), desc("eruption"), SkillEffectOp.ERUPTION, HEAT, FORCE),

            // ── 補滿性質矩陣(batch 4)──
            ReactionRule.chaining(opName(SkillEffectOp.THERMION), desc("thermion"), SkillEffectOp.THERMION, 1, HEAT, ELECTRIC),
            ReactionRule.of(opName(SkillEffectOp.SMELT), desc("smelt"), SkillEffectOp.SMELT, HEAT, METAL),
            ReactionRule.of(opName(SkillEffectOp.CAUTERIZE), desc("cauterize"), SkillEffectOp.CAUTERIZE, HEAT, LIFE),
            ReactionRule.of(opName(SkillEffectOp.DEEP_FREEZE), desc("deep_freeze"), SkillEffectOp.DEEP_FREEZE, COLD, WATER),
            ReactionRule.of(opName(SkillEffectOp.FROST_ETCH), desc("frost_etch"), SkillEffectOp.FROST_ETCH, COLD, ACID),
            ReactionRule.of(opName(SkillEffectOp.MANA_FREEZE), desc("mana_freeze"), SkillEffectOp.MANA_FREEZE, COLD, ARCANE),
            ReactionRule.of(opName(SkillEffectOp.HIBERNATE), desc("hibernate"), SkillEffectOp.HIBERNATE, COLD, LIFE),
            ReactionRule.producing(opName(SkillEffectOp.TAINTED_WATER), desc("tainted_water"), SkillEffectOp.TAINTED_WATER, ACID_CLOUD, WATER, TOXIC),
            ReactionRule.of(opName(SkillEffectOp.ACID_WASH), desc("acid_wash"), SkillEffectOp.ACID_WASH, WATER, ACID),
            ReactionRule.of(opName(SkillEffectOp.RUST), desc("rust"), SkillEffectOp.RUST, WATER, METAL),
            ReactionRule.of(opName(SkillEffectOp.HOLY_WATER), desc("holy_water"), SkillEffectOp.HOLY_WATER, WATER, LIGHT),
            ReactionRule.of(opName(SkillEffectOp.MIASMA), desc("miasma"), SkillEffectOp.MIASMA, WATER, DARK),
            ReactionRule.of(opName(SkillEffectOp.TORRENT), desc("torrent"), SkillEffectOp.TORRENT, WATER, FORCE),
            ReactionRule.of(opName(SkillEffectOp.MANA_SPRING), desc("mana_spring"), SkillEffectOp.MANA_SPRING, WATER, ARCANE),
            ReactionRule.of(opName(SkillEffectOp.LIVING_SPRING), desc("living_spring"), SkillEffectOp.LIVING_SPRING, WATER, LIFE),
            ReactionRule.chaining(opName(SkillEffectOp.ELECTROLYSIS), desc("electrolysis"), SkillEffectOp.ELECTROLYSIS, 1, ELECTRIC, TOXIC),
            ReactionRule.chaining(opName(SkillEffectOp.ELECTRO_CORROSION), desc("electro_corrosion"), SkillEffectOp.ELECTRO_CORROSION, 1, ELECTRIC, ACID),
            ReactionRule.of(opName(SkillEffectOp.EMP), desc("emp"), SkillEffectOp.EMP, ELECTRIC, METAL),
            ReactionRule.chaining(opName(SkillEffectOp.BIOELECTRIC), desc("bioelectric"), SkillEffectOp.BIOELECTRIC, 1, ELECTRIC, ORGANIC),
            ReactionRule.of(opName(SkillEffectOp.DEFIBRILLATE), desc("defibrillate"), SkillEffectOp.DEFIBRILLATE, ELECTRIC, LIFE),
            ReactionRule.of(opName(SkillEffectOp.HEAVY_METAL), desc("heavy_metal"), SkillEffectOp.HEAVY_METAL, TOXIC, METAL),
            ReactionRule.of(opName(SkillEffectOp.PURGE), desc("purge"), SkillEffectOp.PURGE, TOXIC, LIGHT),
            ReactionRule.of(opName(SkillEffectOp.ARCANE_VENOM), desc("arcane_venom"), SkillEffectOp.ARCANE_VENOM, TOXIC, ARCANE),
            ReactionRule.of(opName(SkillEffectOp.VIRULENCE), desc("virulence"), SkillEffectOp.VIRULENCE, TOXIC, LIFE),
            ReactionRule.of(opName(SkillEffectOp.SEARING_ETCH), desc("searing_etch"), SkillEffectOp.SEARING_ETCH, ACID, LIGHT),
            ReactionRule.of(opName(SkillEffectOp.DECAY), desc("decay"), SkillEffectOp.DECAY, ACID, DARK),
            ReactionRule.producing(opName(SkillEffectOp.ACID_SPLASH), desc("acid_splash"), SkillEffectOp.ACID_SPLASH, ACID_CLOUD, ACID, FORCE),
            ReactionRule.of(opName(SkillEffectOp.WILT), desc("wilt"), SkillEffectOp.WILT, ACID, ORGANIC),
            ReactionRule.of(opName(SkillEffectOp.MANA_CORROSION), desc("mana_corrosion"), SkillEffectOp.MANA_CORROSION, ACID, ARCANE),
            ReactionRule.of(opName(SkillEffectOp.NEUTRALIZE), desc("neutralize"), SkillEffectOp.NEUTRALIZE, ACID, LIFE),
            ReactionRule.of(opName(SkillEffectOp.MIRROR_FLASH), desc("mirror_flash"), SkillEffectOp.MIRROR_FLASH, METAL, LIGHT),
            ReactionRule.of(opName(SkillEffectOp.CURSED_METAL), desc("cursed_metal"), SkillEffectOp.CURSED_METAL, METAL, DARK),
            ReactionRule.of(opName(SkillEffectOp.PETRIFY), desc("petrify"), SkillEffectOp.PETRIFY, METAL, ORGANIC),
            ReactionRule.of(opName(SkillEffectOp.IRON_WARD), desc("iron_ward"), SkillEffectOp.IRON_WARD, METAL, LIFE),
            ReactionRule.of(opName(SkillEffectOp.LIGHT_PRESSURE), desc("light_pressure"), SkillEffectOp.LIGHT_PRESSURE, LIGHT, FORCE),
            ReactionRule.of(opName(SkillEffectOp.VINE_LASH), desc("vine_lash"), SkillEffectOp.VINE_LASH, FORCE, ORGANIC),
            ReactionRule.of(opName(SkillEffectOp.RESONANT_PULSE), desc("resonant_pulse"), SkillEffectOp.RESONANT_PULSE, FORCE, LIFE),
            ReactionRule.of(opName(SkillEffectOp.SYMBIOSIS), desc("symbiosis"), SkillEffectOp.SYMBIOSIS, ORGANIC, ARCANE),
            ReactionRule.of(opName(SkillEffectOp.ARCANE_MENDING), desc("arcane_mending"), SkillEffectOp.ARCANE_MENDING, ARCANE, LIFE),

            // ── 連鎖規則(吃前段「產物」才觸發,組成級聯)──
            new ReactionRule(List.of(STEAM, ELECTRIC), SkillEffectOp.SUPERHEATED_PLASMA, List.of(PLASMA), 2, opName(SkillEffectOp.SUPERHEATED_PLASMA), desc("superheated_plasma")),
            ReactionRule.chaining(opName(SkillEffectOp.HYDROGEN_BLAST), desc("hydrogen_blast"), SkillEffectOp.HYDROGEN_BLAST, 1, VOLATILE, HEAT),
            ReactionRule.of(opName(SkillEffectOp.ASH_STORM), desc("ash_storm"), SkillEffectOp.ASH_STORM, ASH, FORCE),
            ReactionRule.of(opName(SkillEffectOp.BIOHAZARD), desc("biohazard"), SkillEffectOp.BIOHAZARD, ACID_CLOUD, HEAT),

            // ── 三性質終極反應 ──
            ReactionRule.of(opName(SkillEffectOp.ELEMENTAL_STORM), desc("elemental_storm"), SkillEffectOp.ELEMENTAL_STORM, HEAT, COLD, ELECTRIC),
            ReactionRule.of(opName(SkillEffectOp.THERMONUCLEAR), desc("thermonuclear"), SkillEffectOp.THERMONUCLEAR, HEAT, ARCANE, ELECTRIC),
            ReactionRule.of(opName(SkillEffectOp.HOLY_NOVA), desc("holy_nova"), SkillEffectOp.HOLY_NOVA, LIGHT, DARK, ARCANE),
            ReactionRule.of(opName(SkillEffectOp.BLIGHT), desc("blight"), SkillEffectOp.BLIGHT, ORGANIC, HEAT, TOXIC),
            // 引力亂流:水+力的渦流,需三個來源(一帶力、兩帶水),如風暴+坎+兌
            ReactionRule.of(opName(SkillEffectOp.MAELSTROM), desc("maelstrom"), SkillEffectOp.MAELSTROM, FORCE, WATER, WATER)
    );

    // ── 級聯求解 ────────────────────────────────────────────────────────────
    /**
     * 跑反應。把效果本源的性質當作「來源」丟進池,逐輪套規則:能配對(SDR)的規則觸發,
     * 產物作為新來源加入下一輪,直到沒有新規則觸發或達深度上限。
     */
    public static Result run(List<Aspect> effects) {
        List<EnumSet<ReactionTrait>> sources = new ArrayList<>();
        for (Aspect a : effects) {
            EnumSet<ReactionTrait> traits = TRAITS.get(a);
            if (traits != null && !traits.isEmpty()) sources.add(traits);
        }
        if (sources.size() < 2) return new Result(List.of(), 0);

        List<SkillEffectOp> ops = new ArrayList<>();
        int chain = 0;
        boolean[] used = new boolean[RULES.size()];

        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            boolean any = false;
            List<EnumSet<ReactionTrait>> produced = new ArrayList<>();
            for (int i = 0; i < RULES.size(); i++) {
                if (used[i]) continue;
                ReactionRule rule = RULES.get(i);
                if (!canMatch(rule.requires(), sources)) continue;
                used[i] = true;
                any = true;
                if (rule.op() != null && !ops.contains(rule.op())) ops.add(rule.op());
                chain += rule.chain();
                for (ReactionTrait p : rule.products()) produced.add(EnumSet.of(p));
            }
            if (!any) break;
            sources.addAll(produced); // 產物加入下一輪 → 級聯
        }
        return new Result(ops, chain);
    }

    /** SDR:每個需求性質能否配到「相異」來源(避免單一多性質本源自觸發)。 */
    private static boolean canMatch(List<ReactionTrait> reqs, List<EnumSet<ReactionTrait>> sources) {
        return match(0, reqs, sources, new boolean[sources.size()]);
    }

    private static boolean match(int i, List<ReactionTrait> reqs, List<EnumSet<ReactionTrait>> sources, boolean[] usedSrc) {
        if (i >= reqs.size()) return true;
        ReactionTrait need = reqs.get(i);
        for (int j = 0; j < sources.size(); j++) {
            if (!usedSrc[j] && sources.get(j).contains(need)) {
                usedSrc[j] = true;
                if (match(i + 1, reqs, sources, usedSrc)) return true;
                usedSrc[j] = false;
            }
        }
        return false;
    }
}
