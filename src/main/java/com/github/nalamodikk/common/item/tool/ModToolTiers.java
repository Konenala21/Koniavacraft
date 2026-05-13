package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.register.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public enum ModToolTiers implements Tier {

    MANA(1800, 8.5f, 3.5f, 14, BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.MANA_CRYSTAL.get()));

    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;
    private final TagKey<Block> incorrectBlocksForDrops;
    private final Supplier<Ingredient> repairIngredient;

    ModToolTiers(int uses, float speed, float attackDamageBonus, int enchantmentValue,
                 TagKey<Block> incorrectBlocksForDrops, Supplier<Ingredient> repairIngredient) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.repairIngredient = repairIngredient;
    }

    @Override public int getUses()                          { return uses; }
    @Override public float getSpeed()                       { return speed; }
    @Override public float getAttackDamageBonus()           { return attackDamageBonus; }
    @Override public int getEnchantmentValue()              { return enchantmentValue; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrectBlocksForDrops; }
    @Override public Ingredient getRepairIngredient()       { return repairIngredient.get(); }
}
