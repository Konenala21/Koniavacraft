// 🌍 生物群系地形註冊類
package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.lib.BiomeTerrainLibAPI;
import com.github.nalamodikk.biome.lib.SurfaceRuleRegistry;
import com.github.nalamodikk.biome.region.BiomeInjectionEntry;
import com.github.nalamodikk.biome.region.BiomeRegionManager;
import com.github.nalamodikk.biome.region.ParameterPointListBuilder;
import com.github.nalamodikk.biome.region.PlacementMode;
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
        KoniavacraftMod.LOGGER.info("Registering Koniavacraft biome terrain definitions.");

        try {
            // 全局參數：vanillaWeight=10 → 魔力草原覆蓋率 2/(10+2) ≈ 17%
            // zoomCount=4 → patch 大小 ≈ 1024 格
            BiomeRegionManager.setVanillaWeight(10);
            BiomeRegionManager.setZoomCount(4);

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

            KoniavacraftMod.LOGGER.info("Biome terrain registration completed.");

        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("Biome terrain registration failed.", e);
        }
    }

    /**
     * 📝 註冊各種生物群系地形
     */
    private static void registerBiomeTerrains() {
        registerManaPlains();
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
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_plains_region"),
                2,
                PlacementMode.SMOOTH_NOISE);

        // Climate 設計目標：出現在草原型地形（乾燥到中性濕度 = Plains/Sunflower Plains），
        // 不出現在森林型地形（WET humidity = 森林），高侵蝕 = 平坦地形
        List<BiomeInjectionEntry> entries = BiomeInjectionEntry.fromBuilder(
                ModBiomes.MANA_PLAINS,
                ParameterPointListBuilder.create()
                        .temperature(VanillaClimateBands.Temperature.NEUTRAL, VanillaClimateBands.Temperature.WARM)
                        .humidity(VanillaClimateBands.Humidity.DRY)
                        .humidity(VanillaClimateBands.Humidity.NEUTRAL)
                        .continentalness(VanillaClimateBands.Continentalness.NEAR_INLAND)
                        .continentalness(VanillaClimateBands.Continentalness.MID_INLAND)
                        .erosion(VanillaClimateBands.Erosion.EROSION_4)
                        .erosion(VanillaClimateBands.Erosion.EROSION_5)
                        .erosion(VanillaClimateBands.Erosion.EROSION_6)
                        .depth(VanillaClimateBands.Depth.SURFACE)
                        // VALLEY omitted — weirdness [-0.05, 0.05] is where rivers appear
                        .weirdness(VanillaClimateBands.Weirdness.MID_SLICE_NORMAL_ASCENDING)
                        .weirdness(VanillaClimateBands.Weirdness.MID_SLICE_NORMAL_DESCENDING),
                7, "魔力草原（精確 climate 定義）", 50, KoniavacraftMod.MOD_ID
        );
        entries.forEach(defaultRegion::registerEntry);

        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.debug("Registered mana plains terrain with {} climate points.", entries.size());
        }
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
