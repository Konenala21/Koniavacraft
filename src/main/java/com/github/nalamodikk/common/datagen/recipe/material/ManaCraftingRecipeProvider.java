package com.github.nalamodikk.common.datagen.recipe.material;

import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.recipe.ManaCraftingRecipeBuilder;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManaCraftingRecipeProvider {
    public static ManaCraftingTableRecipe shaped(ResourceLocation id, List<String> pattern, Map<String, Ingredient> stringKey, ItemStack result, int manaCost) {
        // 把 String 轉成 Character
        Map<Character, Ingredient> charKey = new HashMap<>();
        for (Map.Entry<String, Ingredient> entry : stringKey.entrySet()) {
            String keyStr = entry.getKey();
            if (keyStr.length() != 1) {
                throw new IllegalArgumentException("Key must be single character, got: '" + keyStr + "'");
            }
            charKey.put(keyStr.charAt(0), entry.getValue());
        }

        return ManaCraftingTableRecipe.createShaped(pattern, charKey, result, manaCost).withId(id);
    }


    public static void generate(RecipeOutput output) {
        // ✅ 無序合成配方
        ManaCraftingRecipeBuilder.create(ModItems.MANA_DUST.get(), 1)
                .addIngredient(Ingredient.of(Items.DIAMOND))
                .manaCost(1500)
                .save(output);


        // 本源底座
        ManaCraftingRecipeBuilder.create(ModBlocks.ASPECT_PEDESTAL.get(), 1)
                .shaped(true)
                .pattern(" F ")
                .pattern("FMF")
                .pattern("MMM")
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('M', ModBlocks.MANA_BLOCK.get())
                .manaCost(1500)
                .save(output, "aspect_pedestal");

        // 本源矩陣核心
        ManaCraftingRecipeBuilder.create(ModBlocks.ASPECT_ALTAR.get(), 1)
                .shaped(true)
                .pattern("FGF")
                .pattern("GCG")
                .pattern("FGF")
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('G', ModItems.MANA_INGOT.get())
                .define('C', ModItems.MANA_CRYSTAL.get())
                .manaCost(4000)
                .save(output, "aspect_altar");

        // ⑤ 基礎魔力電路板
        // 排列：D=導線, C=晶片, W=黏膠, S=基板
        //   D C D
        //   W S W
        //   D C D
        ManaCraftingRecipeBuilder.create(ModItems.BASIC_MANA_CIRCUIT.get(), 1)
                .shaped(true)
                .pattern("DCD")
                .pattern("WSW")
                .pattern("DCD")
                .define('D', ModItems.MANA_WIRE.get())
                .define('C', ModItems.MANA_WAFER.get())
                .define('W', ModItems.MANA_ADHESIVE.get())
                .define('S', ModItems.MANA_SUBSTRATE.get())
                .manaCost(3000)
                .save(output, "basic_mana_circuit");

        ManaCraftingRecipeBuilder.create(ModBlocks.MANA_INFUSER.get(), 1)
                .shaped(true)  // 明確設置為有序
                .pattern("RGR")
                .pattern("MIM")
                .pattern("CDC")
                .define('R', Items.REDSTONE_BLOCK)
                .define('G', Items.GLASS)
                .define('M', ModItems.MANA_DUST.get())
                .define('I', Items.IRON_BLOCK)
                .define('C', ModItems.REFINED_MANA_DUST.get())
                .define('D', Items.DIAMOND)
                .manaCost(3500)
                .save(output, "mana_infuser_machine");


        // ✅ 有序合成配方
//        ManaCraftingRecipeBuilder.create(ModItems.MANA_STAFF.get(), 1)
//                .pattern(" A ")
//                .pattern(" B ")
//                .pattern(" C ")
//                .define('A', Items.IRON_INGOT)
//                .define('B', Items.STICK)
//                .define('C', Items.DIAMOND)
//                .manaCost(2000)
//                .save(output);

    }

}
