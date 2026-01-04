package com.github.nalamodikk.common.utils.loot;

import net.minecraft.advancements.critereon.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider ;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import javax.annotation.Nullable;
import java.util.List;

// LootUtils.java
public class LootTableUtils {
    public static LootTable.Builder createOreDropsWithBonusAndSilkTouch(
            Block block,
            ItemLike mainDrop, float min, float max,
            @Nullable ItemLike extraDrop, float extraChance,
            HolderLookup.RegistryLookup<Enchantment> enchantments
    ) {
        // 主掉落邏輯：支援 Fortune 與爆炸損耗
        LootPoolEntryContainer.Builder<?> mainDropEntry = LootItem.lootTableItem(mainDrop)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                .apply(ApplyExplosionDecay.explosionDecay());

        // 額外掉落邏輯
        LootPoolEntryContainer.Builder<?> extraDropEntry = null;
        if (extraDrop != null && extraChance > 0f) {
            extraDropEntry = LootItem.lootTableItem(extraDrop)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                    .apply(ApplyExplosionDecay.explosionDecay())
                    .when(LootItemRandomChanceCondition.randomChance(extraChance));
        }

        // 絲綢之觸條件
        LootItemCondition.Builder silkTouch = MatchTool.toolMatches(
                ItemPredicate.Builder.item().withSubPredicate(
                        ItemSubPredicates.ENCHANTMENTS,
                        ItemEnchantmentsPredicate.enchantments(List.of(
                                new EnchantmentPredicate(
                                        enchantments.getOrThrow(Enchantments.SILK_TOUCH),
                                        MinMaxBounds.Ints.atLeast(1)
                                )
                        ))
                )
        );

        // 整張表：先處理絲綢之觸（掉方塊本身），否則用你自己構造的 pool
        LootTable.Builder table = LootTable.lootTable();

        table.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block).when(silkTouch))
        );

        LootPool.Builder normalDropPool = LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(silkTouch.invert());
        normalDropPool.add(mainDropEntry);
        if (extraDropEntry != null) {
            normalDropPool.add(extraDropEntry);
        }

        table.withPool(normalDropPool);
        return table;
    }
}