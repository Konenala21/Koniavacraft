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

        // 魔力注入機（需魔力工作台，避免 mana_crystal 循環依賴）
        ManaCraftingRecipeBuilder.create(ModBlocks.MANA_INFUSER.get(), 1)
                .shaped(true)
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

        // 魔力砲管
        ManaCraftingRecipeBuilder.create(ModItems.MANA_BARREL.get(), 2)
                .shaped(true)
                .pattern("IWI")
                .pattern("W W")
                .pattern("IWI")
                .define('I', ModItems.MANA_INGOT.get())
                .define('W', ModItems.MANA_WIRE.get())
                .manaCost(1500)
                .save(output, "mana_barrel");

        // 精密魔力迴路
        ManaCraftingRecipeBuilder.create(ModItems.PRECISION_MANA_CIRCUIT.get(), 1)
                .shaped(true)
                .pattern("FRF")
                .pattern("CVC")
                .pattern("FRF")
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('R', ModItems.REFINED_MANA_DUST.get())
                .define('C', ModItems.BASIC_MANA_CIRCUIT.get())
                .define('V', ModItems.MANA_WAFER.get())
                .manaCost(3000)
                .save(output, "precision_mana_circuit");

        // 魔力充能台（前祭壇路徑：注魔台 + 研磨機材料）
        ManaCraftingRecipeBuilder.create(ModBlocks.MANA_CHARGER.get(), 1)
                .shaped(true)
                .pattern("IRI")
                .pattern("WGW")
                .pattern("ISI")
                .define('I', ModItems.MANA_INGOT.get())
                .define('R', ModItems.REFINED_MANA_DUST.get())
                .define('W', ModItems.MANA_WIRE.get())
                .define('G', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('S', ModItems.MANA_SUBSTRATE.get())
                .manaCost(2000)
                .save(output, "mana_charger");

        // === 中間材料 ===
        // 魔力之眼（頭盔激活材料）
        ManaCraftingRecipeBuilder.shapeless(ModItems.MANA_EYE.get(), 1)
                .addIngredient(Ingredient.of(Items.ENDER_EYE))
                .addIngredient(Ingredient.of(ModItems.MANA_CRYSTAL_ALLOY_DUST.get()))
                .manaCost(2000)
                .save(output, "mana_eye");

        // 空白核心（所有核心插件的前置）
        ManaCraftingRecipeBuilder.create(ModItems.BLANK_CORE.get(), 1)
                .shaped(true)
                .pattern(" W ")
                .pattern("CIC")
                .pattern(" W ")
                .define('W', ModItems.MANA_WIRE.get())
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('I', ModItems.MANA_INGOT.get())
                .manaCost(600)
                .save(output, "blank_core");

        // 基礎升級外殼（所有升級插件的前置）
        ManaCraftingRecipeBuilder.create(ModItems.BASIC_UPGRADE_CASING.get(), 1)
                .shaped(true)
                .pattern("IMI")
                .pattern("SWS")
                .pattern("IMI")
                .define('I', Items.IRON_INGOT)
                .define('M', ModItems.MANA_INGOT.get())
                .define('S', ModItems.MANA_SUBSTRATE.get())
                .define('W', ModItems.MANA_WIRE.get())
                .manaCost(800)
                .save(output, "basic_upgrade_casing");

        // === 術式脈衝調制器 (法杖杆) ===
        ManaCraftingRecipeBuilder.create(ModItems.WAND_ROD.get(), 1)
                .shaped(true)
                .pattern("CIC")
                .pattern("WIW")
                .pattern(" I ")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('I', ModItems.MANA_INGOT.get())
                .define('W', ModItems.MANA_WIRE.get())
                .manaCost(2000)
                .save(output, "wand_rod");

        // 構成核心（祭壇前可做）
        ManaCraftingRecipeBuilder.create(ModItems.FORMATION_CORE.get(), 1)
                .shaped(true)
                .pattern("CWC")
                .pattern("WBW")
                .pattern("CWC")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('W', ModItems.MANA_WIRE.get())
                .define('B', ModItems.BLANK_CORE.get())
                .manaCost(800)
                .save(output, "formation_core");

        // 啟動核心（祭壇前可做）
        ManaCraftingRecipeBuilder.create(ModItems.ACTIVATION_CORE.get(), 1)
                .shaped(true)
                .pattern("SWS")
                .pattern("CBC")
                .pattern("SWS")
                .define('S', ModItems.MANA_SUBSTRATE.get())
                .define('W', ModItems.MANA_WIRE.get())
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('B', ModItems.BLANK_CORE.get())
                .manaCost(600)
                .save(output, "activation_core");

        // IO 核心
        ManaCraftingRecipeBuilder.create(ModItems.IO_CORE.get(), 1)
                .shaped(true)
                .pattern("WCW")
                .pattern("SBS")
                .pattern("WCW")
                .define('W', ModItems.MANA_WIRE.get())
                .define('C', Items.COMPARATOR)
                .define('S', ModItems.MANA_SUBSTRATE.get())
                .define('B', ModItems.BLANK_CORE.get())
                .manaCost(800)
                .save(output, "io_core");

        // 旋轉核心
        ManaCraftingRecipeBuilder.create(ModItems.ROTATION_CORE.get(), 1)
                .shaped(true)
                .pattern(" S ")
                .pattern("DBD")
                .pattern(" S ")
                .define('S', Items.STICK)
                .define('D', ModItems.MANA_DUST.get())
                .define('B', ModItems.BLANK_CORE.get())
                .manaCost(400)
                .save(output, "rotation_core");

        // 結構建造核心
        ManaCraftingRecipeBuilder.create(ModItems.STRUCTURE_BUILD_CORE.get(), 1)
                .shaped(true)
                .pattern("CMC")
                .pattern("WKW")
                .pattern("CMC")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('M', ModBlocks.MANA_BLOCK.get())
                .define('W', ModItems.MANA_WIRE.get())
                .define('K', ModItems.BLANK_CORE.get())
                .manaCost(1000)
                .save(output, "structure_build_core");

        // 儀式核心
        ManaCraftingRecipeBuilder.create(ModItems.RITUAL_CORE.get(), 1)
                .shaped(true)
                .pattern(" C ")
                .pattern("GKN")
                .pattern(" C ")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('G', Items.GOLD_INGOT)
                .define('K', ModItems.BLANK_CORE.get())
                .define('N', Items.BLAZE_ROD)
                .manaCost(1500)
                .save(output, "ritual_core");

        // 容量升級 Mk0
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_CAPACITY_MK0.get(), 1)
                .shaped(true)
                .pattern("CHC")
                .pattern("WUW")
                .pattern("CHC")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('W', ModItems.MANA_WAFER.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2500)
                .save(output, "wand_upgrade_capacity_mk0");

        // 容量升級 Mk1（中心放 Mk0）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_CAPACITY_MK1.get(), 1)
                .shaped(true)
                .pattern("CHC")
                .pattern("PAP")
                .pattern("CHC")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.WAND_UPGRADE_CAPACITY_MK0.get())
                .manaCost(4000)
                .save(output, "wand_upgrade_capacity_mk1");

        // 容量升級 Mk2（中心放 Mk1）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_CAPACITY_MK2.get(), 1)
                .shaped(true)
                .pattern("HPH")
                .pattern("PAP")
                .pattern("HPH")
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.WAND_UPGRADE_CAPACITY_MK1.get())
                .manaCost(6000)
                .save(output, "wand_upgrade_capacity_mk2");

        // 容量升級 Mk3（中心放 Mk2）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_CAPACITY_MK3.get(), 1)
                .shaped(true)
                .pattern("NPN")
                .pattern("PAP")
                .pattern("NPN")
                .define('N', Items.NETHERITE_INGOT)
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.WAND_UPGRADE_CAPACITY_MK2.get())
                .manaCost(10000)
                .save(output, "wand_upgrade_capacity_mk3");

        // 效率升級 Mk0
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_EFFICIENCY_MK0.get(), 1)
                .shaped(true)
                .pattern("QPQ")
                .pattern("RUR")
                .pattern("QPQ")
                .define('Q', ModItems.MANA_WAFER.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('R', ModItems.REFINED_MANA_DUST.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2500)
                .save(output, "wand_upgrade_efficiency_mk0");

        // 效率升級 Mk1（中心放 Mk0）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_EFFICIENCY_MK1.get(), 1)
                .shaped(true)
                .pattern("QPQ")
                .pattern("HAH")
                .pattern("QPQ")
                .define('Q', ModItems.MANA_WAFER.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('A', ModItems.WAND_UPGRADE_EFFICIENCY_MK0.get())
                .manaCost(4000)
                .save(output, "wand_upgrade_efficiency_mk1");

        // 效率升級 Mk2（中心放 Mk1）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_EFFICIENCY_MK2.get(), 1)
                .shaped(true)
                .pattern("HPH")
                .pattern("PAP")
                .pattern("HPH")
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.WAND_UPGRADE_EFFICIENCY_MK1.get())
                .manaCost(6000)
                .save(output, "wand_upgrade_efficiency_mk2");

        // 效率升級 Mk3（中心放 Mk2）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_EFFICIENCY_MK3.get(), 1)
                .shaped(true)
                .pattern("NPN")
                .pattern("PAP")
                .pattern("NPN")
                .define('N', Items.NETHERITE_INGOT)
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.WAND_UPGRADE_EFFICIENCY_MK2.get())
                .manaCost(10000)
                .save(output, "wand_upgrade_efficiency_mk3");

        // 範圍升級 Mk0
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_RANGE_MK0.get(), 1)
                .shaped(true)
                .pattern("WEW")
                .pattern("PUP")
                .pattern("WEW")
                .define('W', ModItems.MANA_WIRE.get())
                .define('E', Items.ENDER_PEARL)
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2000)
                .save(output, "wand_upgrade_range_mk0");

        // 範圍升級 Mk1（中心放 Mk0）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_RANGE_MK1.get(), 1)
                .shaped(true)
                .pattern("WEW")
                .pattern("EAE")
                .pattern("WEW")
                .define('W', ModItems.MANA_WIRE.get())
                .define('E', Items.ENDER_PEARL)
                .define('A', ModItems.WAND_UPGRADE_RANGE_MK0.get())
                .manaCost(3500)
                .save(output, "wand_upgrade_range_mk1");

        // 範圍升級 Mk2（中心放 Mk1）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_RANGE_MK2.get(), 1)
                .shaped(true)
                .pattern("EHE")
                .pattern("HAH")
                .pattern("EHE")
                .define('E', Items.ENDER_PEARL)
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('A', ModItems.WAND_UPGRADE_RANGE_MK1.get())
                .manaCost(5500)
                .save(output, "wand_upgrade_range_mk2");

        // 範圍升級 Mk3（中心放 Mk2）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_RANGE_MK3.get(), 1)
                .shaped(true)
                .pattern("EHE")
                .pattern("NAN")
                .pattern("EHE")
                .define('E', Items.ENDER_EYE)
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('N', Items.NETHERITE_INGOT)
                .define('A', ModItems.WAND_UPGRADE_RANGE_MK2.get())
                .manaCost(9000)
                .save(output, "wand_upgrade_range_mk3");

        // 冷卻升級 Mk0
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_COOLDOWN_MK0.get(), 1)
                .shaped(true)
                .pattern("QRQ")
                .pattern("RUR")
                .pattern("QRQ")
                .define('Q', ModItems.MANA_WAFER.get())
                .define('R', Items.REDSTONE)
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(1500)
                .save(output, "wand_upgrade_cooldown_mk0");

        // 冷卻升級 Mk1（中心放 Mk0）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_COOLDOWN_MK1.get(), 1)
                .shaped(true)
                .pattern("QRQ")
                .pattern("RAR")
                .pattern("QRQ")
                .define('Q', ModItems.MANA_WAFER.get())
                .define('R', Items.REDSTONE)
                .define('A', ModItems.WAND_UPGRADE_COOLDOWN_MK0.get())
                .manaCost(2500)
                .save(output, "wand_upgrade_cooldown_mk1");

        // 冷卻升級 Mk2（中心放 Mk1）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_COOLDOWN_MK2.get(), 1)
                .shaped(true)
                .pattern("PRP")
                .pattern("RAR")
                .pattern("PRP")
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('R', Items.REDSTONE)
                .define('A', ModItems.WAND_UPGRADE_COOLDOWN_MK1.get())
                .manaCost(4500)
                .save(output, "wand_upgrade_cooldown_mk2");

        // 冷卻升級 Mk3（中心放 Mk2）
        ManaCraftingRecipeBuilder.create(ModItems.WAND_UPGRADE_COOLDOWN_MK3.get(), 1)
                .shaped(true)
                .pattern("PBP")
                .pattern("BAB")
                .pattern("PBP")
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('B', Items.REDSTONE_BLOCK)
                .define('A', ModItems.WAND_UPGRADE_COOLDOWN_MK2.get())
                .manaCost(8000)
                .save(output, "wand_upgrade_cooldown_mk3");

        // 魔力輸出升級 Mk0
        ManaCraftingRecipeBuilder.create(ModItems.MANA_OUTPUT_UPGRADE_MK0.get(), 1)
                .shaped(true)
                .pattern("WCW")
                .pattern("CUC")
                .pattern("WCW")
                .define('W', ModItems.MANA_WIRE.get())
                .define('C', ModItems.BASIC_MANA_CIRCUIT.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2500)
                .save(output, "mana_output_upgrade_mk0");

        // 魔力輸出升級 Mk1（中心放 Mk0）
        ManaCraftingRecipeBuilder.create(ModItems.MANA_OUTPUT_UPGRADE_MK1.get(), 1)
                .shaped(true)
                .pattern("WCW")
                .pattern("CAC")
                .pattern("WCW")
                .define('W', ModItems.MANA_WIRE.get())
                .define('C', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.MANA_OUTPUT_UPGRADE_MK0.get())
                .manaCost(4000)
                .save(output, "mana_output_upgrade_mk1");

        // 魔力輸出升級 Mk2（中心放 Mk1）
        ManaCraftingRecipeBuilder.create(ModItems.MANA_OUTPUT_UPGRADE_MK2.get(), 1)
                .shaped(true)
                .pattern("HCH")
                .pattern("CAC")
                .pattern("HCH")
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('C', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.MANA_OUTPUT_UPGRADE_MK1.get())
                .manaCost(6500)
                .save(output, "mana_output_upgrade_mk2");

        // 魔力輸出升級 Mk3（中心放 Mk2）
        ManaCraftingRecipeBuilder.create(ModItems.MANA_OUTPUT_UPGRADE_MK3.get(), 1)
                .shaped(true)
                .pattern("NHN")
                .pattern("HAH")
                .pattern("NHN")
                .define('N', Items.NETHERITE_INGOT)
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('A', ModItems.MANA_OUTPUT_UPGRADE_MK2.get())
                .manaCost(10000)
                .save(output, "mana_output_upgrade_mk3");

        // ── 靴子升級插件 ─────────────────────────────────────────────────────

        // 衝刺距離升級 Mk0
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK0.get(), 1)
                .shaped(true)
                .pattern("FMF")
                .pattern("MUM")
                .pattern("FMF")
                .define('F', Items.FEATHER)
                .define('M', ModItems.MANA_INGOT.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2000)
                .save(output, "boots_upgrade_dash_distance_mk0");

        // 衝刺距離升級 Mk1
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK1.get(), 1)
                .shaped(true)
                .pattern("FCF")
                .pattern("CAC")
                .pattern("FCF")
                .define('F', Items.FEATHER)
                .define('C', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('A', ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK0.get())
                .manaCost(3500)
                .save(output, "boots_upgrade_dash_distance_mk1");

        // 衝刺距離升級 Mk2
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK2.get(), 1)
                .shaped(true)
                .pattern("FXF")
                .pattern("XAX")
                .pattern("FXF")
                .define('F', Items.FEATHER)
                .define('X', ModItems.MANA_CRYSTAL.get())
                .define('A', ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK1.get())
                .manaCost(5500)
                .save(output, "boots_upgrade_dash_distance_mk2");

        // 衝刺距離升級 Mk3
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK3.get(), 1)
                .shaped(true)
                .pattern("PHP")
                .pattern("PAP")
                .pattern("PHP")
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('H', Items.PHANTOM_MEMBRANE)
                .define('A', ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK2.get())
                .manaCost(9000)
                .save(output, "boots_upgrade_dash_distance_mk3");

        // 魔力效率升級 Mk0
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK0.get(), 1)
                .shaped(true)
                .pattern("QWQ")
                .pattern("WUW")
                .pattern("QWQ")
                .define('Q', ModItems.MANA_WAFER.get())
                .define('W', ModItems.REFINED_MANA_DUST.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2000)
                .save(output, "boots_upgrade_mana_efficiency_mk0");

        // 魔力效率升級 Mk1
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK1.get(), 1)
                .shaped(true)
                .pattern("QWQ")
                .pattern("WAW")
                .pattern("QWQ")
                .define('Q', ModItems.MANA_WAFER.get())
                .define('W', ModItems.CONDENSED_MANA_DUST.get())
                .define('A', ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK0.get())
                .manaCost(3500)
                .save(output, "boots_upgrade_mana_efficiency_mk1");

        // 魔力效率升級 Mk2
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK2.get(), 1)
                .shaped(true)
                .pattern("QPQ")
                .pattern("PAP")
                .pattern("QPQ")
                .define('Q', ModItems.MANA_WAFER.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK1.get())
                .manaCost(5500)
                .save(output, "boots_upgrade_mana_efficiency_mk2");

        // 魔力效率升級 Mk3
        ManaCraftingRecipeBuilder.create(ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK3.get(), 1)
                .shaped(true)
                .pattern("HPH")
                .pattern("PAP")
                .pattern("HPH")
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK2.get())
                .manaCost(9000)
                .save(output, "boots_upgrade_mana_efficiency_mk3");

        // ── 通用魔力容量升級（適用所有魔力盔甲）───────────────────────────────────
        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_CAPACITY_MK0.get(), 1)
                .shaped(true)
                .pattern("FMF")
                .pattern("MUM")
                .pattern("FMF")
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('M', ModItems.MANA_INGOT.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2000)
                .save(output, "armor_upgrade_capacity_mk0");

        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_CAPACITY_MK1.get(), 1)
                .shaped(true)
                .pattern("FCF")
                .pattern("CAC")
                .pattern("FCF")
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('A', ModItems.ARMOR_UPGRADE_CAPACITY_MK0.get())
                .manaCost(3500)
                .save(output, "armor_upgrade_capacity_mk1");

        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_CAPACITY_MK2.get(), 1)
                .shaped(true)
                .pattern("CHC")
                .pattern("HAH")
                .pattern("CHC")
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('A', ModItems.ARMOR_UPGRADE_CAPACITY_MK1.get())
                .manaCost(5500)
                .save(output, "armor_upgrade_capacity_mk2");

        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_CAPACITY_MK3.get(), 1)
                .shaped(true)
                .pattern("HPH")
                .pattern("PAP")
                .pattern("HPH")
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.ARMOR_UPGRADE_CAPACITY_MK2.get())
                .manaCost(9000)
                .save(output, "armor_upgrade_capacity_mk3");

        // ── 通用防禦升級（適用於所有魔力盔甲）────────────────────────────────────
        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_DEFENSE_MK0.get(), 1)
                .shaped(true)
                .pattern("IMI")
                .pattern("MUM")
                .pattern("IMI")
                .define('I', Items.IRON_INGOT)
                .define('M', ModItems.MANA_INGOT.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2000)
                .save(output, "armor_upgrade_defense_mk0");

        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_DEFENSE_MK1.get(), 1)
                .shaped(true)
                .pattern("IMI")
                .pattern("FAF")
                .pattern("IMI")
                .define('I', Items.IRON_INGOT)
                .define('M', ModItems.MANA_INGOT.get())
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('A', ModItems.ARMOR_UPGRADE_DEFENSE_MK0.get())
                .manaCost(3500)
                .save(output, "armor_upgrade_defense_mk1");

        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_DEFENSE_MK2.get(), 1)
                .shaped(true)
                .pattern("IMI")
                .pattern("CAC")
                .pattern("IMI")
                .define('I', Items.IRON_INGOT)
                .define('M', ModItems.MANA_INGOT.get())
                .define('C', ModItems.MANA_CRYSTAL.get())
                .define('A', ModItems.ARMOR_UPGRADE_DEFENSE_MK1.get())
                .manaCost(5500)
                .save(output, "armor_upgrade_defense_mk2");

        ManaCraftingRecipeBuilder.create(ModItems.ARMOR_UPGRADE_DEFENSE_MK3.get(), 1)
                .shaped(true)
                .pattern("NPN")
                .pattern("PAP")
                .pattern("NPN")
                .define('N', Items.NETHERITE_INGOT)
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.ARMOR_UPGRADE_DEFENSE_MK2.get())
                .manaCost(9000)
                .save(output, "armor_upgrade_defense_mk3");

        // ── 護腿多段跳升級 ────────────────────────────────────────────────────
        ManaCraftingRecipeBuilder.create(ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK0.get(), 1)
                .shaped(true)
                .pattern("FMF")
                .pattern("MUM")
                .pattern("FMF")
                .define('F', Items.FEATHER)
                .define('M', ModItems.MANA_INGOT.get())
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2000)
                .save(output, "leggings_upgrade_multi_jump_mk0");

        ManaCraftingRecipeBuilder.create(ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK1.get(), 1)
                .shaped(true)
                .pattern("FRF")
                .pattern("RAR")
                .pattern("FRF")
                .define('F', ModItems.MANA_CRYSTAL_FRAGMENT.get())
                .define('R', Items.RABBIT_FOOT)
                .define('A', ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK0.get())
                .manaCost(3500)
                .save(output, "leggings_upgrade_multi_jump_mk1");

        ManaCraftingRecipeBuilder.create(ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK2.get(), 1)
                .shaped(true)
                .pattern("SHS")
                .pattern("HAH")
                .pattern("SHS")
                .define('S', Items.SLIME_BALL)
                .define('H', ModItems.HIGH_DENSITY_MANA_CORE.get())
                .define('A', ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK1.get())
                .manaCost(5500)
                .save(output, "leggings_upgrade_multi_jump_mk2");

        ManaCraftingRecipeBuilder.create(ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK3.get(), 1)
                .shaped(true)
                .pattern("VPV")
                .pattern("PAP")
                .pattern("VPV")
                .define('V', Items.PHANTOM_MEMBRANE)
                .define('P', ModItems.PRECISION_MANA_CIRCUIT.get())
                .define('A', ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK2.get())
                .manaCost(9000)
                .save(output, "leggings_upgrade_multi_jump_mk3");

        // ── 頭盔夜視升級 ──────────────────────────────────────────────────────
        ManaCraftingRecipeBuilder.create(ModItems.HELMET_UPGRADE_NIGHT_VISION.get(), 1)
                .shaped(true)
                .pattern("GIG")
                .pattern("IUI")
                .pattern("GIG")
                .define('G', Items.GOLDEN_CARROT)
                .define('I', Items.GLOW_INK_SAC)
                .define('U', ModItems.BASIC_UPGRADE_CASING.get())
                .manaCost(2500)
                .save(output, "helmet_upgrade_night_vision");

        // ── 裝甲底殼（兩步合成第一步）────────────────────────────────────────
        ManaCraftingRecipeBuilder.create(ModItems.MANA_ALLOY_HELMET_BASE.get(), 1)
                .shaped(true)
                .pattern("PPP")
                .pattern("PHP")
                .define('H', Items.IRON_HELMET)
                .define('P', ModItems.MANA_REINFORCED_PLATE.get())
                .manaCost(800)
                .save(output, "mana_alloy_helmet_base");

        ManaCraftingRecipeBuilder.create(ModItems.MANA_ALLOY_CHESTPLATE_BASE.get(), 1)
                .shaped(true)
                .pattern("PCP")
                .pattern("PPP")
                .pattern("PPP")
                .define('C', Items.IRON_CHESTPLATE)
                .define('P', ModItems.MANA_REINFORCED_PLATE.get())
                .manaCost(1200)
                .save(output, "mana_alloy_chestplate_base");

        ManaCraftingRecipeBuilder.create(ModItems.MANA_ALLOY_LEGGINGS_BASE.get(), 1)
                .shaped(true)
                .pattern("PPP")
                .pattern("PLP")
                .pattern("P P")
                .define('L', Items.IRON_LEGGINGS)
                .define('P', ModItems.MANA_REINFORCED_PLATE.get())
                .manaCost(1000)
                .save(output, "mana_alloy_leggings_base");

    }

}
