package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.loot.BossLootEntry;
import com.github.nalamodikk.common.loot.BossLootRegistry;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModChestLootTableProvider implements net.minecraft.data.loot.LootTableSubProvider {

    @SuppressWarnings("unused")
    public ModChestLootTableProvider(HolderLookup.Provider registries) {}

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
                ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "chests/abandoned_altar")),
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3, 5))
                                .add(LootItem.lootTableItem(ModItems.MANA_DUST.get()).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                                .add(LootItem.lootTableItem(ModItems.RAW_MANA_DUST.get()).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))))
                                .add(LootItem.lootTableItem(ModItems.MANA_INGOT.get()).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                                .add(LootItem.lootTableItem(ModItems.MANA_CRYSTAL_FRAGMENT.get()).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                                .add(LootItem.lootTableItem(ModItems.MANA_CRYSTAL.get()).setWeight(1)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        )
        );

        // Boss 獎勵寶箱：由共用 BossLootRegistry 建表（單一真實來源，JEI Boss 掉落分頁讀同一份定義）。
        // 加新 boss 只要在 BossLootRegistry 註冊一筆，loot table 自動產生。
        for (BossLootEntry boss : BossLootRegistry.all()) {
            output.accept(boss.lootTable(), BossLootRegistry.buildLootTable(boss));
        }
    }
}
