package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.register.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AltarRecipe implements Recipe<AltarRecipe.AltarInput> {

    private final Ingredient catalyst;
    private final List<Ingredient> ingredients;
    private final ItemStack result;
    private final int manaCost;
    private final int processingTime;
    private final int minTier;

    public AltarRecipe(Ingredient catalyst, List<Ingredient> ingredients,
                       ItemStack result, int manaCost, int processingTime) {
        this(catalyst, ingredients, result, manaCost, processingTime, 0);
    }

    public AltarRecipe(Ingredient catalyst, List<Ingredient> ingredients,
                       ItemStack result, int manaCost, int processingTime, int minTier) {
        this.catalyst = catalyst;
        this.ingredients = ingredients;
        this.result = result;
        this.manaCost = manaCost;
        this.processingTime = processingTime;
        this.minTier = minTier;
    }

    // ── 配方匹配 ─────────────────────────────────────────────────────────────

    @Override
    public boolean matches(AltarInput input, Level level) {
        if (!catalyst.test(input.catalyst())) return false;
        List<ItemStack> nonEmpty = input.pedestalItems().stream()
                .filter(s -> !s.isEmpty()).toList();
        if (nonEmpty.size() < ingredients.size()) return false;
        return matchIngredients(ingredients, nonEmpty);
    }

    /**
     * Returns original indices (into the given items list) that were matched to recipe ingredients.
     * Items list may include empty stacks and extra items beyond what the recipe needs.
     * Returns null if no match is found.
     */
    public int[] findMatchedIndices(List<ItemStack> items) {
        List<Integer> nonEmptyOrig = new ArrayList<>();
        List<ItemStack> nonEmpty = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                nonEmptyOrig.add(i);
                nonEmpty.add(items.get(i));
            }
        }
        if (nonEmpty.size() < ingredients.size()) return null;
        boolean[] used = new boolean[nonEmpty.size()];
        int[] matchedInNonEmpty = new int[ingredients.size()];
        if (!matchWithIndices(ingredients, nonEmpty, used, matchedInNonEmpty, 0)) return null;
        int[] result = new int[ingredients.size()];
        for (int i = 0; i < matchedInNonEmpty.length; i++) {
            result[i] = nonEmptyOrig.get(matchedInNonEmpty[i]);
        }
        return result;
    }

    /** 不限順序匹配底座物品 */
    private static boolean matchIngredients(List<Ingredient> recipe, List<ItemStack> items) {
        boolean[] used = new boolean[items.size()];
        return matchRecursive(recipe, items, used, 0);
    }

    private static boolean matchRecursive(List<Ingredient> recipe, List<ItemStack> items,
                                           boolean[] used, int idx) {
        if (idx == recipe.size()) return true;
        Ingredient ing = recipe.get(idx);
        for (int i = 0; i < items.size(); i++) {
            if (!used[i] && ing.test(items.get(i))) {
                used[i] = true;
                if (matchRecursive(recipe, items, used, idx + 1)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    private static boolean matchWithIndices(List<Ingredient> recipe, List<ItemStack> items,
                                             boolean[] used, int[] matched, int idx) {
        if (idx == recipe.size()) return true;
        Ingredient ing = recipe.get(idx);
        for (int i = 0; i < items.size(); i++) {
            if (!used[i] && ing.test(items.get(i))) {
                used[i] = true;
                matched[idx] = i;
                if (matchWithIndices(recipe, items, used, matched, idx + 1)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(AltarInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Ingredient getCatalyst() { return catalyst; }
    public List<Ingredient> getIngredientList() { return ingredients; }
    public ItemStack getResult() { return result; }
    public int getManaCost() { return manaCost; }
    public int getProcessingTime() { return processingTime; }
    public int getMinTier() { return minTier; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(catalyst);
        list.addAll(ingredients);
        return list;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }

    @Override
    public RecipeSerializer<AltarRecipe> getSerializer() {
        return ModRecipes.ALTAR_SERIALIZER.get();
    }

    @Override
    public RecipeType<AltarRecipe> getType() {
        return ModRecipes.ALTAR_TYPE.get();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    public record AltarInput(ItemStack catalyst, List<ItemStack> pedestalItems) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            if (index == 0) return catalyst;
            int i = index - 1;
            return i < pedestalItems.size() ? pedestalItems.get(i) : ItemStack.EMPTY;
        }
        @Override
        public int size() { return 1 + pedestalItems.size(); }
    }

    // ── Serializer ────────────────────────────────────────────────────────────

    public static class Serializer implements RecipeSerializer<AltarRecipe> {

        public static final MapCodec<AltarRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("catalyst").forGetter(AltarRecipe::getCatalyst),
                Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(AltarRecipe::getIngredientList),
                ItemStack.CODEC.fieldOf("result").forGetter(AltarRecipe::getResult),
                Codec.INT.fieldOf("mana_cost").forGetter(AltarRecipe::getManaCost),
                Codec.INT.fieldOf("processing_time").forGetter(AltarRecipe::getProcessingTime),
                Codec.INT.optionalFieldOf("min_tier", 0).forGetter(AltarRecipe::getMinTier)
        ).apply(inst, AltarRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, AltarRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, AltarRecipe::getCatalyst,
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), AltarRecipe::getIngredientList,
                        ItemStack.STREAM_CODEC, AltarRecipe::getResult,
                        ByteBufCodecs.INT, AltarRecipe::getManaCost,
                        ByteBufCodecs.INT, AltarRecipe::getProcessingTime,
                        ByteBufCodecs.INT, AltarRecipe::getMinTier,
                        AltarRecipe::new
                );

        @Override
        public MapCodec<AltarRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AltarRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
