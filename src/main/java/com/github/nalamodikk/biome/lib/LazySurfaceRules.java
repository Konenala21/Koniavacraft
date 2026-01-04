// 🔄 懶加載 Surface Rules 系統 - 庫版本
package com.github.nalamodikk.biome.lib;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 🔄 懶加載 Surface Rules - 庫版本
 *
 * 核心功能：
 * 1. 檢查生物群系和方塊是否存在
 * 2. 安全創建 Surface Rules
 * 3. 正確的水檢查邏輯
 * 4. 緩存機制避免重複創建
 */
public class LazySurfaceRules {

    public static final Logger LOGGER = LogUtils.getLogger();

    // 🗃️ 規則供應商註冊表
    private static final Map<ResourceKey<Biome>, Supplier<SurfaceRules.RuleSource>> RULE_SUPPLIERS = new HashMap<>();

    // 📦 已創建的規則緩存
    private static final Map<ResourceKey<Biome>, SurfaceRules.RuleSource> CACHED_RULES = new HashMap<>();

    // 🔇 日誌控制
    private static boolean LOGGED_SUCCESS = false;

    /**
     * 🎯 創建所有可用的規則 - 主要入口點
     */
    public static SurfaceRules.RuleSource createAllAvailableRules() {
        java.util.List<SurfaceRules.RuleSource> validRules = new java.util.ArrayList<>();

        for (ResourceKey<Biome> biome : RULE_SUPPLIERS.keySet()) {
            SurfaceRules.RuleSource rule = getOrCreateRule(biome);
            if (rule != null) {
                validRules.add(rule);
            }
        }

        if (validRules.isEmpty()) {
            if (!LOGGED_SUCCESS) {
                LOGGER.warn("⚠️ 沒有可用的 Surface Rules，使用原版規則");
            }
            return null; // 讓原版規則接管
        }

        if (!LOGGED_SUCCESS) {
            LOGGER.info("✅ 成功創建 " + validRules.size() + " 個 Surface Rules");
            LOGGED_SUCCESS = true;
        }

        return SurfaceRules.sequence(validRules.toArray(new SurfaceRules.RuleSource[0]));
    }

    /**
     * ⚡ 獲取或創建規則（懶加載）
     */
    private static SurfaceRules.RuleSource getOrCreateRule(ResourceKey<Biome> biome) {
        // 檢查緩存
        if (CACHED_RULES.containsKey(biome)) {
            return CACHED_RULES.get(biome);
        }

        Supplier<SurfaceRules.RuleSource> supplier = RULE_SUPPLIERS.get(biome);
        if (supplier == null) {
            return null;
        }

        try {
            // 懶加載創建規則
            SurfaceRules.RuleSource rule = supplier.get();
            if (rule != null) {
                CACHED_RULES.put(biome, rule);
                LOGGER.debug("✨ 成功創建規則: " + biome.location());
                return rule;
            }
        } catch (Exception e) {
            LOGGER.warn("❌ 創建規則失敗: " + biome.location() + " - " + e.getMessage());
        }

        return null;
    }

    /**
     * 🔍 檢查方塊是否存在
     */
    private static boolean doesBlockExist(Supplier<Block> blockSupplier) {
        try {
            Block block = blockSupplier.get();
            return block != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 📝 註冊懶加載規則供應商
     */
    public static void registerRuleSupplier(ResourceKey<Biome> biome, Supplier<SurfaceRules.RuleSource> supplier) {
        RULE_SUPPLIERS.put(biome, supplier);
        LOGGER.debug("📝 註冊規則供應商: " + biome.location());
    }

    /**
     * 🧹 清理緩存
     */
    public static void clearCache() {
        CACHED_RULES.clear();
        LOGGED_SUCCESS = false;
        LOGGER.debug("🧹 清理 Surface Rules 緩存");
    }

    /**
     * 📊 獲取註冊統計
     */
    public static int getRegisteredRuleCount() {
        return RULE_SUPPLIERS.size();
    }

    /**
     * 📋 獲取所有已註冊的生物群系
     */
    public static java.util.Set<ResourceKey<Biome>> getRegisteredBiomes() {
        return new java.util.HashSet<>(RULE_SUPPLIERS.keySet());
    }

    // ===============================
    // 🌱 通用規則創建工具
    // ===============================

    /**
     * 🛠️ 創建基本的生物群系規則供應商
     *
     * 範例用法：
     * ```java
     * registerRuleSupplier(
     *     MyBiomes.CUSTOM_PLAINS,
     *     createBasicBiomeRules(
     *         MyBiomes.CUSTOM_PLAINS,
     *         () -> MyBlocks.CUSTOM_GRASS.get(),
     *         () -> MyBlocks.CUSTOM_SOIL.get(),
     *         () -> MyBlocks.DEEP_CUSTOM_SOIL.get(),
     *         20 // 深層閾值
     *     )
     * );
     * ```
     */
    public static Supplier<SurfaceRules.RuleSource> createBasicBiomeRules(
            ResourceKey<Biome> biome,
            Supplier<Block> surfaceBlock,
            Supplier<Block> soilBlock,
            Supplier<Block> deepSoilBlock,
            int deepThreshold) {

        return () -> {
            // 檢查方塊是否存在
            if (!doesBlockExist(surfaceBlock) || !doesBlockExist(soilBlock)) {
                LOGGER.warn("⚠️ 生物群系所需方塊不存在，跳過: " + biome.location());
                return null;
            }

            try {
                return SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(biome),
                        SurfaceRules.sequence(
                                // 🌿 地表規則
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.waterBlockCheck(-1, 0), // 上方沒有水
                                                SurfaceRules.state(surfaceBlock.get().defaultBlockState())
                                        )
                                ),
                                // 🌱 地下規則
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.waterBlockCheck(0, 0), // 當前位置沒有水
                                                createUndergroundRules(soilBlock, deepSoilBlock, deepThreshold)
                                        )
                                )
                        )
                );
            } catch (Exception e) {
                LOGGER.warn("❌ 創建基本生物群系規則失敗: " + biome.location() + " - " + e.getMessage());
                return null;
            }
        };
    }

    /**
     * 🌱 創建地下規則
     */
    private static SurfaceRules.RuleSource createUndergroundRules(
            Supplier<Block> soilBlock,
            Supplier<Block> deepSoilBlock,
            int deepThreshold) {

        if (deepSoilBlock != null && doesBlockExist(deepSoilBlock)) {
            return SurfaceRules.sequence(
                    // 🏔️ 淺層區域：普通土壤
                    SurfaceRules.ifTrue(
                            SurfaceRules.yBlockCheck(
                                    net.minecraft.world.level.levelgen.VerticalAnchor.absolute(deepThreshold),
                                    1 // Y > threshold
                            ),
                            SurfaceRules.state(soilBlock.get().defaultBlockState())
                    ),
                    // ⚡ 深層區域：深層土壤
                    SurfaceRules.state(deepSoilBlock.get().defaultBlockState())
            );
        } else {
            // 只有普通土壤
            return SurfaceRules.state(soilBlock.get().defaultBlockState());
        }
    }

    /**
     * 📝 註冊所有預定義的規則供應商
     *
     * 注意：庫版本不包含具體實現，需要用戶自己註冊
     */
    public static void registerAllSuppliers() {
        LOGGER.info("🔄 註冊懶加載規則供應商...");

        // 庫版本不自動註冊任何規則，等待用戶調用API註冊

        LOGGER.info("📝 已準備規則供應商系統，等待用戶註冊規則");
    }
}
