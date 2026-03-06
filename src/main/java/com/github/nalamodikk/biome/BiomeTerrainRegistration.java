// 🌍 生物群系地形註冊類
package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.lib.BiomeTerrainLibAPI;
import com.github.nalamodikk.biome.lib.SurfaceRuleRegistry;
import com.github.nalamodikk.biome.region.BiomeInjectionEntry;
import com.github.nalamodikk.biome.region.BiomeRegionManager;
import com.github.nalamodikk.biome.region.ParameterPointListBuilder;
import com.github.nalamodikk.biome.region.SimpleBiomeRegion;
import com.github.nalamodikk.biome.region.VanillaClimateBands;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;

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
            // 全局參數：vanillaWeight=10 保留空間給未來自訂 biome，
            // 當所有 region 總 weight 累積到 10 時全局覆蓋率約 50%。
            // 目前魔力草原單獨 weight=2 → 2/(10+2) ≈ 17%。
            BiomeRegionManager.setVanillaWeight(10);
            BiomeRegionManager.setZoomCount(4); // patch 大小 ≈ 1024 格

            // 📝 註冊所有生物群系地形
            registerBiomeTerrains();

            // 🚀 初始化庫系統
            BiomeTerrainLibAPI.initialize();
            SurfaceRuleRegistry.register(
                    SurfaceRuleRegistry.RuleCategory.OVERWORLD,
                    SurfaceRuleRegistry.RuleStage.BEFORE_VANILLA,
                    KoniavacraftMod.MOD_ID,
                    100,
                    BiomeTerrainLibAPI::getAllRules
            );

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
        // 地表規則：mana_grass_block / mana_soil / deep_mana_soil
        BiomeTerrainLibAPI.addBiome(ModBiomes.MANA_PLAINS)
                .surface(() -> ModBlocks.MANA_GRASS_BLOCK.get())
                .soil(() -> ModBlocks.MANA_SOIL.get())
                .deepSoil(() -> ModBlocks.DEEP_MANA_SOIL.get(), 20)
                .avoidWater()
                .priority(10)
                .register();

        // 氣候注入：用 ParameterPointListBuilder 精確定義 climate 空間
        // （JSON datapack 的 mana_plains.json override 可覆蓋個別 entry 的 weight/priority，
        //  但程式碼定義的多 entry 架構由此建立）
        SimpleBiomeRegion defaultRegion = BiomeRegionManager.getOrCreateRegion(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_plains_region"), 2);

        List<BiomeInjectionEntry> entries = BiomeInjectionEntry.fromBuilder(
                ModBiomes.MANA_PLAINS,
                ParameterPointListBuilder.create()
                        .temperature(VanillaClimateBands.Temperature.NEUTRAL, VanillaClimateBands.Temperature.WARM)
                        .humidity(VanillaClimateBands.Humidity.WET)
                        .continentalness(VanillaClimateBands.Continentalness.NEAR_INLAND)
                        .continentalness(VanillaClimateBands.Continentalness.MID_INLAND)
                        .erosion(VanillaClimateBands.Erosion.EROSION_3)
                        .erosion(VanillaClimateBands.Erosion.EROSION_4)
                        .depth(VanillaClimateBands.Depth.SURFACE)
                        .weirdness(VanillaClimateBands.Weirdness.VALLEY)
                        .weirdness(VanillaClimateBands.Weirdness.MID_SLICE_NORMAL_ASCENDING)
                        .weirdness(VanillaClimateBands.Weirdness.MID_SLICE_NORMAL_DESCENDING),
                7, "魔力草原（精確 climate 定義）", 50, KoniavacraftMod.MOD_ID
        );
        entries.forEach(defaultRegion::registerEntry);

        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.debug("🌱 已註冊魔力草原地形（{} 個 climate points）", entries.size());
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
     * Initialise the Zoom-Layer region area when the Overworld is loaded.
     * Registered with {@code NeoForge.EVENT_BUS} from {@link KoniavacraftMod}.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        BiomeRegionManager.initForWorld(level.getSeed());
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
