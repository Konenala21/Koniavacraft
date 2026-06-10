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
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressBlock;
import com.github.nalamodikk.common.block.blockentity.skillencoder.SkillEncoderBlock;
import com.github.nalamodikk.common.block.blockentity.mana_charger.ManaChargerBlock;
import com.github.nalamodikk.common.block.normal.DeepManaSoilBlock;
import com.github.nalamodikk.common.block.normal.ManaBloomBlock;
import com.github.nalamodikk.common.block.normal.ManaGrassBlock;
import com.github.nalamodikk.common.block.normal.ManaSoilBlock;
import com.github.nalamodikk.space.ship.ShipAssemblyBaseBlock;
import com.github.nalamodikk.space.ship.ShipAssemblyGantryBlock;
import com.github.nalamodikk.space.ship.ShipAssemblyPadBlock;
import com.github.nalamodikk.space.ship.ShipCoreBlock;
import com.github.nalamodikk.space.ship.ShipSeatBlock;
import com.github.nalamodikk.space.ship.ManaEngineBlock;
import com.github.nalamodikk.space.ship.ManaWarpEngineBlock;
import com.github.nalamodikk.space.ship.ManaFuelTankBlock;
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

    // ── 月球方塊 ─────────────────────────────────────────────────────────
    // 月壤（表層細塵，灰白，鏟挖）
    public static final DeferredBlock<Block> MOON_REGOLITH =
            registerBlock("moon_regolith", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL)));
    // 月岩（灰色基岩，鎬挖）
    public static final DeferredBlock<Block> MOON_STONE =
            registerBlock("moon_stone", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(1.6f, 6.0f).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));
    // 深層月岩（深灰，更硬）
    public static final DeferredBlock<Block> MOON_DEEPSTONE =
            registerBlock("moon_deepstone", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE).strength(3.0f, 9.0f).sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));
    // 月核（發光，中空底部的核心；玩家在此重力翻轉穿到另一面）
    public static final DeferredBlock<Block> MOON_CORE =
            registerBlock("moon_core", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).strength(5.0f, 12.0f).sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(s -> 10).requiresCorrectToolForDrops()));

    // 本源研究桌:本源研究員村民的職業方塊(純方塊、無 BlockEntity,跟玩家用的研究台分開,避免 POI 卡頓)
    public static final DeferredBlock<Block> ASPECT_RESEARCH_DESK =
            registerBlock("aspect_research_desk", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN)));

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

    // === ⚡ 魔力充能台 ===
    public static final DeferredBlock<ManaChargerBlock> MANA_CHARGER =
            registerBlock("mana_charger", () -> new ManaChargerBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                            .strength(3.0f).sound(SoundType.METAL)
                            .requiresCorrectToolForDrops().noOcclusion()));

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

    // === 🔩 魔力壓板機 ===
    public static final DeferredBlock<ManaPlatePressBlock> MANA_PLATE_PRESS = registerBlock("mana_plate_press",
            () -> new ManaPlatePressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state ->
                            state.getValue(ManaPlatePressBlock.WORKING) ? 5 : 0)));

    // === 🪄 技能核心編碼台 ===
    public static final DeferredBlock<SkillEncoderBlock> SKILL_ENCODER = registerBlock("skill_encoder",
            () -> new SkillEncoderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

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

    // ── 飛船核心（駕駛/錨點，會跟船走；用組裝台組裝）─────────────────────
    public static final DeferredBlock<ShipCoreBlock> SHIP_CORE = registerBlock("ship_core",
            () -> new ShipCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // ── 飛船魔力引擎 / 燃料槽（組裝必要骨架：核心 + ≥1 引擎 + ≥1 燃料槽）──────
    public static final DeferredBlock<ManaEngineBlock> MANA_ENGINE = registerBlock("mana_engine",
            () -> new ManaEngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<ManaFuelTankBlock> MANA_FUEL_TANK = registerBlock("mana_fuel_tank",
            () -> new ManaFuelTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // ── 曲速引擎（T2 高速 tier，在場時上限 200→600，吃燃料兇）─────────────
    public static final DeferredBlock<ManaWarpEngineBlock> MANA_WARP_ENGINE = registerBlock("mana_warp_engine",
            () -> new ManaWarpEngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 6)));

    // ── 飛船組裝台（控制台，右鍵開 GUI 掃描/組裝）───────────────────────
    public static final DeferredBlock<ShipAssemblyPadBlock> SHIP_ASSEMBLY_PAD = registerBlock("ship_assembly_pad",
            () -> new ShipAssemblyPadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // ── 飛船組裝底座（地板，footprint 由相連底座外接矩形決定）────────────
    public static final DeferredBlock<ShipAssemblyBaseBlock> SHIP_ASSEMBLY_BASE = registerBlock("ship_assembly_base",
            () -> new ShipAssemblyBaseBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // ── 飛船組裝架（龍門框架柱，最高的決定建造盒高度）──────────────────
    public static final DeferredBlock<ShipAssemblyGantryBlock> SHIP_ASSEMBLY_GANTRY = registerBlock("ship_assembly_gantry",
            () -> new ShipAssemblyGantryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // ── 飛船座椅（組進船的乘客座位，核心=駕駛位）─────────────────────────
    public static final DeferredBlock<ShipSeatBlock> SHIP_SEAT = registerBlock("ship_seat",
            () -> new ShipSeatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f).sound(SoundType.METAL).noOcclusion()));


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
