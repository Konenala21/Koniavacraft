package com.github.nalamodikk.common.datagen.recipe.material;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserRecipe;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

public class ManaInfuserRecipeProvider {

    public static void generate(RecipeOutput output, HolderLookup.Provider lookupProvider) {
        generateBasicMaterialRecipes(output);
        generateArmorRecipes(output);
        generateFoodEnhancementRecipes(output);
        generateSpecialItemRecipes(output);
        generateEnchantmentRecipes(output, lookupProvider);
    }

    private static void generateBasicMaterialRecipes(RecipeOutput output) {
        // 鐵錠 → 魔力錠
        createManaInfuserRecipe(output,
                "iron_to_mana_ingot",
                Ingredient.of(Items.IRON_INGOT),
                new ItemStack(ModItems.MANA_INGOT.get()),
                1750, 40, 1
        );

        // 魔力粉 → 濃縮魔力粉
        createManaInfuserRecipe(output,
                "mana_dust_to_condensed",
                Ingredient.of(ModItems.MANA_DUST.get()),
                new ItemStack(ModItems.CONDENSED_MANA_DUST.get()),
                875, 30, 2
        );

        // 濃縮魔力粉 → 精煉魔力粉
        createManaInfuserRecipe(output,
                "condensed_to_refined_mana_dust",
                Ingredient.of(ModItems.CONDENSED_MANA_DUST.get()),
                new ItemStack(ModItems.REFINED_MANA_DUST.get(), 2),
                2625, 60, 1
        );

        // 魔力晶體 → 高密度魔力核
        createManaInfuserRecipe(output,
                "mana_crystal_to_high_density_core",
                Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                new ItemStack(ModItems.HIGH_DENSITY_MANA_CORE.get()),
                8000, 200, 1
        );

        // 精煉魔力粉 → 魔力水晶碎片
        createManaInfuserRecipe(output,
                "refined_mana_dust_to_crystal_fragment",
                Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                new ItemStack(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                4200, 80, 3
        );
    }

    private static void generateArmorRecipes(RecipeOutput output) {
        // 魔力水晶合金粉 × 2 → 魔力合金錠
        createManaInfuserRecipe(output,
                "mana_crystal_alloy_dust_to_ingot",
                Ingredient.of(ModItems.MANA_CRYSTAL_ALLOY_DUST.get()),
                new ItemStack(ModItems.MANA_ALLOY_INGOT.get()),
                4000, 100, 2
        );
    }

    private static void generateFoodEnhancementRecipes(RecipeOutput output) {
        // 蘋果 → 金蘋果
        createManaInfuserRecipe(output,
                "apple_to_golden_apple",
                Ingredient.of(Items.APPLE),
                new ItemStack(Items.GOLDEN_APPLE),
                2800, 60, 1
        );

        // 胡蘿蔔 → 金胡蘿蔔
        createManaInfuserRecipe(output,
                "carrot_to_golden_carrot",
                Ingredient.of(Items.CARROT),
                new ItemStack(Items.GOLDEN_CARROT),
                2100, 50, 1
        );
    }

    private static void generateSpecialItemRecipes(RecipeOutput output) {
        // 石頭 → 石磚
        createManaInfuserRecipe(output,
                "stone_to_stone_bricks",
                Ingredient.of(Items.STONE),
                new ItemStack(Items.STONE_BRICKS),
                700, 25, 1
        );

        // 沙子 → 玻璃
        createManaInfuserRecipe(output,
                "sand_to_glass",
                Ingredient.of(Items.SAND),
                new ItemStack(Items.GLASS),
                525, 30, 1
        );

        // 圓石 → 石頭
        createManaInfuserRecipe(output,
                "cobblestone_to_stone",
                Ingredient.of(Items.COBBLESTONE),
                new ItemStack(Items.STONE),
                350, 20, 1
        );
    }

    private static void generateEnchantmentRecipes(RecipeOutput output, HolderLookup.Provider lookupProvider) {
        // 書 → 附魔書 (Unbreaking I)
        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry = lookupProvider.lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack enchantedBook = EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(enchantmentRegistry.getOrThrow(Enchantments.UNBREAKING), 1)
        );
        createManaInfuserRecipe(output,
                "book_to_enchanted_book",
                Ingredient.of(Items.BOOK),
                enchantedBook,
                14000, 80, 1
        );

        // 經驗瓶 → 更多經驗瓶
        createManaInfuserRecipe(output,
                "experience_bottle_multiplication",
                Ingredient.of(Items.EXPERIENCE_BOTTLE),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 2),
                2100, 45, 1
        );
    }

    private static void createManaInfuserRecipe(RecipeOutput output,
                                                String name,
                                                Ingredient input,
                                                ItemStack result,
                                                int manaCost,
                                                int infusionTime,
                                                int inputCount) {
        ManaInfuserRecipe recipe = new ManaInfuserRecipe(input, result, manaCost, infusionTime, inputCount);
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                KoniavacraftMod.MOD_ID, "mana_infuser/" + name);
        output.accept(recipeId, recipe, null);
    }

    private static void createManaInfuserRecipeWithAdvancement(RecipeOutput output,
                                                               String name,
                                                               Ingredient input,
                                                               ItemStack result,
                                                               int manaCost,
                                                               int infusionTime,
                                                               int inputCount,
                                                               String criterionName,
                                                               Ingredient criterionItem) {
        ManaInfuserRecipe recipe = new ManaInfuserRecipe(input, result, manaCost, infusionTime, inputCount);
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                KoniavacraftMod.MOD_ID, "mana_infuser/" + name);

        ItemStack[] criterionItems = criterionItem.getItems();
        if (criterionItems.length == 0) {
            throw new IllegalArgumentException("Criterion ingredient has no concrete items: " + name);
        }

        Advancement.Builder advancement = output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(criterionName,
                        InventoryChangeTrigger.TriggerInstance.hasItems(criterionItems[0].getItem()))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR);

        output.accept(recipeId, recipe, advancement.build(recipeId.withPrefix("recipes/")));
    }
}
