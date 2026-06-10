package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, KoniavacraftMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
//                .add(ModBlocks.BISMUTH_BLOCK.get())
//                .add(ModBlocks.BISMUTH_ORE.get())
//                .add(ModBlocks.BISMUTH_LAMP.get())
                .add(ModBlocks.MAGIC_ORE.get())
                .add(ModBlocks.DEEPSLATE_MAGIC_ORE.get())
                .add(ModBlocks.MANA_BLOCK.get())
                .add(ModBlocks.BASIC_ARCANE_CONDUIT.get())
                .add(ModBlocks.ADVANCED_ARCANE_CONDUIT.get())
                .add(ModBlocks.ELITE_ARCANE_CONDUIT.get())
                .add(ModBlocks.MANA_GENERATOR.get())
                .add(ModBlocks.SOLAR_MANA_COLLECTOR.get())
                .add(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get())
                .add(ModBlocks.MANA_INFUSER.get())
                .add(ModBlocks.MANA_GRINDER.get())
                .add(ModBlocks.ASPECT_ALTAR.get())
                .add(ModBlocks.ALTAR_PILLAR.get())
                .add(ModBlocks.ASPECT_PEDESTAL.get())
                .add(ModBlocks.MANA_DEPLOYER.get())
                .add(ModBlocks.RESONANCE_RING.get())
                .add(ModBlocks.MANA_CHARGER.get());





        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.DEEPSLATE_MAGIC_ORE.get());

        tag(Tags.Blocks.ORES_IN_GROUND_STONE)
                .add(ModBlocks.MAGIC_ORE.get());

        tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
                .add(ModBlocks.DEEPSLATE_MAGIC_ORE.get());

        tag(Tags.Blocks.ORES)
                .add(ModBlocks.MAGIC_ORE.get())
                .add(ModBlocks.DEEPSLATE_MAGIC_ORE.get());

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.MAGIC_ORE.get());

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.CRACKED_MANA_BRICKS.get())
                .add(ModBlocks.MOSSY_MANA_BRICKS.get())
                .add(ModBlocks.RUINED_MANA_PEDESTAL.get())
                .add(ModBlocks.SHIP_CORE.get())
                .add(ModBlocks.SHIP_ASSEMBLY_PAD.get())
                .add(ModBlocks.SHIP_ASSEMBLY_BASE.get())
                .add(ModBlocks.SHIP_ASSEMBLY_GANTRY.get())
                .add(ModBlocks.SHIP_SEAT.get())
                .add(ModBlocks.MANA_ENGINE.get())
                .add(ModBlocks.MANA_FUEL_TANK.get());

        this.tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(ModBlocks.MANA_GRASS_BLOCK.get())

                .add(ModBlocks.MANA_SOIL.get())

                .add(ModBlocks.DEEP_MANA_SOIL.get());

        this.tag(BlockTags.DIRT)
                .add(ModBlocks.MANA_GRASS_BLOCK.get())

                .add(ModBlocks.MANA_SOIL.get())
                .add(ModBlocks.DEEP_MANA_SOIL.get());

        tag(BlockTags.SMALL_FLOWERS)
                .add(ModBlocks.MANA_BLOOM.get());

        tag(BlockTags.FLOWERS)
                .add(ModBlocks.MANA_BLOOM.get());

//        tag(BlockTags.NEEDS_DIAMOND_TOOL)
//                .add(ModBlocks.BISMUTH_LAMP.get());
//
//        tag(BlockTags.FENCES).add(ModBlocks.BISMUTH_FENCE.get());
//        tag(BlockTags.FENCE_GATES).add(ModBlocks.BISMUTH_FENCE_GATE.get());
//        tag(BlockTags.WALLS).add(ModBlocks.BISMUTH_WALL.get());

//        tag(ModTags.Blocks.NEEDS_BISMUTH_TOOL)
//                .add(ModBlocks.BISMUTH_LAMP.get())
//                .addTag(BlockTags.NEEDS_IRON_TOOL);
//
//        tag(ModTags.Blocks.INCORRECT_FOR_BISMUTH_TOOL)
//                .addTag(BlockTags.INCORRECT_FOR_IRON_TOOL)
//                .remove(ModTags.Blocks.NEEDS_BISMUTH_TOOL);

//        this.tag(BlockTags.LOGS_THAT_BURN)
//                .add(ModBlocks.BLOODWOOD_LOG.get())
//                .add(ModBlocks.BLOODWOOD_WOOD.get())
//                .add(ModBlocks.STRIPPED_BLOODWOOD_LOG.get())
//                .add(ModBlocks.STRIPPED_BLOODWOOD_WOOD.get());
    }
}
