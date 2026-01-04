package com.github.nalamodikk.narasystem.nara.api;

import net.minecraft.world.item.ItemStack;

/**
 * 對 ItemStack 提供娜拉系統的印記檢查。
 * 適用於顯示「已接入娜拉系統」的物品視覺效果、Tooltip 或判定。
 */
public interface INaraImprint {

    /**
     * 是否已被接入娜拉系統。
     * @param stack 目標物品
     * @return 是否接入
     */
    boolean hasNaraImprint(ItemStack stack);

    /**
     * 標記為已接入娜拉系統。
     * @param stack 目標物品
     */
    void setNaraImprint(ItemStack stack);
}
