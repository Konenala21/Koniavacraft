package com.github.nalamodikk.register;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ConduitTier;
import com.github.nalamodikk.common.block.ResearchTableBlock;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlock;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlock;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderBlock;
import com.github.nalamodikk.common.block.blockentity.altar.AltarPillarBlock;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlock;
import com.github.nalamodikk.common.block.blockentity.altar.AspectPedestalBlock;
import com.github.nalamodikk.common.block.blockentity.altar.ResonanceRingBlock;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlock;
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

    public static final DeferredBlock<Block> RESEARCH_TABLE =
            registerBlock("research_table", () -> new ResearchTableBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_WOOD).strength(2.5f).noOcclusion()));


    public static final DeferredBlock<Block> MANA_GENERATOR =
            registerBlock("mana_generator", () -> new ManaGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

    // 🆕 三種等級的導管
    public static final DeferredBlock<Block> BASIC_ARCANE_CONDUIT =
            registerBlock("basic_arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(1.5f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .lightLevel(state -> 5), // 基礎等級 - 較弱的光
                    ConduitTier.BASIC
            ));

    public static final DeferredBlock<Block> ADVANCED_ARCANE_CONDUIT =
            registerBlock("advanced_arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(2.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .lightLevel(state -> 7), // 進階等級 - 中等光
                    ConduitTier.ADVANCED
            ));

    public static final DeferredBlock<Block> ELITE_ARCANE_CONDUIT =
            registerBlock("elite_arcane_conduit", () -> new ArcaneConduitBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                            .strength(2.5f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .lightLevel(state -> 9), // 精英等級 - 最強的光
                    ConduitTier.ELITE
            ));

    // ⚠️ 已棄用：保留舊的 arcane_conduit 以向後兼容

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

    // === 🏚️ 廢墟裝飾方塊 ===
    public static final DeferredBlock<Block> CRACKED_MANA_BRICKS =
            registerBlock("cracked_mana_bricks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS)
                    .strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MOSSY_MANA_BRICKS =
            registerBlock("mossy_mana_bricks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS)
                    .strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> RUINED_MANA_PEDESTAL =
            registerBlock("ruined_mana_pedestal", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)
                    .strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<Block> MANA_GRASS_BLOCK =
            registerBlock("mana_grass_block", () -> new ManaGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .lightLevel((state) -> 3)  // 比純土壤亮一點
            ));

    // === 🔮 新增：魔力注入機 ===
    public static final DeferredBlock<ManaInfuserBlock> MANA_INFUSER = registerBlock("mana_infuser",
            () -> new ManaInfuserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state ->
                            state.getValue(ManaInfuserBlock.WORKING) ? 7 : 0))); // 工作時發光

    // === ⚙️ 新增：魔力粉碎機 ===
    public static final DeferredBlock<ManaGrinderBlock> MANA_GRINDER = registerBlock("mana_grinder",
            () -> new ManaGrinderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state ->
                            state.getValue(ManaGrinderBlock.WORKING) ? 6 : 0))); // 工作時發光

    // === 🔮 多方塊：本源聚陣 ===
    // 核心（controller，放在底座上方）
    public static final DeferredBlock<AspectAltarBlock> ASPECT_ALTAR = registerBlock("aspect_altar",
            () -> new AspectAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(state -> 6).noOcclusion()));

    // 祭壇柱（成形後替換角落魔力方塊的視覺方塊，挖掉掉落 mana_block）
    public static final DeferredBlock<AltarPillarBlock> ALTAR_PILLAR = registerBlock("altar_pillar",
            () -> new AltarPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .noOcclusion()));

    // 底座柱（放材料用，中心與周圍皆可放）
    public static final DeferredBlock<AspectPedestalBlock> ASPECT_PEDESTAL = registerBlock("aspect_pedestal",
            () -> new AspectPedestalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(2.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .noOcclusion()));

    // 共鳴環（戴森環升級結構方塊，放在環狀位置觸發祭壇升級）
    public static final DeferredBlock<ManaDeployerBlock> MANA_DEPLOYER = registerBlock("mana_deployer",
            () -> new ManaDeployerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<ResonanceRingBlock> RESONANCE_RING = registerBlock("resonance_ring",
            () -> new ResonanceRingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(state -> 8).noOcclusion()));


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
