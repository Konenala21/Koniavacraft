package com.github.nalamodikk.research.skill.reaction;

import com.github.nalamodikk.research.aspect.Aspect;

import java.util.List;

/**
 * 一個本源反應的「展示資料」:輸入本源(代表)、反應名稱鍵、效果說明鍵。
 *
 * 這份是給 JEI「本源反應」分頁顯示用的;{@link #ALL} 直接從 {@link ReactionEngine#RULES} 衍生,
 * 用每條規則需求性質的「代表本源」當輸入,所以改 {@code ReactionEngine.RULES} 這裡自動跟著變。
 * 實際的反應判定(性質規則 + 級聯)在 {@link ReactionEngine}。
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
