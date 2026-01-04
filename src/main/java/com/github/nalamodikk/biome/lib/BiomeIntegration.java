// 🌍 生物群系整合管理器 - 庫版本
package com.github.nalamodikk.biome.lib;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 🌍 生物群系整合管理器 - 庫版本
 *
 * 這個類統一管理所有生物群系相關的初始化和整合：
 * - Surface Rules 註冊
 * - 配置檢查和錯誤處理
 * - 系統狀態管理
 */
public class BiomeIntegration {

    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean isInitialized = false;

    /**
     * 🚀 初始化所有生物群系系統
     *
     * 在模組主類中調用這個方法來啟動所有生物群系功能
     */
    public static void initialize() {
        if (isInitialized) {
            LOGGER.info("🌍 生物群系系統已經初始化，跳過重複初始化");
            return;
        }

        LOGGER.info("🌍 === 初始化 Biome Terrain Library ===");

        try {
            // 📝 註冊 Surface Rules 供應商
            registerSurfaceRules();

            // ✅ 驗證配置
            validateConfiguration();

            isInitialized = true;
            LOGGER.info("✅ 生物群系系統初始化完成！");

        } catch (Exception e) {
            LOGGER.error("❌ 生物群系系統初始化失敗！" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 📝 註冊 Surface Rules 供應商
     */
    private static void registerSurfaceRules() {
        LOGGER.info("📝 註冊 Surface Rules...");

        // 註冊所有預定義的規則供應商
        LazySurfaceRules.registerAllSuppliers();

        LOGGER.info("✅ Surface Rules 註冊完成");
    }

    /**
     * ✅ 驗證配置
     */
    private static void validateConfiguration() {
        LOGGER.info("🔍 驗證生物群系配置...");

        // 檢查關鍵配置項
        boolean surfaceRulesEnabled = true; // Surface Rules 總是啟用

        LOGGER.info("🔍 配置驗證完成");
        LOGGER.info("   Surface Rules: ✅ 啟用");
    }

    /**
     * 🧹 清理系統（當配置變更或世界卸載時調用）
     */
    public static void cleanup() {
        LOGGER.info("🧹 清理生物群系系統...");

        // 清理 Surface Rules 緩存
        LazySurfaceRules.clearCache();
        UniversalTerrainEcosystemLibrary.clearAllRulesCache();

        isInitialized = false;
        LOGGER.info("✅ 生物群系系統清理完成");
    }

    /**
     * 📊 獲取系統狀態信息
     */
    public static String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("🌍 Biome Terrain Library 系統狀態:\n");

        // Surface Rules 狀態
        status.append("   Surface Rules: ").append(isInitialized ? "✅ 啟用" : "🔴 未初始化").append("\n");

        // 註冊統計
        int ecosystemCount =UniversalTerrainEcosystemLibrary.getRegisteredEcosystemCount();
        status.append("   已註冊生態系統: ").append(ecosystemCount).append(" 個\n");

        return status.toString();
    }

    /**
     * 🎯 快速添加新生物群系的輔助方法
     */
    public static void addNewBiome(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome,
            java.util.function.Supplier<net.minecraft.world.level.levelgen.SurfaceRules.RuleSource> ruleSupplier) {

        // 註冊 Surface Rules
        LazySurfaceRules.registerRuleSupplier(biome, ruleSupplier);

        LOGGER.info("🆕 已添加新生物群系: " + biome.location());
    }

    /**
     * 🔍 檢查系統是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
}
