package com.github.nalamodikk.common.block.blockentity.manabase;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Side-aware wrapper used for hoppers/pipes to obey machine IO config and slot policies.
 */
public class MachineSidedItemHandler implements IItemHandler {
    private final AbstractManaMachineEntityBlock machine;
    @Nullable
    private final Direction side;

    public MachineSidedItemHandler(AbstractManaMachineEntityBlock machine, @Nullable Direction side) {
        this.machine = machine;
        this.side = side;
    }

    @Override
    public int getSlots() {
        ItemStackHandler delegate = machine.getItemHandler();
        return delegate == null ? 0 : delegate.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        ItemStackHandler delegate = machine.getItemHandler();
        return delegate == null ? ItemStack.EMPTY : delegate.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        ItemStackHandler delegate = machine.getItemHandler();
        if (delegate == null || stack.isEmpty()) {
            return stack;
        }
        if (!machine.canReceiveItemsFromSide(side) || !machine.canExternalInsertToSlot(slot, stack, side)) {
            return stack;
        }
        return delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStackHandler delegate = machine.getItemHandler();
        if (delegate == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (!machine.canOutputItemsToSide(side) || !machine.canExternalExtractFromSlot(slot, side)) {
            return ItemStack.EMPTY;
        }
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        ItemStackHandler delegate = machine.getItemHandler();
        return delegate == null ? 64 : delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        ItemStackHandler delegate = machine.getItemHandler();
        return delegate != null
                && machine.canReceiveItemsFromSide(side)
                && machine.canExternalInsertToSlot(slot, stack, side)
                && delegate.isItemValid(slot, stack);
    }
}
