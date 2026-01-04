package com.github.nalamodikk.common.coreapi.recipe;

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

/**
 * 🔄 通用加工配方類
 *
 * 支援：
 * - 多輸入（最多 9 個）
 * - 多輸出（主輸出 + 副輸出）
 * - 概率輸出
 * - 機器類型過濾（grinder, washer, enricher 等）
 * - 魔力消耗
 * - 處理時間
 */
public class ProcessingRecipe implements Recipe<ProcessingRecipe.ProcessingInput> {

    // === 📦 配方數據 ===
    private final NonNullList<Ingredient> inputs;           // 多個輸入物品要求
    private final ItemStack mainOutput;                     // 主輸出物品
    private final List<ChanceOutput> chanceOutputs;         // 概率輸出
    private final int manaCost;                             // 魔力消耗
    private final int processingTime;                       // 處理時間 (ticks)
    private final String machineType;                       // 機器類型 (grinder, washer, enricher, etc)

    public ProcessingRecipe(
            NonNullList<Ingredient> inputs,
            ItemStack mainOutput,
            List<ChanceOutput> chanceOutputs,
            int manaCost,
            int processingTime,
            String machineType
    ) {
        this.inputs = inputs;
        this.mainOutput = mainOutput;
        this.chanceOutputs = chanceOutputs != null ? chanceOutputs : new ArrayList<>();
        this.manaCost = manaCost;
        this.processingTime = processingTime;
        this.machineType = machineType;
    }

    // === 🔍 配方匹配邏輯 ===

    @Override
    public boolean matches(ProcessingInput input, Level level) {
        // 檢查機器類型是否匹配
        if (!input.getMachineType().equals(this.machineType)) {
            return false;
        }

        // 檢查輸入數量是否足夠
        if (input.size() < this.inputs.size()) {
            return false;
        }

        // 檢查每個輸入是否匹配
        for (int i = 0; i < this.inputs.size(); i++) {
            ItemStack inputStack = input.getItem(i);
            Ingredient ingredient = this.inputs.get(i);

            if (!ingredient.test(inputStack)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(ProcessingInput input, HolderLookup.Provider registries) {
        return this.mainOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // 加工機不受尺寸限制
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.mainOutput;
    }

    // === 📊 配方屬性 ===

    public NonNullList<Ingredient> getInputs() {
        return inputs;
    }

    public ItemStack getMainOutput() {
        return mainOutput;
    }

    public List<ChanceOutput> getChanceOutputs() {
        return chanceOutputs;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public String getMachineType() {
        return machineType;
    }

    // === 🏷️ 配方元數據 ===

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.PROCESSING_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.inputs;
    }

    // === 📦 輸入容器類 ===

    /**
     * 🔧 加工機的輸入容器
     */
    public static class ProcessingInput implements RecipeInput {
        private final List<ItemStack> inputs;
        private final String machineType;

        public ProcessingInput(List<ItemStack> inputs, String machineType) {
            this.inputs = inputs;
            this.machineType = machineType;
        }

        public String getMachineType() {
            return machineType;
        }

        @Override
        public ItemStack getItem(int index) {
            return index < inputs.size() ? inputs.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return inputs.size();
        }
    }

    // === 🎲 概率輸出類 ===

    /**
     * 表示有概率的輸出物品
     */
    public static class ChanceOutput {
        private final ItemStack output;
        private final float chance;  // 0.0 ~ 1.0

        public ChanceOutput(ItemStack output, float chance) {
            this.output = output;
            this.chance = Math.max(0.0f, Math.min(1.0f, chance));
        }

        public ItemStack getOutput() {
            return output;
        }

        public float getChance() {
            return chance;
        }

        /**
         * 根據隨機數決定是否應該輸出
         */
        public boolean shouldOutput(float randomValue) {
            return randomValue < chance;
        }
    }

    // === 📝 序列化器 ===

    /**
     * 🔧 加工配方序列化器
     */
    public static class Serializer implements RecipeSerializer<ProcessingRecipe> {

        // MapCodec 用於 JSON 序列化
        private static final MapCodec<ProcessingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        // 輸入物品（最多 9 個）
                        Ingredient.CODEC_NONEMPTY.listOf().fieldOf("inputs")
                                .forGetter(recipe -> recipe.inputs.stream().toList()),

                        // 主輸出物品
                        ItemStack.STRICT_CODEC.fieldOf("main_output")
                                .forGetter(recipe -> recipe.mainOutput),

                        // 概率輸出（可選）
                        ChanceOutputCodec.CODEC.listOf()
                                .optionalFieldOf("chance_outputs", new ArrayList<>())
                                .forGetter(recipe -> recipe.chanceOutputs),

                        // 魔力消耗
                        Codec.INT.fieldOf("mana_cost").forGetter(recipe -> recipe.manaCost),

                        // 處理時間
                        Codec.INT.optionalFieldOf("processing_time", 200)
                                .forGetter(recipe -> recipe.processingTime),

                        // 機器類型
                        Codec.STRING.fieldOf("machine_type").forGetter(recipe -> recipe.machineType)
                ).apply(instance, (inputs, output, chances, mana, time, type) -> {
                    NonNullList<Ingredient> ingredientList = NonNullList.create();
                    ingredientList.addAll(inputs);
                    return new ProcessingRecipe(ingredientList, output, chances, mana, time, type);
                })
        );

        // StreamCodec 用於網路序列化
        private static final StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        // 輸入物品
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).map(
                                list -> {
                                    NonNullList<Ingredient> result = NonNullList.create();
                                    result.addAll(list);
                                    return result;
                                },
                                ing -> ing.stream().toList()
                        ),
                        ProcessingRecipe::getInputs,

                        // 主輸出物品
                        ItemStack.STREAM_CODEC,
                        ProcessingRecipe::getMainOutput,

                        // 概率輸出
                        ChanceOutputCodec.STREAM_CODEC.apply(ByteBufCodecs.list()),
                        ProcessingRecipe::getChanceOutputs,

                        // 魔力消耗
                        ByteBufCodecs.VAR_INT,
                        ProcessingRecipe::getManaCost,

                        // 處理時間
                        ByteBufCodecs.VAR_INT,
                        ProcessingRecipe::getProcessingTime,

                        // 機器類型
                        ByteBufCodecs.STRING_UTF8,
                        ProcessingRecipe::getMachineType,

                        ProcessingRecipe::new
                );

        @Override
        public MapCodec<ProcessingRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    // === 🎲 概率輸出的 Codec ===

    /**
     * 用於序列化 ChanceOutput
     */
    public static class ChanceOutputCodec {
        public static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ItemStack.STRICT_CODEC.fieldOf("item").forGetter(ChanceOutput::getOutput),
                        Codec.FLOAT.fieldOf("chance").forGetter(ChanceOutput::getChance)
                ).apply(instance, ChanceOutput::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStack.STREAM_CODEC,
                        ChanceOutput::getOutput,
                        ByteBufCodecs.FLOAT,
                        ChanceOutput::getChance,
                        ChanceOutput::new
                );
    }
}
