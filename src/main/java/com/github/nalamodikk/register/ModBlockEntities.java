package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ore_grinder.OreGrinderBlockEntity;
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
                            ModBlocks.ARCANE_CONDUIT.get(),
                            ModBlocks.BASIC_ARCANE_CONDUIT.get(),
                            ModBlocks.ADVANCED_ARCANE_CONDUIT.get(),
                            ModBlocks.ELITE_ARCANE_CONDUIT.get()).build(null));

    public static final Supplier<BlockEntityType<ManaInfuserBlockEntity>> MANA_INFUSER =
            BLOCK_ENTITY_TYPES.register("mana_infuser", () ->
                    BlockEntityType.Builder.of(ManaInfuserBlockEntity::new,
                            ModBlocks.MANA_INFUSER.get()).build(null));

    // === ⚙️ 新增：礦石粉碎機 ===
    public static final Supplier<BlockEntityType<OreGrinderBlockEntity>> ORE_GRINDER =
            BLOCK_ENTITY_TYPES.register("mana_grinder", () ->
                    BlockEntityType.Builder.of(OreGrinderBlockEntity::new,
                            ModBlocks.ORE_GRINDER.get()).build(null));

//    public static final Supplier<BlockEntityType<ModularMachineBlockEntity>> MODULAR_MACHINE_BE =
//            BLOCK_ENTITY_TYPES.register("modular_machine", () ->
//                    BlockEntityType.Builder.of(ModularMachineBlockEntity::new,
//                            ModBlocks.MODULAR_MACHINE_BLOCK.get()).build());


    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
