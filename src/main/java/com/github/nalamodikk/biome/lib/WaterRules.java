package com.github.nalamodikk.biome.lib;

/**
 * 🌊 水處理規則枚舉
 */
public enum WaterRules {
    /**
     * 🚫 避免水 - 不在水中和水下替換方塊（預設）
     */
    AVOID_WATER,

    /**
     * 🌊 允許水下 - 可以在水下替換方塊
     */
    ALLOW_UNDERWATER,

    /**
     * 🏖️ 靠近水源 - 只在水源附近替換方塊
     */
    ONLY_NEAR_WATER,

    /**
     * 🌋 水變岩漿 - 將水替換為岩漿
     */
    REPLACE_WITH_LAVA
}
