package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AltarPillarBlockEntity;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.block.blockentity.altar.AspectPedestalBlockEntity;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KoniavacraftMod.MOD_ID);

    public static final Supplier<BlockEntityType<ManaCraftingTableBlockEntity>> MANA_CRAFTING_TABLE_BLOCK_BE =
            BLOCK_ENTITY_TYPES.register("mana_crafting_table_be", () ->
                    BlockEntityType.Builder.of(ManaCraftingTableBlockEntity::new,
                            ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<ManaGeneratorBlockEntity>> MANA_GENERATOR_BE =
            BLOCK_ENTITY_TYPES.register("mana_generator", () ->
                    BlockEntityType.Builder.of(ManaGeneratorBlockEntity::new,
                            ModBlocks.MANA_GENERATOR.get()).build(null));

    public static final Supplier<BlockEntityType<SolarManaCollectorBlockEntity>> SOLAR_MANA_COLLECTOR_BE =
            BLOCK_ENTITY_TYPES.register("solar_mana_collector_be", () ->
                    BlockEntityType.Builder.of(SolarManaCollectorBlockEntity::new,
                            ModBlocks.SOLAR_MANA_COLLECTOR.get()).build(null));

    // 🆕 所有導管變種共用同一個 BlockEntity 類型
    public static final Supplier<BlockEntityType<ArcaneConduitBlockEntity>> ARCANE_CONDUIT_BE =
            BLOCK_ENTITY_TYPES.register("arcane_conduit", () ->
                    BlockEntityType.Builder.of(ArcaneConduitBlockEntity::new,
                            ModBlocks.BASIC_ARCANE_CONDUIT.get(),
                            ModBlocks.ADVANCED_ARCANE_CONDUIT.get(),
                            ModBlocks.ELITE_ARCANE_CONDUIT.get()).build(null));

    public static final Supplier<BlockEntityType<ManaInfuserBlockEntity>> MANA_INFUSER =
            BLOCK_ENTITY_TYPES.register("mana_infuser", () ->
                    BlockEntityType.Builder.of(ManaInfuserBlockEntity::new,
                            ModBlocks.MANA_INFUSER.get()).build(null));

    public static final Supplier<BlockEntityType<ManaDeployerBlockEntity>> MANA_DEPLOYER_BE =
            BLOCK_ENTITY_TYPES.register("mana_deployer", () ->
                    BlockEntityType.Builder.of(ManaDeployerBlockEntity::new,
                            ModBlocks.MANA_DEPLOYER.get()).build(null));

    // === ⚙️ 新增：魔力粉碎機 ===
    public static final Supplier<BlockEntityType<ManaGrinderBlockEntity>> MANA_GRINDER_BE =
            BLOCK_ENTITY_TYPES.register("mana_grinder", () ->
                    BlockEntityType.Builder.of(ManaGrinderBlockEntity::new,
                            ModBlocks.MANA_GRINDER.get()).build(null));

//    public static final Supplier<BlockEntityType<ModularMachineBlockEntity>> MODULAR_MACHINE_BE =
//            BLOCK_ENTITY_TYPES.register("modular_machine", () ->
//                    BlockEntityType.Builder.of(ModularMachineBlockEntity::new,
//                            ModBlocks.MODULAR_MACHINE_BLOCK.get()).build());


    public static final Supplier<BlockEntityType<AspectAltarBlockEntity>> ASPECT_ALTAR_BE =
            BLOCK_ENTITY_TYPES.register("aspect_altar", () ->
                    BlockEntityType.Builder.of(AspectAltarBlockEntity::new,
                            ModBlocks.ASPECT_ALTAR.get()).build(null));

    public static final Supplier<BlockEntityType<AltarPillarBlockEntity>> ALTAR_PILLAR_BE =
            BLOCK_ENTITY_TYPES.register("altar_pillar", () ->
                    BlockEntityType.Builder.of(AltarPillarBlockEntity::new,
                            ModBlocks.ALTAR_PILLAR.get()).build(null));

    public static final Supplier<BlockEntityType<AspectPedestalBlockEntity>> ASPECT_PEDESTAL_BE =
            BLOCK_ENTITY_TYPES.register("aspect_pedestal", () ->
                    BlockEntityType.Builder.of(AspectPedestalBlockEntity::new,
                            ModBlocks.ASPECT_PEDESTAL.get()).build(null));

    public static final Supplier<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_BE =
            BLOCK_ENTITY_TYPES.register("research_table", () ->
                    BlockEntityType.Builder.of(ResearchTableBlockEntity::new,
                            ModBlocks.RESEARCH_TABLE.get()).build(null));


    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
