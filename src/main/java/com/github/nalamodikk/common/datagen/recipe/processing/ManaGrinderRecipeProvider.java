package com.github.nalamodikk.common.datagen.recipe.processing;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.ProcessingRecipeProvider;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

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

        // ── T1 電路板中間材料 ─────────────────────────────────────────────────
        // ① 精煉魔力粉 + 銅錠 → 魔力基板
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/mana_substrate", "grinder")
                .input(ModItems.REFINED_MANA_DUST.get())
                .input(Items.COPPER_INGOT)
                .output(ModItems.MANA_SUBSTRATE.get(), 1)
                .manaCost(2000)
                .processingTime(80)
                .save();

        // ② 壓縮魔力粉 + 金錠 → 魔力導線
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/mana_wire", "grinder")
                .input(ModItems.CONDENSED_MANA_DUST.get())
                .input(Items.GOLD_INGOT)
                .output(ModItems.MANA_WIRE.get(), 4)
                .manaCost(2500)
                .processingTime(100)
                .save();

        // ③ 史萊姆球 + 魔力粉 → 魔力黏膠（grinder 只有 2 槽，改為單個 slime_ball）
        ProcessingRecipeProvider.createProcessingRecipe(output, "grinder/mana_adhesive", "grinder")
                .input(Items.SLIME_BALL)
                .input(ModItems.MANA_DUST.get())
                .output(ModItems.MANA_ADHESIVE.get(), 3)
                .manaCost(1500)
                .processingTime(60)
                .save();

        KoniavacraftMod.LOGGER.debug("Generated {} mana grinder recipes.", 9);
    }
}
