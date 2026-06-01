package com.github.nalamodikk.research.skill.reaction;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;

import java.util.List;

/**
 * 一個本源反應的「展示資料」:輸入本源(代表)、反應名稱鍵、效果說明鍵。
 *
 * 這份是給 JEI「本源反應」分頁顯示用的;實際的反應判定邏輯(用本源「家族」比對)在
 * {@code SkillCompiler.applyReactions}。這裡用代表性本源 + 註明「或同類」讓玩家看得懂。
 * 改反應時兩邊要一起更新。
 */
public record ReactionInfo(List<Aspect> inputs, String nameKey, String descKey) {

    private static ReactionInfo r(String nameKey, String descKey, Aspect... inputs) {
        return new ReactionInfo(List.of(inputs), nameKey, descKey);
    }

    /** 全部 22 個反應(16 兩兩 + 6 三本源)。 */
    public static final List<ReactionInfo> ALL = List.of(
            // 兩本源
            r("skillop.koniava.thermal_shock", "reaction.koniava.thermal_shock.desc", ModAspects.PHLOGISTON, ModAspects.FROST),
            r("skillop.koniava.combustion", "reaction.koniava.combustion.desc", ModAspects.PHLOGISTON, ModAspects.ENERGY),
            r("skillop.koniava.toxic_burn", "reaction.koniava.toxic_burn.desc", ModAspects.PHLOGISTON, ModAspects.VENOM),
            r("skillop.koniava.shatter", "reaction.koniava.shatter.desc", ModAspects.FROST, ModAspects.STORM),
            r("reaction.koniava.conduction", "reaction.koniava.conduction.desc", ModAspects.ZHEN, ModAspects.KAN),
            r("skillop.koniava.overload", "reaction.koniava.overload.desc", ModAspects.ENERGY, ModAspects.CRYSTAL),
            r("skillop.koniava.death_siphon", "reaction.koniava.death_siphon.desc", ModAspects.DEATH, ModAspects.VITAE),
            r("skillop.koniava.wildfire", "reaction.koniava.wildfire.desc", ModAspects.GROWTH, ModAspects.PHLOGISTON),
            r("skillop.koniava.strong_acid", "reaction.koniava.strong_acid.desc", ModAspects.VENOM, ModAspects.CORROSION),
            r("skillop.koniava.annihilation", "reaction.koniava.annihilation.desc", ModAspects.RADIANCE, ModAspects.DEATH),
            r("skillop.koniava.soulfire", "reaction.koniava.soulfire.desc", ModAspects.DEATH, ModAspects.PHLOGISTON),
            r("skillop.koniava.cryo_venom", "reaction.koniava.cryo_venom.desc", ModAspects.FROST, ModAspects.VENOM),
            r("reaction.koniava.plasma", "reaction.koniava.plasma.desc", ModAspects.ZHEN, ModAspects.ENERGY),
            r("skillop.koniava.solar_flare", "reaction.koniava.solar_flare.desc", ModAspects.RADIANCE, ModAspects.PHLOGISTON),
            r("skillop.koniava.overgrowth", "reaction.koniava.overgrowth.desc", ModAspects.GROWTH, ModAspects.KAN),
            r("skillop.koniava.shrapnel", "reaction.koniava.shrapnel.desc", ModAspects.CRYSTAL, ModAspects.STORM),
            // 三本源
            r("skillop.koniava.elemental_storm", "reaction.koniava.elemental_storm.desc", ModAspects.PHLOGISTON, ModAspects.FROST, ModAspects.ZHEN),
            r("skillop.koniava.thermonuclear", "reaction.koniava.thermonuclear.desc", ModAspects.PHLOGISTON, ModAspects.ENERGY, ModAspects.ZHEN),
            r("skillop.koniava.biohazard", "reaction.koniava.biohazard.desc", ModAspects.VENOM, ModAspects.CORROSION, ModAspects.PHLOGISTON),
            r("skillop.koniava.holy_nova", "reaction.koniava.holy_nova.desc", ModAspects.RADIANCE, ModAspects.DEATH, ModAspects.ENERGY),
            r("skillop.koniava.blight", "reaction.koniava.blight.desc", ModAspects.GROWTH, ModAspects.PHLOGISTON, ModAspects.VENOM),
            r("skillop.koniava.maelstrom", "reaction.koniava.maelstrom.desc", ModAspects.STORM, ModAspects.KAN, ModAspects.DUI)
    );
}
