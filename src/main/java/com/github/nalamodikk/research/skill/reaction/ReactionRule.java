package com.github.nalamodikk.research.skill.reaction;

import com.github.nalamodikk.research.skill.SkillEffectOp;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 一條反應規則:需要哪些性質同時存在(由不同來源提供)→ 觸發一個效果 op,
 * 並可生成「產物性質」丟回反應池(供下一段級聯),以及附加連鎖數。
 *
 * @param requires 需要的性質(由 {@link ReactionEngine} 用 SDR 配對確保來自不同來源,避免單一本源自觸發)
 * @param op       觸發的效果 op(可為 null,例如純連鎖的「導電」)
 * @param products 產物性質(丟回池子,可再反應)
 * @param chain    附加連鎖跳數
 * @param nameKey  JEI / 反饋顯示用的名稱翻譯鍵
 * @param descKey  JEI 說明翻譯鍵
 */
public record ReactionRule(List<ReactionTrait> requires, @Nullable SkillEffectOp op,
                           List<ReactionTrait> products, int chain,
                           String nameKey, String descKey) {

    /** 便利建構:無產物、無連鎖。 */
    public static ReactionRule of(String name, String desc, SkillEffectOp op, ReactionTrait... requires) {
        return new ReactionRule(List.of(requires), op, List.of(), 0, name, desc);
    }

    /** 便利建構:帶產物(會級聯)。 */
    public static ReactionRule producing(String name, String desc, SkillEffectOp op,
                                         ReactionTrait product, ReactionTrait... requires) {
        return new ReactionRule(List.of(requires), op, List.of(product), 0, name, desc);
    }

    /** 便利建構:帶連鎖跳數。 */
    public static ReactionRule chaining(String name, String desc, @Nullable SkillEffectOp op,
                                        int chain, ReactionTrait... requires) {
        return new ReactionRule(List.of(requires), op, List.of(), chain, name, desc);
    }
}
