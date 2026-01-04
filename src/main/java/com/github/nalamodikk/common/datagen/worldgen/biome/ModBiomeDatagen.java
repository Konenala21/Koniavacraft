package com.github.nalamodikk.common.datagen.worldgen.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.ModBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Koniavacraft 生物群落數據生成器
 */
public class ModBiomeDatagen {

    /**
     * 生成所有模組生物群落數據
     */
    public static void bootstrap(BootstrapContext<Biome> context) {
        KoniavacraftMod.LOGGER.info("開始生成 Koniavacraft 生物群落數據...");

        // 獲取必要的註冊表
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // 註冊魔力草原
        context.register(ModBiomes.MANA_PLAINS, createManaPlains(placedFeatures, carvers));

        KoniavacraftMod.LOGGER.info("Koniavacraft 生物群落數據生成完成！");
    }

    /**
     * 創建魔力草原生物群落
     */
    private static Biome createManaPlains(HolderGetter<PlacedFeature> placedFeatures,
                                          HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        // 使用帶參數的 Builder，這樣可以使用 BiomeDefaultFeatures
        return ModBiomes.createManaPlains(placedFeatures, carvers);
    }
}