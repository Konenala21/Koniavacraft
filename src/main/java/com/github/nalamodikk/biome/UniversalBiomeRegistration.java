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


        // 🌱 魔力草原 - 使用地面友好的配置
        UniversalBiomeInjector.registerBiome(
                ModBiomes.MANA_PLAINS,
                // 🔧 不要用 TEMPERATE_PLAINS，手動配置地面高度
                UniversalBiomeInjector.ClimateConfig.builder()
                        .temperature(0.1F, 0.8F)     // 溫帶
                        .humidity(0.2F, 0.7F)        // 中等濕度
                        .continentalness(-0.19F, 0.4F) // 內陸
                        .erosion(-0.22F, 0.55F)      // 低到中等侵蝕
                        .depth(0.0F, 0.0F)           // 🌟 關鍵：地面到小丘陵高度
                        .weirdness(-0.56F, 0.56F)    // 正常怪異度
                        .build(),
                7, // 較高權重，容易找到
                "充滿魔力能量的神秘草原"
        );

        KoniavacraftMod.LOGGER.info("📝 生物群落註冊完成！");

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