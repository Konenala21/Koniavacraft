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

    public static final ResourceKey<StructureSet> ABANDONED_RUINS =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_ruins"));

    public static void bootstrap(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);

        ctx.register(ABANDONED_RUINS, new StructureSet(
                List.of(new StructureSet.StructureSelectionEntry(
                        structures.getOrThrow(ModStructures.ABANDONED_ALTAR_01), 1
                )),
                new RandomSpreadStructurePlacement(
                        32,   // 平均間距（chunk 數）
                        8,    // 最小間距
                        RandomSpreadType.LINEAR,
                        987654321
                )
        ));
    }
}
