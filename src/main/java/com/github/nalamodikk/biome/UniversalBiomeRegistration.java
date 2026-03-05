package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 通用生物群落註冊管理器
 * 負責初始化和註冊所有自訂生物群落
 */
public class UniversalBiomeRegistration {

    /**
     * 初始化所有生物群落註冊
     */
    public static void init() {
        KoniavacraftMod.LOGGER.info("🌍 === 開始初始化 Koniavacraft 生物群落系統 ===");

        try {
            registerAllBiomes();
            KoniavacraftMod.LOGGER.info("✅ 生物群落系統初始化完成！");
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("❌ 生物群落系統初始化失敗！", e);
        }
    }

    /**
     * 註冊所有模組生物群落
     */
    private static void registerAllBiomes() {
        KoniavacraftMod.LOGGER.info("📋 註冊模組生物群落...");


        // 🌱 魔力草原的氣候注入已移至 BiomeTerrainRegistration.registerManaPlains()
        // 使用 ParameterPointListBuilder 精確定義，避免與原版 biome 過度重疊

        // 🌲 未來的生物群落示例：

        // 水晶森林 (如果你有的話)
        // UniversalBiomeInjector.registerBiome(
        //         ModBiomes.CRYSTAL_FOREST,
        //         UniversalBiomeInjector.ClimatePresets.MYSTICAL_FOREST,
        //         4,
        //         "閃爍著水晶光芒的魔法森林"
        // );

        // 虛空之地 (如果你有的話)
        // UniversalBiomeInjector.registerBiome(
        //         ModBiomes.VOIDLANDS,
        //         UniversalBiomeInjector.ClimateConfig.builder()
        //                 .temperature(-1.0F, -0.5F)
        //                 .humidity(-0.8F, -0.3F)
        //                 .continentalness(0.5F, 1.0F)
        //                 .erosion(0.3F, 0.8F)
        //                 .depth(0.4F, 1.0F)
        //                 .weirdness(0.7F, 1.2F)
        //                 .build(),
        //         2, // 稀有
        //         "虛無縹緲的異次元空間"
        // );

        KoniavacraftMod.LOGGER.info("📝 生物群落註冊完成！");
    }

    /**
     * 快速添加新的生物群落 (供其他地方調用)
     */
    public static void addBiome(ResourceKey<Biome> biome, UniversalBiomeInjector.ClimateConfig climate, int weight, String description) {
        UniversalBiomeInjector.registerBiome(biome, climate, weight, description);
        KoniavacraftMod.LOGGER.info("🆕 動態添加生物群落: {}", biome.location());
    }

    /**
     * 使用預設氣候添加新生物群落
     */
    public static void addTemperate(ResourceKey<Biome> biome, String description) {
        addBiome(biome, UniversalBiomeInjector.ClimatePresets.TEMPERATE_PLAINS, 5, description);
    }

    /**
     * 添加稀有生物群落
     */
    public static void addRare(ResourceKey<Biome> biome, UniversalBiomeInjector.ClimateConfig climate, String description) {
        addBiome(biome, climate, 2, description);
    }

    /**
     * 獲取註冊統計信息
     */
    public static void printRegistrationStats() {
        var biomes = UniversalBiomeInjector.getRegisteredBiomes();
        KoniavacraftMod.LOGGER.info("📊 生物群落註冊統計:");
        KoniavacraftMod.LOGGER.info("   總數: {}", biomes.size());

        for (var entry : biomes) {
            KoniavacraftMod.LOGGER.info("   🌍 {} (權重: {}) - {}",
                    entry.biome.location().getPath(),
                    entry.weight,
                    entry.description);
        }
    }
}