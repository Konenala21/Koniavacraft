package com.github.nalamodikk.common.datagen.recipe.processing;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.ProcessingRecipeProvider;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * ⚙️ 魔力粉碎機配方數據生成器
 */
public class ManaGrinderRecipeProvider {
    public static void generate(RecipeOutput output) {
        // 🪨 石頭 → 沙粒
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/stone_grind", "grinder")
                .input(Blocks.STONE)
                .output(Items.SAND, 1)
                .manaCost(2000)
                .processingTime(100)
                .save();

        // 💎 鑽石 → 魔力粉 (修正：之前錯誤產出玻璃)
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/diamond_grind", "grinder")
                .input(Items.DIAMOND)
                .output(ModItems.MANA_DUST.get(), 1)
                .chanceOutput(Items.GRAVEL, 1, 0.2f)
                .manaCost(3000)
                .processingTime(150)
                .save();

        // 石頭 + 圓石 → 磚塊 x2 + 黏土 (15%)
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/multi_input_example", "grinder")
                .input(Blocks.STONE)
                .input(Blocks.COBBLESTONE)
                .output(Items.BRICKS, 2)
                .chanceOutput(Items.CLAY_BALL, 1, 0.15f)
                .manaCost(4000)
                .processingTime(200)
                .save();

        KoniavacraftMod.LOGGER.info("✅ 生成了 3 個粉碎機配方");
    }
}
