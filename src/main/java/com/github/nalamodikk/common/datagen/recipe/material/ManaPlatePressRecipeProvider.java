package com.github.nalamodikk.common.datagen.recipe.material;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressRecipe;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class ManaPlatePressRecipeProvider {

    public static void generate(RecipeOutput output) {
        // 魔力合金錠 → 魔力強化板
        createPlatePressRecipe(output, "mana_alloy_ingot_to_plate",
                Ingredient.of(ModItems.MANA_ALLOY_INGOT.get()),
                new ItemStack(ModItems.MANA_REINFORCED_PLATE.get()),
                3000, 80, 1);
    }

    private static void createPlatePressRecipe(RecipeOutput output,
                                               String name,
                                               Ingredient input,
                                               ItemStack result,
                                               int manaCost,
                                               int pressingTime,
                                               int inputCount) {
        ManaPlatePressRecipe recipe = new ManaPlatePressRecipe(input, result, manaCost, pressingTime, inputCount);
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                KoniavacraftMod.MOD_ID, "plate_press/" + name);
        output.accept(recipeId, recipe, null);
    }
}
