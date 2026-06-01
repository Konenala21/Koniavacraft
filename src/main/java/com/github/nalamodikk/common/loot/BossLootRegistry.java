package com.github.nalamodikk.common.loot;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry of boss to droppable loot. Single source of truth for the
 * chest loot tables (datagen reads {@link #buildLootTable}) and the JEI Boss
 * Drops page (reads {@link #all}). Adding a future boss is one {@link #register}
 * call: its loot table and JEI row both follow automatically.
 */
public final class BossLootRegistry {

    private static final Map<ResourceLocation, BossLootEntry> REGISTRY = new LinkedHashMap<>();

    /** Mirror World boss (PlayerClone): the first reward, gating the skill system via the shard. */
    public static final BossLootEntry MIRROR_BOSS = register(new BossLootEntry(
            id("mirror_boss"),
            new ItemStack(ModItems.MIRROR_CORE_SHARD.get()),
            ResourceKey.create(Registries.LOOT_TABLE, id("chests/mirror_boss_reward")),
            3, 4,
            List.of(
                    BossDrop.guaranteed(ModItems.MIRROR_CORE_SHARD.get(), 1),
                    BossDrop.random(ModItems.MANA_INGOT.get(), 2, 4, 4),
                    BossDrop.random(ModItems.MANA_DUST.get(), 4, 8, 4),
                    BossDrop.random(ModItems.CORRUPTED_MANA_DUST.get(), 2, 4, 2),
                    BossDrop.random(ModItems.MANA_CRYSTAL.get(), 1, 2, 1)
            )
    ));

    private static BossLootEntry register(BossLootEntry entry) {
        REGISTRY.put(entry.id(), entry);
        return entry;
    }

    public static Collection<BossLootEntry> all() {
        return REGISTRY.values();
    }

    /**
     * Builds the chest loot table for a boss from its shared drop list: one
     * single-roll pool per guaranteed drop, plus one weighted random pool.
     */
    public static LootTable.Builder buildLootTable(BossLootEntry entry) {
        LootTable.Builder table = LootTable.lootTable();
        for (BossDrop d : entry.guaranteedDrops()) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(countedItem(d)));
        }
        List<BossDrop> random = entry.randomDrops();
        if (!random.isEmpty()) {
            LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(UniformGenerator.between(entry.minRolls(), entry.maxRolls()));
            for (BossDrop d : random) {
                pool.add(countedItem(d).setWeight(d.weight()));
            }
            table.withPool(pool);
        }
        return table;
    }

    private static LootPoolSingletonContainer.Builder<?> countedItem(BossDrop d) {
        LootPoolSingletonContainer.Builder<?> item = LootItem.lootTableItem(d.item());
        if (d.min() == d.max()) {
            if (d.max() > 1) {
                item.apply(SetItemCountFunction.setCount(ConstantValue.exactly(d.max())));
            }
        } else {
            item.apply(SetItemCountFunction.setCount(UniformGenerator.between(d.min(), d.max())));
        }
        return item;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
    }

    private BossLootRegistry() {}
}
