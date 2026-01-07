package com.github.nalamodikk.common.datagen.recipe.material;

import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.ManaGenFuelRecipeBuilder;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeOutput;

public class ManaFuelRecipeProvider {

    public static void generate(RecipeOutput output) {
        // 🪵 Minecraft 基礎燃料
        ManaGenFuelRecipeBuilder.create(Items.COAL, 5, 16, 200).save(output);
        ManaGenFuelRecipeBuilder.create(Items.BLAZE_ROD, 0, 32, 1600).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.REFINED_MANA_DUST.get(), 75, 30,    600).save(output);

        // 🧪 你模組內的物品
        ManaGenFuelRecipeBuilder.create(ModItems.CORRUPTED_MANA_DUST.get(), 20, 15, 400).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.MANA_DUST.get(), 50, 25, 800).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.MANA_INGOT.get(), 55, 25, 1000).save(output);
    }
}
