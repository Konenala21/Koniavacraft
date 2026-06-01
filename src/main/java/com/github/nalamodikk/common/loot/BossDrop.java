package com.github.nalamodikk.common.loot;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One possible drop from a boss: an item with a count range, plus a weight for
 * the random pool. Guaranteed drops always appear (their own single-roll pool);
 * non-guaranteed drops compete by weight in the random pool.
 *
 * <p>Shared single source: {@link BossLootRegistry} turns these into a real chest
 * loot table for datagen, and the JEI Boss Drops page renders the same list.
 */
public record BossDrop(Item item, int min, int max, int weight, boolean guaranteed) {

    public static BossDrop guaranteed(Item item, int count) {
        return new BossDrop(item, count, count, 1, true);
    }

    public static BossDrop random(Item item, int min, int max, int weight) {
        return new BossDrop(item, min, max, weight, false);
    }

    /** Representative stack for display (uses the max count). */
    public ItemStack displayStack() {
        return new ItemStack(item, Math.max(1, max));
    }
}
