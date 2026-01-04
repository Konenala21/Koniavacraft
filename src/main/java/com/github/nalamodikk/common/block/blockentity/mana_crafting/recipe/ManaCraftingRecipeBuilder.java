package com.github.nalamodikk.common.block.blockentity.mana_crafting.recipe;

import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableRecipe;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManaCraftingRecipeBuilder implements RecipeBuilder {
    public static final String CRAFTING_RECIPE_PATH_PREFIX = "mana_recipes/mana_crafting/";

    private final NonNullList<Ingredient> ingredients = NonNullList.create();
    private final ItemStack result;
    private int manaCost = 0;
    private boolean shaped = false;
    private final List<String> pattern = new ArrayList<>();
    private final Map<Character, Ingredient> key = new HashMap<>();

    public ManaCraftingRecipeBuilder(ItemStack result) {
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("❌ 無效的配方輸出：ItemStack 不能為 null 或 EMPTY！");
        }
        this.result = result;
    }
    // ✅ 語法糖：支援 Ingredient 直接 define
    public ManaCraftingRecipeBuilder define(char symbol, Ingredient ingredient) {
        if (symbol == ' ')
            throw new IllegalArgumentException("Cannot redefine empty space");
        this.key.put(symbol, ingredient);
        return this;
    }

    // ✅ 保留原本主方法：定義 item 自動轉換成 Ingredient
    public ManaCraftingRecipeBuilder define(char symbol, ItemLike item) {
        return define(symbol, Ingredient.of(item)); // ✅ 呼叫上面的版本
    }



    public static ManaCraftingRecipeBuilder shaped(ItemLike item, int count) {
        return create(item, count).shaped(true);
    }

    public static ManaCraftingRecipeBuilder shaped(ItemStack stack) {
        return create(stack).shaped(true);
    }

    public static ManaCraftingRecipeBuilder shapeless(ItemLike item, int count) {
        return create(item, count).shaped(false);
    }

    public static ManaCraftingRecipeBuilder shapeless(ItemStack stack) {
        return create(stack).shaped(false);
    }


    public static ManaCraftingRecipeBuilder create(ItemLike item, int count) {
        return new ManaCraftingRecipeBuilder(new ItemStack(item, count));
    }

    public static ManaCraftingRecipeBuilder create(ItemStack result) {
        return new ManaCraftingRecipeBuilder(result);
    }

    public ManaCraftingRecipeBuilder pattern(String patternLine) {
        if (patternLine.length() > 3)
            throw new IllegalArgumentException("Pattern too long (max 3 chars): " + patternLine);
        if (pattern.size() >= 3)
            throw new IllegalStateException("Too many pattern lines (max 3)");
        this.pattern.add(patternLine);
        return this;
    }


    public ManaCraftingRecipeBuilder manaCost(int cost) {
        this.manaCost = cost;
        return this;
    }

    public ManaCraftingRecipeBuilder shaped(boolean shaped) {
        this.shaped = shaped;
        return this;
    }

    public ManaCraftingRecipeBuilder addIngredient(Ingredient ingredient) {
        this.ingredients.add(ingredient);
        return this;
    }

    public ManaCraftingRecipeBuilder addIngredients(Iterable<Ingredient> list) {
        list.forEach(this.ingredients::add);
        return this;
    }

    public void save(RecipeOutput output) {
        if (!shaped && ingredients.isEmpty()) {
            throw new IllegalStateException("[DataGen] ❌ ManaCraftingRecipeBuilder ingredients 為空，無法儲存配方！");
        }

        Item item = result.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        if (itemId == null || itemId.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalStateException("[DataGen] ❌ 結果物品尚未註冊！Item: " + item);
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                ModRecipes.MANA_CRAFTING_SERIALIZER.getId().getNamespace(),
                CRAFTING_RECIPE_PATH_PREFIX + itemId.getPath()
        );

        save(output, id);
    }

    public void save(RecipeOutput output, @Nullable String customName) {
        Item item = result.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        if (itemId == null || itemId.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalStateException("[DataGen] ❌ 結果物品尚未註冊！Item: " + item);
        }

        String path = customName != null ? customName : itemId.getPath();

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                ModRecipes.MANA_CRAFTING_SERIALIZER.getId().getNamespace(),
                CRAFTING_RECIPE_PATH_PREFIX + path
        );

        save(output, id);
    }


    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(id, buildRecipe(id), null);
    }

    public ManaCraftingTableRecipe buildRecipe(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("❌ buildRecipe: 你不能傳入 null 的配方 ID！");
        }

        ManaCraftingTableRecipe recipe = shaped
                ? ManaCraftingTableRecipe.createShaped(pattern, key, result, manaCost)
                : ManaCraftingTableRecipe.createShapeless(ingredients, result, manaCost);
        return recipe.withId(id);
    }



    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        return this; // optional
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this; // optional
    }

    @Override
    public Item getResult() {
        return result.getItem();
    }
}
