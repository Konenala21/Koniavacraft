package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AltarPillarBlockEntity;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.block.blockentity.altar.AspectPedestalBlockEntity;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_charger.ManaChargerBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KoniavacraftMod.MOD_ID);

    // ── Helper ────────────────────────────────────────────────────────────────

    /** 單一方塊對應一個 BlockEntity 的標準註冊。 */
    private static <T extends BlockEntity> Supplier<BlockEntityType<T>> register(
            String name,
            BiFunction<BlockPos, BlockState, T> factory,
            DeferredBlock<? extends Block> block) {
        return BLOCK_ENTITY_TYPES.register(name, () ->
                BlockEntityType.Builder.of(factory::apply, block.get()).build(null));
    }

    /** 多個方塊共用同一個 BlockEntity（例如導管三種等級）。 */
    @SafeVarargs
    private static <T extends BlockEntity> Supplier<BlockEntityType<T>> register(
            String name,
            BiFunction<BlockPos, BlockState, T> factory,
            DeferredBlock<? extends Block>... blocks) {
        return BLOCK_ENTITY_TYPES.register(name, () -> {
            Block[] resolved = java.util.Arrays.stream(blocks)
                    .map(DeferredBlock::get)
                    .toArray(Block[]::new);
            return BlockEntityType.Builder.of(factory::apply, resolved).build(null);
        });
    }

    // ── Block Entities ────────────────────────────────────────────────────────

    public static final Supplier<BlockEntityType<ManaCraftingTableBlockEntity>> MANA_CRAFTING_TABLE_BLOCK_BE =
            register("mana_crafting_table_be", ManaCraftingTableBlockEntity::new, ModBlocks.MANA_CRAFTING_TABLE_BLOCK);

    public static final Supplier<BlockEntityType<ManaGeneratorBlockEntity>> MANA_GENERATOR_BE =
            register("mana_generator", ManaGeneratorBlockEntity::new, ModBlocks.MANA_GENERATOR);

    public static final Supplier<BlockEntityType<SolarManaCollectorBlockEntity>> SOLAR_MANA_COLLECTOR_BE =
            register("solar_mana_collector_be", SolarManaCollectorBlockEntity::new, ModBlocks.SOLAR_MANA_COLLECTOR);

    public static final Supplier<BlockEntityType<ArcaneConduitBlockEntity>> ARCANE_CONDUIT_BE =
            register("arcane_conduit", ArcaneConduitBlockEntity::new,
                    ModBlocks.BASIC_ARCANE_CONDUIT, ModBlocks.ADVANCED_ARCANE_CONDUIT, ModBlocks.ELITE_ARCANE_CONDUIT);

    public static final Supplier<BlockEntityType<ManaInfuserBlockEntity>> MANA_INFUSER =
            register("mana_infuser", ManaInfuserBlockEntity::new, ModBlocks.MANA_INFUSER);

    public static final Supplier<BlockEntityType<ManaDeployerBlockEntity>> MANA_DEPLOYER_BE =
            register("mana_deployer", ManaDeployerBlockEntity::new, ModBlocks.MANA_DEPLOYER);

    public static final Supplier<BlockEntityType<ManaChargerBlockEntity>> MANA_CHARGER_BE =
            register("mana_charger", ManaChargerBlockEntity::new, ModBlocks.MANA_CHARGER);

    public static final Supplier<BlockEntityType<ManaGrinderBlockEntity>> MANA_GRINDER_BE =
            register("mana_grinder", ManaGrinderBlockEntity::new, ModBlocks.MANA_GRINDER);

    public static final Supplier<BlockEntityType<ManaPlatePressBlockEntity>> MANA_PLATE_PRESS_BE =
            register("mana_plate_press", ManaPlatePressBlockEntity::new, ModBlocks.MANA_PLATE_PRESS);

    public static final Supplier<BlockEntityType<AspectAltarBlockEntity>> ASPECT_ALTAR_BE =
            register("aspect_altar", AspectAltarBlockEntity::new, ModBlocks.ASPECT_ALTAR);

    public static final Supplier<BlockEntityType<AltarPillarBlockEntity>> ALTAR_PILLAR_BE =
            register("altar_pillar", AltarPillarBlockEntity::new, ModBlocks.ALTAR_PILLAR);

    public static final Supplier<BlockEntityType<AspectPedestalBlockEntity>> ASPECT_PEDESTAL_BE =
            register("aspect_pedestal", AspectPedestalBlockEntity::new, ModBlocks.ASPECT_PEDESTAL);

    public static final Supplier<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_BE =
            register("research_table", ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE);

    // ── Init ──────────────────────────────────────────────────────────────────

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
