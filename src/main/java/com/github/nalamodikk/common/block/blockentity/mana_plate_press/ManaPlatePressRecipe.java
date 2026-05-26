package com.github.nalamodikk.common.block.blockentity.mana_plate_press;

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

public class ManaPlatePressRecipe implements Recipe<ManaPlatePressRecipe.PlatePressInput> {

    private final Ingredient input;
    private final ItemStack result;
    private final int manaCost;
    private final int pressingTime;
    private final int inputCount;

    public ManaPlatePressRecipe(Ingredient input, ItemStack result, int manaCost, int pressingTime, int inputCount) {
        this.input = input;
        this.result = result;
        this.manaCost = manaCost;
        this.pressingTime = pressingTime;
        this.inputCount = inputCount;
    }

    @Override
    public boolean matches(PlatePressInput input, Level level) {
        ItemStack inputStack = input.getInputStack();
        return this.input.test(inputStack) && inputStack.getCount() >= this.inputCount;
    }

    @Override
    public ItemStack assemble(PlatePressInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    public Ingredient getInput() { return input; }
    public ItemStack getResult() { return result; }
    public int getManaCost() { return manaCost; }
    public int getPressingTime() { return pressingTime; }
    public int getInputCount() { return inputCount; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PLATE_PRESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.PLATE_PRESS_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input);
        return ingredients;
    }

    public static class PlatePressInput implements RecipeInput {
        private final ItemStack inputStack;

        public PlatePressInput(ItemStack inputStack) {
            this.inputStack = inputStack;
        }

        public ItemStack getInputStack() { return inputStack; }

        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? inputStack : ItemStack.EMPTY;
        }

        @Override
        public int size() { return 1; }
    }

    public static class Serializer implements RecipeSerializer<ManaPlatePressRecipe> {

        private static final MapCodec<ManaPlatePressRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                        Codec.INT.fieldOf("mana_cost").forGetter(r -> r.manaCost),
                        Codec.INT.optionalFieldOf("pressing_time", 80).forGetter(r -> r.pressingTime),
                        Codec.INT.optionalFieldOf("input_count", 1).forGetter(r -> r.inputCount)
                ).apply(instance, ManaPlatePressRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, ManaPlatePressRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, r -> r.input,
                        ItemStack.STREAM_CODEC, r -> r.result,
                        ByteBufCodecs.VAR_INT, r -> r.manaCost,
                        ByteBufCodecs.VAR_INT, r -> r.pressingTime,
                        ByteBufCodecs.VAR_INT, r -> r.inputCount,
                        ManaPlatePressRecipe::new
                );

        @Override
        public MapCodec<ManaPlatePressRecipe> codec() { return MAP_CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ManaPlatePressRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
