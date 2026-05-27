package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.worldgen.ModDimensionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModDatapackProvider extends DatapackBuiltinEntriesProvider {

    public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, ModWorldgenRegistries.BUILDER, Set.of(KoniavacraftMod.MOD_ID));
    }

    public static class ModWorldgenRegistries {

        // `mana_plains` 由靜態資源 JSON 提供，datagen 只處理會被 registry builder 收集的 worldgen 項目。
        public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
                .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap)
                .add(Registries.STRUCTURE, ModStructures::bootstrap)
                .add(Registries.STRUCTURE_SET, ModStructureSets::bootstrap)
                .add(Registries.BIOME, ModDimensionProvider::bootstrapBiome)
                .add(Registries.DIMENSION_TYPE, ModDimensionProvider::bootstrapDimensionType)
                .add(Registries.LEVEL_STEM, ModDimensionProvider::bootstrapLevelStem);

        }
}
