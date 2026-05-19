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

    public static final TagKey<Biome> HAS_ABANDONED_RUINS =
            TagKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "has_structure/abandoned_ruins"));

    // 01: 最破損，小地形高機率
    public static final ResourceKey<Structure> ABANDONED_ALTAR_01 = key("abandoned_altar_01");
    // 02: 中等損壞，大地形出現
    public static final ResourceKey<Structure> ABANDONED_ALTAR_02 = key("abandoned_altar_02");
    // 03: 中等損壞（另一變體），小地形偶爾出現
    public static final ResourceKey<Structure> ABANDONED_ALTAR_03 = key("abandoned_altar_03");
    // 04: 最完整但損壞，大地形低機率
    public static final ResourceKey<Structure> ABANDONED_ALTAR_04 = key("abandoned_altar_04");

    private static ResourceKey<Structure> key(String name) {
        return ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, name));
    }

    public static void bootstrap(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        var biomeSet = biomes.getOrThrow(HAS_ABANDONED_RUINS);
        var settings = new Structure.StructureSettings(biomeSet, Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);

        ctx.register(ABANDONED_ALTAR_01, new AbandonedAltarStructure(settings,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_altar_01")));
        ctx.register(ABANDONED_ALTAR_02, new AbandonedAltarStructure(settings,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_altar_02")));
        ctx.register(ABANDONED_ALTAR_03, new AbandonedAltarStructure(settings,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_altar_03")));
        ctx.register(ABANDONED_ALTAR_04, new AbandonedAltarStructure(settings,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_altar_04")));
    }
}
