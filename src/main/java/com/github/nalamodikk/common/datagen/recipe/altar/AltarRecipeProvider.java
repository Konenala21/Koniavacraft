package com.github.nalamodikk.common.datagen.recipe.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AltarRecipe;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class AltarRecipeProvider {

    public static void generate(RecipeOutput output) {
        registerCircuitMaterials(output);
        // 魔力水晶（催化：精煉魔力粉，底座：魔力碎片×4）
        save(output, "mana_crystal_ritual",
                Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                List.of(
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get())
                ),
                new ItemStack(ModItems.MANA_CRYSTAL.get(), 2),
                8000, 120
        );

        // 魔力錠（催化：魔力水晶，底座：鐵錠×4）
        save(output, "mana_ingot_ritual",
                Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT)
                ),
                new ItemStack(ModItems.MANA_INGOT.get(), 4),
                6000, 100
        );

        // 金蘋果（催化：魔力錠，底座：金錠×4）
        save(output, "golden_apple_ritual",
                Ingredient.of(ModItems.MANA_INGOT.get()),
                List.of(
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.APPLE)
                ),
                new ItemStack(Items.GOLDEN_APPLE),
                12000, 160
        );
    }

    private static void registerCircuitMaterials(RecipeOutput output) {
        save(output, "mana_wafer",
                Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                List.of(
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get())
                ),
                new ItemStack(ModItems.MANA_WAFER.get(), 2),
                5000, 100
        );
    }

    private static void save(RecipeOutput output, String name,
                              Ingredient catalyst, List<Ingredient> ingredients,
                              ItemStack result, int manaCost, int processingTime) {
        save(output, name, catalyst, ingredients, result, manaCost, processingTime, 0);
    }

    private static void save(RecipeOutput output, String name,
                              Ingredient catalyst, List<Ingredient> ingredients,
                              ItemStack result, int manaCost, int processingTime, int minTier) {
        AltarRecipe recipe = new AltarRecipe(catalyst, ingredients, result, manaCost, processingTime, minTier);
        output.accept(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "altar/" + name),
                recipe, null
        );
    }
}
