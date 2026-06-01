package com.github.nalamodikk.research.skill.reaction;

/**
 * 化學「性質」標籤。每個效果本源帶一個或多個性質(像元素的反應特性),反應規則
 * ({@link ReactionRule})靠性質而非具體本源觸發,所以 80 種本源能湧現出大量組合。
 *
 * 前段是本源自帶的「輸入性質」;後段是反應「產物性質」,由規則生成、丟回反應池,
 * 能再符合別的規則 → 連鎖級聯(像真實化學的連鎖反應)。
 */
public enum ReactionTrait {
    // ── 輸入性質(本源自帶)──
    HEAT,       // 熱
    COLD,       // 寒
    WATER,      // 水
    ELECTRIC,   // 電
    TOXIC,      // 毒
    ACID,       // 酸
    METAL,      // 金屬
    LIGHT,      // 光
    DARK,       // 暗/死
    FORCE,      // 力
    ORGANIC,    // 生機
    ARCANE,     // 奧能
    LIFE,       // 生命

    // ── 產物性質(反應生成,可再反應 → 連鎖)──
    STEAM,      // 蒸氣(熱+水/寒)
    ASH,        // 灰燼(熱+生機)
    VOLATILE,   // 揮發氣(酸+金屬)
    PLASMA,     // 電漿(電+能/蒸氣)
    ACID_CLOUD  // 酸毒雲(毒+酸/熱)
}
