package com.github.nalamodikk.common.block.blockentity.mana_generator.recipe;

import com.github.nalamodikk.register.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;


public class ManaGenFuelRecipe implements Recipe<RecipeInput> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int manaRate;
    private final int energyRate;
    private final int burnTime;

    public ManaGenFuelRecipe(ResourceLocation id, Ingredient ingredient, int manaRate, int energyRate, int burnTime) {
        this.id = id;
        this.ingredient = ingredient;
        this.manaRate = manaRate;
        this.energyRate = energyRate;
        this.burnTime = burnTime;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.ingredient);
    }



    // ===== Getter for Codec & Game Access =====
    public ResourceLocation getId() {
        return id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getManaRate() {
        return manaRate;
    }

    public int getEnergyRate() {
        return energyRate;
    }

    public int getBurnTime() {
        return burnTime;
    }

    // ===== Recipe Logic =====

    @Override
    public boolean matches(RecipeInput container, Level level) {
        for (int i = 0; i < container.size(); i++) {
            if (ingredient.test(container.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput container, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MANA_FUEL_SERIALIZER.get(); // ← 綁定註冊物件
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MANA_FUEL_TYPE.get(); // ← 同上
    }


    // ===== Serializer Inner Class =====

    public static class FuelRecipeSerializer implements RecipeSerializer<ManaGenFuelRecipe> {
        public static final FuelRecipeSerializer INSTANCE = new FuelRecipeSerializer();

        public static final MapCodec<ManaGenFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(ManaGenFuelRecipe::getId),
                        Ingredient.CODEC.fieldOf("ingredient").forGetter(ManaGenFuelRecipe::getIngredient),
                        Codec.INT.fieldOf("mana").forGetter(ManaGenFuelRecipe::getManaRate),
                        Codec.INT.fieldOf("energy").forGetter(ManaGenFuelRecipe::getEnergyRate),
                        Codec.INT.optionalFieldOf("burn_time", 200).forGetter(ManaGenFuelRecipe::getBurnTime)
                ).apply(inst, ManaGenFuelRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ManaGenFuelRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, ManaGenFuelRecipe::getId,
                        Ingredient.CONTENTS_STREAM_CODEC, ManaGenFuelRecipe::getIngredient,
                        ByteBufCodecs.INT, ManaGenFuelRecipe::getManaRate,
                        ByteBufCodecs.INT, ManaGenFuelRecipe::getEnergyRate,
                        ByteBufCodecs.INT, ManaGenFuelRecipe::getBurnTime,
                        ManaGenFuelRecipe::new
                );

        @Override
        public MapCodec<ManaGenFuelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ManaGenFuelRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }


}
