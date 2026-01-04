package com.github.nalamodikk.common.block.blockentity.mana_generator.recipe;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public class ManaGenFuelRecipeBuilder implements RecipeBuilder {
    public static final String FUEL_RECIPE_PATH_PREFIX = "mana_recipes/mana_fuel/";

    private final Ingredient ingredient;
    private final int manaRate;
    private final int energyRate;
    private final int burnTime;

    public ManaGenFuelRecipeBuilder(Ingredient ingredient, int manaRate, int energyRate, int burnTime) {
        this.ingredient = ingredient;
        this.manaRate = manaRate;
        this.energyRate = energyRate;
        this.burnTime = burnTime;
    }

    public static ManaGenFuelRecipeBuilder create(ItemLike item, int manaRate, int energyRate, int burnTime) {
        return new ManaGenFuelRecipeBuilder(Ingredient.of(item), manaRate, energyRate, burnTime);
    }
    public static ManaGenFuelRecipeBuilder create(Item item, int manaRate, int energyRate, int burnTime) {
        return create((ItemLike) item, manaRate, energyRate, burnTime);
    }

    @Override
    public void save(RecipeOutput output) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            throw new IllegalStateException("[DataGen] ❌ FuelRecipeBuilder ingredient 為空，無法決定輸出物品！");
        }

        Item resultItem = stacks[0].getItem();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                KoniavacraftMod.MOD_ID,
                FUEL_RECIPE_PATH_PREFIX + BuiltInRegistries.ITEM.getKey(resultItem).getPath() + "_fuel"
        );

        save(output, id);
    }


    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(id, new ManaGenFuelRecipe(id, ingredient, manaRate, energyRate, burnTime), null);
    }

    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        return this; // 可選功能
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this; // 可選功能
    }

    @Override
    public Item getResult() {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            throw new IllegalStateException("ManaGenFuelRecipeBuilder: ingredient is empty, cannot determine result item!");
        }
        return stacks[0].getItem();
    }
}

