package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.worldgen.structure.AbandonedAltarStructure;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

import java.util.Map;

public class ModStructures {

    public static final ResourceKey<Structure> ABANDONED_ALTAR_01 =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_altar_01"));

    public static final TagKey<Biome> HAS_ABANDONED_RUINS =
            TagKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "has_structure/abandoned_ruins"));

    public static void bootstrap(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);

        ctx.register(ABANDONED_ALTAR_01, new AbandonedAltarStructure(
                new Structure.StructureSettings(
                        biomes.getOrThrow(HAS_ABANDONED_RUINS),
                        Map.of(),
                        GenerationStep.Decoration.SURFACE_STRUCTURES,
                        TerrainAdjustment.NONE
                )
        ));
    }
}
