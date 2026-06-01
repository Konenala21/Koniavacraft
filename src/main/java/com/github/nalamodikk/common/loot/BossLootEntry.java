package com.github.nalamodikk.common.loot;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * A boss and the loot it can drop. One entry drives both the chest loot table
 * (built in datagen via {@link BossLootRegistry#buildLootTable}) and one row of
 * the JEI Boss Drops page, so the two never drift.
 *
 * @param id        boss id, also the JEI title translation key suffix
 * @param icon      the stack shown as the page icon / input for this boss
 * @param lootTable the chest loot table this boss fills on reward
 * @param minRolls  min rolls of the weighted random pool
 * @param maxRolls  max rolls of the weighted random pool
 * @param drops     every possible drop (guaranteed and random)
 */
public record BossLootEntry(ResourceLocation id, ItemStack icon, ResourceKey<LootTable> lootTable,
                            int minRolls, int maxRolls, List<BossDrop> drops) {

    /** Translation key for the boss's display name in the JEI page. */
    public String titleKey() {
        return "jei." + id.getNamespace() + ".boss_loot." + id.getPath();
    }

    public List<BossDrop> guaranteedDrops() {
        return drops.stream().filter(BossDrop::guaranteed).toList();
    }

    public List<BossDrop> randomDrops() {
        return drops.stream().filter(d -> !d.guaranteed()).toList();
    }
}
