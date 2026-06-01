package com.github.nalamodikk.research.skill.reaction;

import com.github.nalamodikk.research.aspect.Aspect;

import java.util.List;

/**
 * 一個本源反應的「展示資料」:輸入本源(代表)、反應名稱鍵、效果說明鍵。
 *
 * 這份是給 JEI「本源反應」分頁顯示用的;實際的反應判定邏輯(用本源「家族」比對)在
 * {@code SkillCompiler.applyReactions}。這裡用代表性本源 + 註明「或同類」讓玩家看得懂。
 * 改反應時兩邊要一起更新。
 */
public record ReactionInfo(List<Aspect> inputs, String nameKey, String descKey) {

    /**
     * JEI 展示資料,直接從 {@link ReactionEngine#RULES} 衍生:每條規則的需求性質
     * 換成「代表本源」當輸入,加上規則自帶的名稱/說明鍵。規則改了 JEI 自動跟著變。
     */
    public static final List<ReactionInfo> ALL = ReactionEngine.RULES.stream()
            .map(rule -> new ReactionInfo(
                    rule.requires().stream().map(ReactionEngine::representative).toList(),
                    rule.nameKey(), rule.descKey()))
            .toList();
}
