package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.recipe.MaterialProcessingRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.material.ManaCraftingRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.material.ManaFuelRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.material.ManaInfuserRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.material.ManaPlatePressRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.altar.AltarRecipeProvider;
import com.github.nalamodikk.common.datagen.recipe.processing.ManaGrinderRecipeProvider;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.lookupProvider = registries;
    }


    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        HolderLookup.Provider provider = lookupProvider.join();

        // === 🔥 委派給專門的提供者 ===
        MaterialProcessingRecipeProvider.generate(recipeOutput);  // 處理所有材料加工
        ManaFuelRecipeProvider.generate(recipeOutput);
        ManaCraftingRecipeProvider.generate(recipeOutput);
        ManaInfuserRecipeProvider.generate(recipeOutput, provider);
        ManaGrinderRecipeProvider.generate(recipeOutput);
        ManaPlatePressRecipeProvider.generate(recipeOutput);
        AltarRecipeProvider.generate(recipeOutput);
        // ⚙️ 加工配方（粉碎機、清洗機、富集機）
        ProcessingRecipeProvider.generate(recipeOutput);

        // === 📋 剩餘的主要配方類別 ===
        generateMachineRecipes(recipeOutput);
        generateUpgradeRecipes(recipeOutput);
        generateToolRecipes(recipeOutput);
        generateStorageRecipes(recipeOutput);
        generateMaterialChainRecipes(recipeOutput);
        generateExperimentalRecipes(recipeOutput);
        normalBlock(recipeOutput);
    }


    // === 🧪 材料配方 ===
    private void normalBlock(RecipeOutput output) {
        // 🔆 魔力土壤
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MANA_SOIL.get(), 4)
                .define('D', Blocks.DIRT)
                .define('M', ModItems.MANA_DUST.get())
                .pattern("DD ")
                .pattern("DM ")
                .pattern("   ")
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output);

        // 📖 本源研究桌(本源研究員村民的職業方塊):書架 + 魔力水晶 + 紫水晶
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.ASPECT_RESEARCH_DESK.get())
                .define('B', Items.BOOKSHELF)
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('A', Items.AMETHYST_SHARD)
                .pattern("ACA")
                .pattern("BBB")
                .unlockedBy("has_mana_crystal", has(ModItems.MANA_CRYSTAL.get()))
                .save(output);
    }

    // === 🏭 機器配方 ===
    private void generateMachineRecipes(RecipeOutput output) {
        // 🔆 太陽能魔力收集器
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SOLAR_MANA_COLLECTOR.get())
                .pattern("GGG")
                .pattern("GMG")
                .pattern("III")
                .define('G', Items.GLASS)
                .define('M', ModItems.MANA_DUST.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "solar_mana_collector");

        // 🌵 訓練假人：仙人掌身體 + 魔力塵填充 + 乾草底座，測傷害用
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRAINING_DUMMY.get())
                .pattern(" C ")
                .pattern("CMC")
                .pattern("HHH")
                .define('C', Items.CACTUS)
                .define('M', ModItems.MANA_DUST.get())
                .define('H', Items.HAY_BLOCK)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "training_dummy");

        // 🔗 基礎奧術導管 (舊 arcane_conduit 配方轉移至基礎版)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BASIC_ARCANE_CONDUIT.get(), 8)
                .pattern("MMM")
                .pattern("IGI")
                .pattern("MMM")
                .define('M', ModItems.MANA_DUST.get())
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "basic_arcane_conduit");

        // 進階/精英導管已移至祭壇配方（altar recipe），需要升級環 T1/T3

        // 🔥 魔力發電機
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_GENERATOR.get())
                .pattern("AAA")
                .pattern("RMR")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE_BLOCK)
                .define('M', Items.DIAMOND_BLOCK)
                .define('F', Blocks.FURNACE)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output, "mana_generator");

        // 🧪 魔力合成台
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get())
                .pattern("GMG")
                .pattern("RCR")
                .pattern("IMI")
                .define('G', Items.GLASS)
                .define('M', ModItems.MANA_DUST.get())
                .define('R', Items.REDSTONE)
                .define('C', Items.CRAFTING_TABLE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "mana_crafting_table");

        // ⚙️ 魔力粉碎機
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_GRINDER.get())
                .pattern("SCS")
                .pattern("GIG")
                .pattern("RMR")
                .define('S', Items.STONE)
                .define('C', ModBlocks.BASIC_ARCANE_CONDUIT.get())
                .define('G', Items.IRON_INGOT)
                .define('I', ModItems.MANA_INGOT.get())
                .define('R', ModItems.REFINED_MANA_DUST.get())
                .define('M', Blocks.PISTON)
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "mana_grinder");

        // 📖 研究台
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RESEARCH_TABLE.get())
                .pattern("MCM")
                .pattern("SIS")
                .pattern("MCM")
                .define('M', ModItems.MANA_INGOT.get())
                .define('C', ItemTags.PLANKS)
                .define('S', Items.STICK)
                .define('I', Blocks.IRON_BLOCK)
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "research_table");

        // 🔩 魔力壓板機
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_PLATE_PRESS.get())
                .pattern("IMI")
                .pattern("BCB")
                .pattern("IMI")
                .define('I', ModItems.MANA_ALLOY_INGOT.get())
                .define('M', ModItems.MANA_INGOT.get())
                .define('B', Blocks.IRON_BLOCK)
                .define('C', ModBlocks.BASIC_ARCANE_CONDUIT.get())
                .unlockedBy("has_mana_alloy_ingot", has(ModItems.MANA_ALLOY_INGOT.get()))
                .save(output, "mana_plate_press");

        // 🤖 魔力部署器
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_DEPLOYER.get())
                .pattern("MIM")
                .pattern("CDC")
                .pattern("MIM")
                .define('M', ModItems.MANA_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .define('C', ModBlocks.BASIC_ARCANE_CONDUIT.get())
                .define('D', Blocks.DISPENSER)
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "mana_deployer");
    }

    // === ⚡ 升級模組配方 ===
    private void generateUpgradeRecipes(RecipeOutput output) {
        // ⚡ 速度升級模組
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SPEED_UPGRADE.get())
                .pattern("RMR")
                .pattern("MGM")
                .pattern("RMR")
                .define('R', Items.REDSTONE)
                .define('M', ModItems.MANA_DUST.get())
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "speed_upgrade");

        // 🔋 效率升級模組
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EFFICIENCY_UPGRADE.get())
                .pattern("DMD")
                .pattern("MIM")
                .pattern("DMD")
                .define('D', Items.DIAMOND)
                .define('M', ModItems.MANA_DUST.get())
                .define('I', ModItems.MANA_INGOT.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "efficiency_upgrade");

        // ⚡ 加速處理升級
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ACCELERATED_PROCESSING_UPGRADE.get())
                .pattern("RIR")
                .pattern("ICI")
                .pattern("RIR")
                .define('R', Items.REDSTONE)
                .define('I', ModItems.MANA_INGOT.get())
                .define('C', ModItems.CONDENSED_MANA_DUST.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "accelerated_processing_upgrade");

        // 🪣 擴充燃料室升級
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EXPANDED_FUEL_CHAMBER_UPGRADE.get())
                .pattern("IMI")
                .pattern("MCM")
                .pattern("IMI")
                .define('I', Items.IRON_INGOT)
                .define('M', ModItems.MANA_DUST.get())
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "expanded_fuel_chamber_upgrade");

        // 🧪 催化轉換升級
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CATALYTIC_CONVERTER_UPGRADE.get())
                .pattern("GRG")
                .pattern("RNR")
                .pattern("GRG")
                .define('G', Items.GOLD_INGOT)
                .define('R', ModItems.REFINED_MANA_DUST.get())
                .define('N', ModItems.MANA_INGOT.get())
                .unlockedBy("has_refined_mana_dust", has(ModItems.REFINED_MANA_DUST.get()))
                .save(output, "catalytic_converter_upgrade");

        // 🔍 診斷顯示升級
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DIAGNOSTIC_DISPLAY_UPGRADE.get())
                .pattern("GQG")
                .pattern("QMQ")
                .pattern("GQG")
                .define('G', Items.GLASS)
                .define('Q', Items.QUARTZ)
                .define('M', ModItems.MANA_DUST.get())
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "diagnostic_display_upgrade");
    }


    // === 🔧 工具配方 ===
    private void generateToolRecipes(RecipeOutput output) {
        // 💎 魔力水晶（4 碎片 → 1 完整）
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MANA_CRYSTAL.get())
                .pattern("FF")
                .pattern("FF")
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .unlockedBy("has_mana_crystal_fragment", has(ModItems.MANA_CRYSTAL_FRAGMENT.get()))
                .save(output, "mana_crystal");

        // ⛏️ 魔力鎬
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MANA_PICKAXE.get())
                .pattern("CCC")
                .pattern(" S ")
                .pattern(" S ")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('S', Items.STICK)
                .unlockedBy("has_mana_crystal", has(ModItems.MANA_CRYSTAL.get()))
                .save(output, "mana_pickaxe");

        // 🖊️ 墨水羽毛筆
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INK_QUILL.get())
                .pattern(" F ")
                .pattern(" I ")
                .pattern(" S ")
                .define('F', Items.FEATHER)
                .define('I', Items.INK_SAC)
                .define('S', Items.STICK)
                .unlockedBy("has_feather", has(Items.FEATHER))
                .save(output, "ink_quill");

        // 🖊️ 墨水羽毛筆補充（無序：墨水羽毛筆 + 墨囊 → 全新墨水羽毛筆）
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.INK_QUILL.get())
                .requires(ModItems.INK_QUILL.get())
                .requires(Items.INK_SAC)
                .unlockedBy("has_ink_quill", has(ModItems.INK_QUILL.get()))
                .save(output, "ink_quill_refill");

        // 🔭 娜拉全息手錶（研究系統入口）
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NARA_WATCH.get())
                .pattern("GCG")
                .pattern("CMC")
                .pattern("GCG")
                .define('G', Items.GOLD_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('M', ModItems.MANA_DUST.get())
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "nara_watch");
    }

    // === 📦 儲存和轉換配方 ===
    private void generateStorageRecipes(RecipeOutput output) {
        // 魔力粉 ↔ 魔力錠
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MANA_INGOT.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ModItems.MANA_DUST.get())
                .unlockedBy("has_mana_dust", has(ModItems.MANA_DUST.get()))
                .save(output, "mana_ingot_from_dust");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get(), 9)
                .requires(ModItems.MANA_INGOT.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "mana_dust_from_ingot");

        // 魔力錠 ↔ 魔力方塊
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_BLOCK.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ModItems.MANA_INGOT.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "mana_block_from_ingots");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_INGOT.get(), 9)
                .requires(ModBlocks.MANA_BLOCK.get())
                .unlockedBy("has_mana_block", has(ModBlocks.MANA_BLOCK.get()))
                .save(output, "mana_ingot_from_block");
    }


    // === 🧪 實驗性/示例配方 ===
    private void generateMaterialChainRecipes(RecipeOutput output) {
        // 鐵錠 + 魔力錠 → 魔力鐵（無序合成）
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_IRON.get())
                .requires(Items.IRON_INGOT)
                .requires(ModItems.MANA_INGOT.get())
                .unlockedBy("has_mana_ingot", has(ModItems.MANA_INGOT.get()))
                .save(output, "mana_iron");
    }

    private void generateExperimentalRecipes(RecipeOutput output) {
        // 🌟 切石機示例
        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(Items.ANDESITE),
                        RecipeCategory.BUILDING_BLOCKS,
                        Items.ANDESITE_SLAB, 2)
                .unlockedBy("has_andesite", has(Items.ANDESITE))
                .save(output, KoniavacraftMod.MOD_ID + ":andesite_slab_stonecutting");

        // 🌟 鍛造台示例
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(Items.DIAMOND_AXE),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.TOOLS,
                        Items.NETHERITE_AXE)
                .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                .save(output, KoniavacraftMod.MOD_ID + ":netherite_axe_smithing_example");
    }

    // === 🛠️ 工具方法 ===
    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> ingredients,
                                      RecipeCategory category, ItemLike result, float xp, int time, String group) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new,
                ingredients, category, result, xp, time, group, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> ingredients,
                                      RecipeCategory category, ItemLike result, float xp, int time, String group) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new,
                ingredients, category, result, xp, time, group, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput,
                                                                       RecipeSerializer<T> serializer,
                                                                       AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> ingredients,
                                                                       RecipeCategory category,
                                                                       ItemLike result,
                                                                       float xp, int time,
                                                                       String group, String suffix) {
        for (ItemLike item : ingredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(item), category, result, xp, time, serializer, factory)
                    .group(group)
                    .unlockedBy(getHasName(item), has(item))
                    .save(recipeOutput, KoniavacraftMod.MOD_ID + ":" + getItemName(result) + suffix + "_" + getItemName(item));
        }
    }

}
