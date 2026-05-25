package com.github.nalamodikk.common.datagen.recipe;

import com.github.nalamodikk.register.ModItems;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class MaterialProcessingRecipeProvider {

    private static Criterion<InventoryChangeTrigger.TriggerInstance> hasItem(net.minecraft.world.level.ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    public static void generate(RecipeOutput output) {
        generateRawManaProcessing(output);
        generateRefinedManaProcessing(output);
        generateCorruptedManaProcessing(output);
        generateEmergencyRecipes(output);
    }

    private static void generateRawManaProcessing(RecipeOutput output) {
        // 原魔塵 → 魔力粉 (熔爐)
        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModItems.RAW_MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.MANA_DUST.get(),
                        0.1f, 300)
                .unlockedBy("has_raw_mana_dust", hasItem(ModItems.RAW_MANA_DUST.get()))
                .save(output, "mana_dust_from_raw_smelting");

        // 原魔塵 → 魔力粉 (高爐)
        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ModItems.RAW_MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.MANA_DUST.get(),
                        0.2f, 150)
                .unlockedBy("has_raw_mana_dust", hasItem(ModItems.RAW_MANA_DUST.get()))
                .save(output, "mana_dust_from_raw_blasting");
    }

    private static void generateRefinedManaProcessing(RecipeOutput output) {
        // 魔力粉 → 精煉魔力粉 (熔爐)
        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModItems.MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.REFINED_MANA_DUST.get(),
                        0.2f, 200)
                .unlockedBy("has_mana_dust", hasItem(ModItems.MANA_DUST.get()))
                .save(output, "refined_mana_dust_from_smelting");

        // 魔力粉 → 精煉魔力粉 (高爐)
        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ModItems.MANA_DUST.get()),
                        RecipeCategory.MISC,
                        ModItems.REFINED_MANA_DUST.get(),
                        0.2f, 100)
                .unlockedBy("has_mana_dust", hasItem(ModItems.MANA_DUST.get()))
                .save(output, "refined_mana_dust_from_blasting");
    }

    private static void generateCorruptedManaProcessing(RecipeOutput output) {
        // 主動製作汙穢魔力粉
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CORRUPTED_MANA_DUST.get(), 3)
                .pattern(" D ")
                .pattern("DFD")
                .pattern("RGR")
                .define('D', ModItems.MANA_DUST.get())
                .define('R', Items.ROTTEN_FLESH)
                .define('F', Items.FERMENTED_SPIDER_EYE)
                .define('G', Items.GREEN_DYE)
                .unlockedBy("has_mana_dust", hasItem(ModItems.MANA_DUST.get()))
                .save(output, "corrupted_mana_dust");
    }

    private static void generateEmergencyRecipes(RecipeOutput output) {
        // 緊急魔力粉合成
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get())
                .requires(ModItems.RAW_MANA_DUST.get(), 3)
                .requires(Items.COAL)
                .unlockedBy("has_raw_mana_dust", hasItem(ModItems.RAW_MANA_DUST.get()))
                .save(output, "emergency_mana_dust_from_raw");
    }
}
