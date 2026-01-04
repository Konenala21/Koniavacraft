package com.github.nalamodikk.common.utils.slot;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import org.jetbrains.annotations.NotNull;

/**
 * A Slot implementation that directly wraps a NonNullList<ItemStack>,
 * allowing DataComponent-based storage (like on Player) to be used in GUIs.
 */
public class ComponentItemStackSlot extends Slot {

    private final NonNullList<ItemStack> list;
    private final int index;

    public ComponentItemStackSlot(NonNullList<ItemStack> list, int index, int x, int y) {
        super(null, index, x, y); // container is null because we manage the list directly
        this.list = list;
        this.index = index;
    }

    @Override
    public @NotNull ItemStack getItem() {
        return list.get(index);
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        list.set(index, stack);
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    @Override
    public void setChanged() {
        // Optional: trigger sync or dirty flag here
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return getMaxStackSize();
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        return getItem().split(amount);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return true; // You can customize this (e.g. only allow "module" items)
    }
}
