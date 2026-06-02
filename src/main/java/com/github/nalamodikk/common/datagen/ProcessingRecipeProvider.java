package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚙️ 加工配方數據生成器（靜態工具類）
 *
 * 此類提供靜態方法來生成所有加工配方的 JSON 文件
 * 由 ModRecipeProvider 的 buildRecipes() 調用
 * 輸出到 src/generated/resources/data/koniava/recipes/
 */
public class ProcessingRecipeProvider {
    /**
     * 🔧 生成所有加工配方
     *
     * 由 ModRecipeProvider.buildRecipes() 調用
     */
    public static void generate(RecipeOutput output) {
        // ============================================
        // ✨ 富集機配方（Enricher Recipes）
        // ============================================

        // 沙粒 → 土
        createProcessingRecipe(output, "enricher/sand_enrich", "enricher")
                .input(Items.SAND)
                .output(Items.DIRT, 1)
                .manaCost(60)
                .processingTime(120)
                .save();

        KoniavacraftMod.LOGGER.debug("Generated 2 processing recipes.");
    }

    /**
     * 🔧 配方構建器輔助方法
     */
    public static ProcessingRecipeHelper createProcessingRecipe(RecipeOutput output, String name, String machineType) {
        return new ProcessingRecipeHelper(output, name, machineType);
    }

    /**
     * 🔨 ProcessingRecipe 的輔助構建器
     */
    public static class ProcessingRecipeHelper {
        private final RecipeOutput output;
        private final String name;
        private final String machineType;
        private final List<Ingredient> inputs = new ArrayList<>();
        private ItemStack mainOutput = ItemStack.EMPTY;
        private final List<ProcessingRecipe.ChanceOutput> chanceOutputs = new ArrayList<>();
        private int manaCost = 0;
        private int processingTime = 200;

        public ProcessingRecipeHelper(RecipeOutput output, String name, String machineType) {
            this.output = output;
            this.name = name;
            this.machineType = machineType;
        }

        public ProcessingRecipeHelper input(ItemLike item) {
            this.inputs.add(Ingredient.of(item));
            return this;
        }

        public ProcessingRecipeHelper output(ItemLike item, int count) {
            this.mainOutput = new ItemStack(item, count);
            return this;
        }

        public ProcessingRecipeHelper chanceOutput(ItemLike item, int count, float chance) {
            this.chanceOutputs.add(new ProcessingRecipe.ChanceOutput(
                    new ItemStack(item, count), chance));
            return this;
        }

        public ProcessingRecipeHelper manaCost(int cost) {
            this.manaCost = cost;
            return this;
        }

        public ProcessingRecipeHelper processingTime(int ticks) {
            this.processingTime = ticks;
            return this;
        }

        public void save() {
            // 建立 NonNullList
            net.minecraft.core.NonNullList<Ingredient> ingredientList = net.minecraft.core.NonNullList.create();
            ingredientList.addAll(inputs);

            // 建立配方物件
            ProcessingRecipe recipe = new ProcessingRecipe(
                    ingredientList,
                    mainOutput,
                    chanceOutputs,
                    manaCost,
                    processingTime,
                    machineType
            );

            // 建立配方ID
            ResourceLocation recipeId =
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, name);

            // 輸出到 JSON（暫不包含advancement）
            this.output.accept(recipeId, recipe, null);
        }
    }
}
