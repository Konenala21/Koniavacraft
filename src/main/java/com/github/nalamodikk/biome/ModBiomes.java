package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * 模組生物群落 ResourceKey 定義。
 * 生物群落屬性（effects、spawns、features）定義於：
 * src/main/resources/data/koniava/worldgen/biome/mana_plains.json
 * 生物群落注入邏輯（climate 參數、地形規則）定義於：
 * BiomeTerrainRegistration、MultiNoiseBiomeSourceMixin
 */
public class ModBiomes {

    public static final ResourceKey<Biome> MANA_PLAINS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_plains")
    );
}