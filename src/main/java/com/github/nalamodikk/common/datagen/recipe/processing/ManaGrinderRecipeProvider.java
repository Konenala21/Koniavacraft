package com.github.nalamodikk.common.datagen.recipe.processing;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.ProcessingRecipeProvider;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

/**
 * ⚙️ 魔力粉碎機配方數據生成器
 */
public class ManaGrinderRecipeProvider {
    public static void generate(RecipeOutput output) {

        // === 核心材料鏈 ===

        // 原魔塵 → 魔力粉 ×2（比熔爐 1:1 更高效）
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/raw_mana_dust_grind", "grinder")
                .input(ModItems.RAW_MANA_DUST.get())
                .output(ModItems.MANA_DUST.get(), 2)
                .manaCost(1500)
                .processingTime(80)
                .save();

        // 汙穢魔力粉 ×2 → 魔力粉 ×1（淨化副產物）
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/corrupted_mana_dust_purify", "grinder")
                .input(ModItems.CORRUPTED_MANA_DUST.get())
                .input(ModItems.CORRUPTED_MANA_DUST.get())
                .output(ModItems.MANA_DUST.get(), 1)
                .manaCost(2500)
                .processingTime(100)
                .save();

        // === 實用研磨配方 ===

        // 圓石 → 碎石
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/cobblestone_to_gravel", "grinder")
                .input(Items.COBBLESTONE)
                .output(Items.GRAVEL, 1)
                .manaCost(500)
                .processingTime(40)
                .save();

        // 碎石 → 沙子
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/gravel_to_sand", "grinder")
                .input(Items.GRAVEL)
                .output(Items.SAND, 1)
                .manaCost(500)
                .processingTime(40)
                .save();

        // 骨頭 → 骨粉 ×4（比原版更多）
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/bone_to_bonemeal", "grinder")
                .input(Items.BONE)
                .output(Items.BONE_MEAL, 4)
                .manaCost(800)
                .processingTime(50)
                .save();

        // 烈焰棒 → 烈焰粉 ×4（比原版 ×2 更多）
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/blaze_rod_to_powder", "grinder")
                .input(Items.BLAZE_ROD)
                .output(Items.BLAZE_POWDER, 4)
                .manaCost(1200)
                .processingTime(60)
                .save();

        KoniavacraftMod.LOGGER.debug("Generated {} mana grinder recipes.", 6);
    }
}
