package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.capability.RestrictedManaHandler;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Supplier;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModCapabilities {

    public static final BlockCapability<IUnifiedManaHandler, Direction> MANA =
            BlockCapability.create(
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana"),
                    IUnifiedManaHandler.class,
                    Direction.class
            );

    public static final DataComponentType<Boolean> NARA_BOUND =
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build();

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** 所有面只接收魔力（INPUT only）。充能台、聚陣等純消耗機器使用。 */
    public static <T extends AbstractManaMachineEntityBlock> void manaInput(
            RegisterCapabilitiesEvent event, Supplier<BlockEntityType<T>> beType) {
        event.registerBlockEntity(MANA, beType.get(),
                (be, side) -> new RestrictedManaHandler(be.getManaStorage(), true, false));
    }

    /** 所有面只輸出魔力（OUTPUT only）。純產出機器使用。 */
    public static <T extends AbstractManaMachineEntityBlock> void manaOutput(
            RegisterCapabilitiesEvent event, Supplier<BlockEntityType<T>> beType) {
        event.registerBlockEntity(MANA, beType.get(),
                (be, side) -> new RestrictedManaHandler(be.getManaStorage(), false, true));
    }

    /**
     * 依 IO 配置決定能力（DISABLED/INPUT/OUTPUT/BOTH）。
     * 有 IConfigurableBlock 的機器（發電機、注入機、粉碎機等）使用。
     */
    public static <T extends AbstractManaMachineEntityBlock & IConfigurableBlock> void manaIO(
            RegisterCapabilitiesEvent event, Supplier<BlockEntityType<T>> beType) {
        event.registerBlockEntity(MANA, beType.get(), (be, side) -> {
            if (side != null) {
                return switch (be.getIOConfig(side)) {
                    case DISABLED -> null;
                    case INPUT    -> new RestrictedManaHandler(be.getManaStorage(), true, false);
                    case OUTPUT   -> new RestrictedManaHandler(be.getManaStorage(), false, true);
                    case BOTH     -> be.getManaStorage();
                };
            }
            return be.getManaStorage();
        });
    }

    /** 依 IO 配置暴露物品槽（DISABLED 面不提供）。有 getItemHandlerForSide 的機器使用。 */
    public static <T extends AbstractManaMachineEntityBlock & IConfigurableBlock> void itemHandlerIO(
            RegisterCapabilitiesEvent event, Supplier<BlockEntityType<T>> beType) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, beType.get(), (be, side) -> {
            if (side != null && be.getIOConfig(side) == IOHandlerUtils.IOType.DISABLED) return null;
            return be.getItemHandlerForSide(side);
        });
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        // 魔力發電機：可配置 IO + RF 能量
        manaIO(event, ModBlockEntities.MANA_GENERATOR_BE);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_GENERATOR_BE.get(),
                (be, side) -> {
                    if (side != null && be.getIOConfig(side) == IOHandlerUtils.IOType.DISABLED) return null;
                    return be.getInventory();
                });
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.MANA_GENERATOR_BE.get(),
                (be, side) -> {
                    if (side == null) return be.getEnergyStorage();
                    IOHandlerUtils.IOType io = be.getIOConfig(side);
                    if (io == IOHandlerUtils.IOType.DISABLED || io == IOHandlerUtils.IOType.INPUT) return null;
                    return be.getEnergyStorage();
                });

        // 太陽能魔力收集器：完整魔力 + 物品槽
        event.registerBlockEntity(MANA, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(),
                (be, side) -> be.getManaStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(),
                (be, side) -> be.getItemHandler());

        // 魔力合成台：完整魔力 + 物品槽
        event.registerBlockEntity(MANA, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(),
                (be, side) -> be.getManaStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(),
                (be, side) -> be.getItemHandler());

        // 魔力注入機：可配置 IO + 物品槽
        manaIO(event, ModBlockEntities.MANA_INFUSER);
        itemHandlerIO(event, ModBlockEntities.MANA_INFUSER);

        // 魔力粉碎機：可配置 IO + 物品槽
        manaIO(event, ModBlockEntities.MANA_GRINDER_BE);
        itemHandlerIO(event, ModBlockEntities.MANA_GRINDER_BE);

        // 魔力壓板機：可配置 IO + 物品槽（先前漏註冊，導致導管/鄰居無法灌入魔力）
        manaIO(event, ModBlockEntities.MANA_PLATE_PRESS_BE);
        itemHandlerIO(event, ModBlockEntities.MANA_PLATE_PRESS_BE);

        // 魔力部署器：INPUT only
        manaInput(event, ModBlockEntities.MANA_DEPLOYER_BE);

        // 本源聚陣：INPUT only
        event.registerBlockEntity(MANA, ModBlockEntities.ASPECT_ALTAR_BE.get(),
                (be, side) -> new RestrictedManaHandler(be.getManaStorage(), true, false));

        // 本源基座：物品槽
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ASPECT_PEDESTAL_BE.get(),
                (be, side) -> be.getItemHandler());

        // 導管：完整魔力（conduit 自身就是 handler）
        event.registerBlockEntity(MANA, ModBlockEntities.ARCANE_CONDUIT_BE.get(),
                (be, side) -> be);

        // 魔力充能台：INPUT only + 物品槽
        manaInput(event, ModBlockEntities.MANA_CHARGER_BE);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_CHARGER_BE.get(),
                (be, side) -> be.getItemHandler());
    }
}
