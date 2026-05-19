package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.List;

public class ModStructureSets {

    // 常見廢墟：01（高損壞）+ 03（中損壞），間距小，小地形也出現
    public static final ResourceKey<StructureSet> ABANDONED_RUINS_COMMON =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_ruins_common"));

    // 罕見廢墟：02（中損壞）+ 04（最完整），間距大，需要大地形才常見
    public static final ResourceKey<StructureSet> ABANDONED_RUINS_RARE =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_ruins_rare"));

    public static void bootstrap(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);

        ctx.register(ABANDONED_RUINS_COMMON, new StructureSet(
                List.of(
                        new StructureSet.StructureSelectionEntry(
                                structures.getOrThrow(ModStructures.ABANDONED_ALTAR_01), 5),
                        new StructureSet.StructureSelectionEntry(
                                structures.getOrThrow(ModStructures.ABANDONED_ALTAR_03), 3)
                ),
                new RandomSpreadStructurePlacement(24, 6, RandomSpreadType.LINEAR, 123456789)
        ));

        ctx.register(ABANDONED_RUINS_RARE, new StructureSet(
                List.of(
                        new StructureSet.StructureSelectionEntry(
                                structures.getOrThrow(ModStructures.ABANDONED_ALTAR_02), 2),
                        new StructureSet.StructureSelectionEntry(
                                structures.getOrThrow(ModStructures.ABANDONED_ALTAR_04), 1)
                ),
                new RandomSpreadStructurePlacement(48, 16, RandomSpreadType.LINEAR, 987654321)
        ));
    }
}
