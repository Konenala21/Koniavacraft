package com.github.nalamodikk.common.datagen.recipe.material;

import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.ManaGenFuelRecipeBuilder;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeOutput;

public class ManaFuelRecipeProvider {

    public static void generate(RecipeOutput output) {
        // Minecraft 基礎燃料
        ManaGenFuelRecipeBuilder.create(Items.COAL, 4, 16, 200).save(output);
        ManaGenFuelRecipeBuilder.create(Items.BLAZE_ROD, 0, 32, 1600).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.REFINED_MANA_DUST.get(), 40, 30, 600).save(output);

        // 模組物品燃料
        ManaGenFuelRecipeBuilder.create(ModItems.CORRUPTED_MANA_DUST.get(), 12, 15, 400).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.MANA_DUST.get(), 30, 25, 800).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.MANA_INGOT.get(), 34, 25, 1000).save(output);
    }
}
