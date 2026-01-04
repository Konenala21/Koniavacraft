// 🌍 生態系統配置系統
package com.github.nalamodikk.biome.lib;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Supplier;

/**
 * 🌱 生態系統配置記錄
 *
 * 定義一個完整的地形生態系統，包括：
 * - 地表方塊（草地、沙子等）
 * - 土壤方塊（普通土壤）
 * - 深層土壤方塊（深層土壤）
 * - 石頭替換（特殊石頭類型）
 * - 水規則（如何處理水）
 * - 優先級（決定處理順序）
 */
public record EcosystemConfig(
        Supplier<Block> surfaceBlock,     // 🌿 地表方塊
        Supplier<Block> soilBlock,        // 🌾 土壤方塊
        Supplier<Block> deepSoilBlock,    // 🏔️ 深層土壤方塊
        Supplier<Block> stoneBlock,       // 🗿 石頭替換方塊
        int deepSoilThreshold,            // 🏔️ 深層土壤Y座標閾值
        int stoneThreshold,               // 🗿 石頭替換Y座標閾值
        WaterRules waterRules,            // 🌊 水處理規則
        int priority                      // 🎯 處理優先級
) {

    /**
     * 🏗️ 創建配置建構器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 🔍 檢查所有必需的方塊是否有效
     */
    public boolean areBlocksValid() {
        return isBlockValid(surfaceBlock) ||
                isBlockValid(soilBlock) ||
                isBlockValid(deepSoilBlock) ||
                isBlockValid(stoneBlock);
    }

    /**
     * 🔍 檢查單個方塊是否有效
     */
    private boolean isBlockValid(Supplier<Block> blockSupplier) {
        if (blockSupplier == null) return false;
        try {
            Block block = blockSupplier.get();
            return block != null && block != Blocks.AIR;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🏗️ 生態系統配置建構器
     */
    public static class Builder {
        private Supplier<Block> surfaceBlock = null;
        private Supplier<Block> soilBlock = null;
        private Supplier<Block> deepSoilBlock = null;
        private Supplier<Block> stoneBlock = null;
        private int deepSoilThreshold = 20; // 預設Y=20以下為深層
        private int stoneThreshold = 0;     // 預設Y=0以下替換石頭
        private WaterRules waterRules = WaterRules.AVOID_WATER; // 預設避免水
        private int priority = 5; // 預設優先級

        /**
         * 🌿 設定地表方塊（草地、沙子等）
         */
        public Builder surfaceBlock(Supplier<Block> block) {
            this.surfaceBlock = block;
            return this;
        }

        /**
         * 🌾 設定土壤方塊
         */
        public Builder soilBlock(Supplier<Block> block) {
            this.soilBlock = block;
            return this;
        }

        /**
         * 🏔️ 設定深層土壤方塊和閾值
         */
        public Builder deepSoilBlock(Supplier<Block> block, int threshold) {
            this.deepSoilBlock = block;
            this.deepSoilThreshold = threshold;
            return this;
        }

        /**
         * 🏔️ 設定深層土壤方塊（使用預設閾值Y=20）
         */
        public Builder deepSoilBlock(Supplier<Block> block) {
            return deepSoilBlock(block, 20);
        }

        /**
         * 🏔️ 設定深層土壤閾值
         */
        public Builder deepSoilThreshold(int threshold) {
            this.deepSoilThreshold = threshold;
            return this;
        }

        /**
         * 🗿 設定石頭替換方塊和閾值
         */
        public Builder stoneBlock(Supplier<Block> block, int threshold) {
            this.stoneBlock = block;
            this.stoneThreshold = threshold;
            return this;
        }

        /**
         * 🗿 設定石頭替換方塊（使用預設閾值Y=0）
         */
        public Builder stoneBlock(Supplier<Block> block) {
            return stoneBlock(block, 0);
        }

        /**
         * 🗿 設定石頭替換閾值
         */
        public Builder stoneThreshold(int threshold) {
            this.stoneThreshold = threshold;
            return this;
        }

        /**
         * 🌊 設定水處理規則
         */
        public Builder waterRules(WaterRules rules) {
            this.waterRules = rules;
            return this;
        }

        /**
         * 🎯 設定處理優先級（數字越大優先級越高）
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 🏗️ 建構生態系統配置
         */
        public EcosystemConfig build() {
            return new EcosystemConfig(
                    surfaceBlock,
                    soilBlock,
                    deepSoilBlock,
                    stoneBlock,
                    deepSoilThreshold,
                    stoneThreshold,
                    waterRules,
                    priority
            );
        }
    }
}

