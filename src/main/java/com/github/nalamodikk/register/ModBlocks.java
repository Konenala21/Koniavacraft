package com.github.nalamodikk.register;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ConduitTier;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlock;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlock;
import com.github.nalamodikk.common.block.blockentity.ore_grinder.OreGrinderBlock;
import com.github.nalamodikk.common.block.normal.DeepManaSoilBlock;
import com.github.nalamodikk.common.block.normal.ManaBloomBlock;
import com.github.nalamodikk.common.block.normal.ManaGrassBlock;
import com.github.nalamodikk.common.block.normal.ManaSoilBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(KoniavacraftMod.MOD_ID);


    public static final DeferredBlock<Block> MANA_BLOCK =
            registerBlock("mana_block", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> MANA_CRAFTING_TABLE_BLOCK =
            registerBlock("mana_crafting_table", () -> new ManaCraftingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_WOOD)));

    public static final DeferredBlock<Block> MANA_GENERATOR =
            registerBlock("mana_generator", () -> new ManaGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    // 🆕 三種等級的導管
    public static final DeferredBlock<Block> BASIC_ARCANE_CONDUIT =
            registerBlock("basic_arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(1.5f)
                            .noOcclusion()
                            .lightLevel(state -> 5), // 基礎等級 - 較弱的光
                    ConduitTier.BASIC
            ));

    public static final DeferredBlock<Block> ADVANCED_ARCANE_CONDUIT =
            registerBlock("advanced_arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(2.0f)
                            .noOcclusion()
                            .lightLevel(state -> 7), // 進階等級 - 中等光
                    ConduitTier.ADVANCED
            ));

    public static final DeferredBlock<Block> ELITE_ARCANE_CONDUIT =
            registerBlock("elite_arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(2.5f)
                            .noOcclusion()
                            .lightLevel(state -> 9), // 精英等級 - 最強的光
                    ConduitTier.ELITE
            ));

    // ⚠️ 已棄用：保留舊的 arcane_conduit 以向後兼容
    @Deprecated
    public static final DeferredBlock<Block> ARCANE_CONDUIT =
            registerBlock("arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(1.5f)
                            .noOcclusion()
                            .lightLevel(state -> 7), // 發光等級
                    ConduitTier.BASIC // 預設為基礎等級
            ));

    public static final DeferredBlock<Block> SOLAR_MANA_COLLECTOR =
            registerBlock("solar_mana_collector", () -> new SolarManaCollectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    // === 魔力生態系統 - 地表植物 ===
    public static final DeferredBlock<ManaBloomBlock> MANA_BLOOM =
            registerBlock("mana_bloom", ManaBloomBlock::new);


    public static final DeferredBlock<Block> MAGIC_ORE =
            registerBlock("magic_ore", () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                                .strength(2f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DEEPSLATE_MAGIC_ORE =
            registerBlock("deepslate_magic_ore", () -> new DropExperienceBlock(UniformInt.of(3, 8), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                .strength(4f).requiresCorrectToolForDrops()));

    // 🌍 魔力生態系統 - 土壤層
    public static final DeferredBlock<Block> MANA_SOIL =
            registerBlock("mana_soil", () -> new ManaSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)  // 略微不同的音效
                    .lightLevel((state) -> 2)  // 微弱發光 (2/15)
            ));

    public static final DeferredBlock<Block> DEEP_MANA_SOIL =
            registerBlock("deep_mana_soil", () -> new DeepManaSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .lightLevel((state) -> 1)  // 更微弱的發光
            ));

    public static final DeferredBlock<Block> MANA_GRASS_BLOCK =
            registerBlock("mana_grass_block", () -> new ManaGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .lightLevel((state) -> 3)  // 比純土壤亮一點
            ));

    // === 🔮 新增：魔力注入機 ===
    public static final DeferredBlock<ManaInfuserBlock> MANA_INFUSER = registerBlock("mana_infuser",
            () -> new ManaInfuserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).lightLevel(state ->
                            state.getValue(ManaInfuserBlock.WORKING) ? 7 : 0))); // 工作時發光

    // === ⚙️ 新增：礦石粉碎機 ===
    public static final DeferredBlock<OreGrinderBlock> ORE_GRINDER = registerBlock("ore_grinder",
            () -> new OreGrinderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).lightLevel(state ->
                            state.getValue(OreGrinderBlock.WORKING) ? 6 : 0))); // 工作時發光


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }


    private static <T extends Block> DeferredBlock<T> registerBlockWithItem(String name, Supplier<T> block, Function<T, BlockItem> itemFactory) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(toReturn.get()));
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
