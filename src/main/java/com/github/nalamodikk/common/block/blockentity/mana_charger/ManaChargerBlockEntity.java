package com.github.nalamodikk.common.block.blockentity.mana_charger;

import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class ManaChargerBlockEntity extends AbstractManaMachineEntityBlock {

    public static final int MAX_MANA_CAP = 10000;
    private static final int CHARGE_RATE = 20;
    static final int ITEM_SLOT = 0;

    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);

    final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> manaStorage != null ? manaStorage.getManaStored() : 0;
                case 1 -> MAX_MANA_CAP;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 2; }
    };

    public ManaChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CHARGER_BE.get(), pos, state,
                false, 0, MAX_MANA_CAP, 1, 0);
        for (Direction dir : Direction.values()) {
            directionConfig.put(dir, IOHandlerUtils.IOType.INPUT);
        }
    }

    @Override
    protected ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.has(ModDataComponents.MAX_MANA);
            }
        };
    }

    @Override
    protected boolean canExternalInsertToSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == ITEM_SLOT;
    }

    @Override
    protected boolean canExternalExtractFromSlot(int slot, @Nullable Direction side) {
        return slot == ITEM_SLOT;
    }

    @Override
    public void tickMachine() {
        if (level == null || level.isClientSide() || itemHandler == null || manaStorage == null) return;

        ItemStack stack = itemHandler.getStackInSlot(ITEM_SLOT);
        if (stack.isEmpty() || !stack.has(ModDataComponents.MAX_MANA)) return;

        int maxItemMana = stack.getOrDefault(ModDataComponents.MAX_MANA, 0);
        int currentMana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (currentMana >= maxItemMana) return;

        int available = manaStorage.getManaStored();
        if (available <= 0) return;

        int toCharge = Math.min(CHARGE_RATE, Math.min(maxItemMana - currentMana, available));
        if (toCharge <= 0) return;

        manaStorage.extractMana(toCharge, ManaAction.EXECUTE);
        ItemStack charged = stack.copy();
        charged.set(ModDataComponents.MANA_STORED, currentMana + toCharge);
        itemHandler.setStackInSlot(ITEM_SLOT, charged);
        setChanged();
    }

    // ── IConfigurableBlock ────────────────────────────────────────────────────

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        directionConfig.put(direction, type);
        setChanged();
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.INPUT);
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return new EnumMap<>(directionConfig);
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
        directionConfig.clear();
        for (Direction dir : Direction.values()) {
            directionConfig.put(dir, map.getOrDefault(dir, IOHandlerUtils.IOType.INPUT));
        }
        setChanged();
    }

    @Override
    protected boolean canGenerate() { return false; }

    public int getCurrentMana() {
        return manaStorage != null ? manaStorage.getManaStored() : 0;
    }

    public static int getMaxMana() { return MAX_MANA_CAP; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.mana_charger");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ManaChargerMenu(containerId, playerInventory, this);
    }
}
