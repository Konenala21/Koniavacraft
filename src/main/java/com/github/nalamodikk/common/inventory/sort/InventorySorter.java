package com.github.nalamodikk.common.inventory.sort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InventorySorter {

    private InventorySorter() {}

    public static void sort(List<Slot> slots, SortMode mode) {
        List<ItemStack> stacks = collectAndMerge(slots);
        stacks.sort(comparatorFor(mode));
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).set(i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
    }

    private static List<ItemStack> collectAndMerge(List<Slot> slots) {
        List<ItemStack> result = new ArrayList<>();
        for (Slot slot : slots) {
            if (slot.getItem().isEmpty()) continue;
            ItemStack remaining = slot.getItem().copy();
            for (ItemStack existing : result) {
                if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space <= 0) continue;
                int transfer = Math.min(remaining.getCount(), space);
                existing.grow(transfer);
                remaining.shrink(transfer);
                if (remaining.isEmpty()) break;
            }
            if (!remaining.isEmpty()) result.add(remaining);
        }
        return result;
    }

    private static Comparator<ItemStack> comparatorFor(SortMode mode) {
        return switch (mode) {
            case BY_TYPE -> Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            case BY_NAME -> Comparator.comparing(s -> s.getHoverName().getString());
            case BY_COUNT -> Comparator.comparingInt(ItemStack::getCount).reversed();
        };
    }
}
