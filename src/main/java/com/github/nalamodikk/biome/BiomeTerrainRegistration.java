// 🌍 生物群系地形註冊類
package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.lib.BiomeTerrainLibAPI;
import com.github.nalamodikk.register.ModBlocks;

/**
 * 🌍 Koniavacraft 生物群系地形註冊
 *
 * 統一管理所有生物群系地形的註冊和初始化
 */
public class BiomeTerrainRegistration {

    /**
     * 🚀 註冊所有生物群系地形
     */
    public static void registerAll() {
        KoniavacraftMod.LOGGER.info("🌍 開始註冊 Koniavacraft 生物群系地形...");

        try {
            // 📝 註冊所有生物群系地形
            registerBiomeTerrains();

            // 🚀 初始化庫系統
            BiomeTerrainLibAPI.initialize();

            KoniavacraftMod.LOGGER.info("✅ Koniavacraft 生物群系地形註冊完成！");

        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("❌ 生物群系地形註冊失敗！", e);
        }
    }

    /**
     * 📝 註冊各種生物群系地形
     */
    private static void registerBiomeTerrains() {
        // 🌱 魔力草原
        registerManaPlains();

        // 🔥 未來可以添加更多生物群系
        // registerVolcanicLands();
        // registerCrystalDesert();
        // registerFrozenWasteland();
    }

    /**
     * 🌱 註冊魔力草原地形
     */
    private static void registerManaPlains() {
        BiomeTerrainLibAPI.addBiome(ModBiomes.MANA_PLAINS)
                .surface(() -> ModBlocks.MANA_GRASS_BLOCK.get())
                .soil(() -> ModBlocks.MANA_SOIL.get())
                .deepSoil(() -> ModBlocks.DEEP_MANA_SOIL.get(), 20)
                .avoidWater()
                .priority(10)
                .register();

        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.debug("🌱 已註冊魔力草原地形");
        }
    }

    // ===============================
    // 🔥 未來生物群系註冊範例
    // ===============================

    /**
     * 🌋 註冊火山灰原地形（範例）
     */
    private static void registerVolcanicLands() {
        // 當你有火山生物群系時，取消註解：
        /*
        BiomeTerrainLibAPI.addVolcanic(
            ModBiomes.VOLCANIC_ASHLANDS,
            () -> ModBlocks.VOLCANIC_ASH.get(),
            () -> ModBlocks.HARDENED_ASH.get()
        );
        
        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.debug("🌋 已註冊火山灰原地形");
        }
        */
    }

    /**
     * 💎 註冊水晶沙漠地形（範例）
     */
    private static void registerCrystalDesert() {
        // 當你有水晶沙漠生物群系時，取消註解：
        /*
        BiomeTerrainLibAPI.addDesert(
            ModBiomes.CRYSTAL_DESERT,
            () -> ModBlocks.CRYSTAL_SAND.get(),
            () -> ModBlocks.CRYSTAL_SANDSTONE.get()
        );
        
        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.debug("💎 已註冊水晶沙漠地形");
        }
        */
    }

    /**
     * ❄️ 註冊冰凍荒地地形（範例）
     */
    private static void registerFrozenWasteland() {
        // 當你有冰凍荒地生物群系時，取消註解：
        /*
        BiomeTerrainLibAPI.addSnowy(
            ModBiomes.FROZEN_WASTELAND,
            () -> ModBlocks.ETERNAL_SNOW.get(),
            () -> ModBlocks.PERMAFROST.get()
        );
        
        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.debug("❄️ 已註冊冰凍荒地地形");
        }
        */
    }

    /**
     * 🎨 註冊複雜自定義地形（範例）
     */
    private static void registerComplexCustomTerrain() {
        // 展示如何使用完整的 API：
        /*
        BiomeTerrainLibAPI.addBiome(ModBiomes.MYSTIC_FOREST)
            .surface(() -> ModBlocks.ENCHANTED_GRASS.get())
            .soil(() -> ModBlocks.FERTILE_SOIL.get())
            .deepSoil(() -> ModBlocks.ANCIENT_SOIL.get(), 15)
            .stone(() -> ModBlocks.MYSTIC_STONE.get(), 5)
            .nearWater() // 只在水源附近生成
            .priority(12)
            .register();
        */
    }

    /**
     * 📊 獲取註冊統計信息
     */
    public static String getRegistrationStats() {
        return BiomeTerrainLibAPI.getStats();
    }

    /**
     * 🧹 清理註冊信息（用於開發測試）
     */
    public static void cleanup() {
        BiomeTerrainLibAPI.cleanup();
    }
}