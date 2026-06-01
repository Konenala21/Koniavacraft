package com.github.nalamodikk.common.datagen.recipe.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AltarRecipe;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class AltarRecipeProvider {

    public static void generate(RecipeOutput output) {
        registerWeapons(output);
        registerWandParts(output);
        registerBoots(output);
        registerArmor(output);
        registerCircuitMaterials(output);
        registerConduits(output);
        registerKnowledgeItems(output);
        registerSkillSystem(output);
        // 魔力水晶（催化：精煉魔力粉，底座：魔力碎片×4）
        save(output, "mana_crystal_ritual",
                Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                List.of(
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get())
                ),
                new ItemStack(ModItems.MANA_CRYSTAL.get(), 2),
                8000, 120
        );

    }

    private static void registerWeapons(RecipeOutput output) {
        // 浮游砲（T3 祭壇，min_tier=3）
        save(output, "floating_turret",
                Ingredient.of(ModItems.HIGH_DENSITY_MANA_CORE.get()),
                List.of(
                        Ingredient.of(ModItems.PRECISION_MANA_CIRCUIT.get()),
                        Ingredient.of(ModItems.PRECISION_MANA_CIRCUIT.get()),
                        Ingredient.of(ModItems.MANA_BARREL.get()),
                        Ingredient.of(ModItems.MANA_BARREL.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get())
                ),
                new ItemStack(ModItems.FLOATING_TURRET.get()),
                50000, 400, 3
        );
    }

    private static void registerWandParts(RecipeOutput output) {
        // 術式脈衝諧振器（T3 祭壇，升級自基礎杖柄）
        save(output, "wand_rod_advanced",
                Ingredient.of(ModItems.HIGH_DENSITY_MANA_CORE.get()),
                List.of(
                        Ingredient.of(ModItems.WAND_ROD.get()),
                        Ingredient.of(ModItems.PRECISION_MANA_CIRCUIT.get()),
                        Ingredient.of(ModItems.PRECISION_MANA_CIRCUIT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(Items.AMETHYST_SHARD)
                ),
                new ItemStack(ModItems.WAND_ROD_ADVANCED.get()),
                40000, 320, 3
        );
    }

    private static void registerCircuitMaterials(RecipeOutput output) {
        save(output, "mana_wafer",
                Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                List.of(
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get())
                ),
                new ItemStack(ModItems.MANA_WAFER.get(), 2),
                5000, 100
        );
    }

    private static void registerArmor(RecipeOutput output) {
        // 頭盔激活 T1 祭壇：底殼 + 魔力之眼 ×2 + 魔力線 ×2 + 晶片 ×2 + 精煉粉 ×2
        save(output, "mana_alloy_helmet",
                Ingredient.of(ModItems.MANA_ALLOY_HELMET_BASE.get()),
                List.of(
                        Ingredient.of(ModItems.MANA_EYE.get()),
                        Ingredient.of(ModItems.MANA_EYE.get()),
                        Ingredient.of(ModItems.MANA_WIRE.get()),
                        Ingredient.of(ModItems.MANA_WIRE.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get())
                ),
                new ItemStack(ModItems.MANA_ALLOY_HELMET.get()),
                10000, 160, 1
        );

        // 胸甲激活 T1 祭壇：底殼 + 盾牌 + 魔力水晶 ×2 + 強化板 ×2 + 魔力錠 ×2 + 魔力線
        save(output, "mana_alloy_chestplate",
                Ingredient.of(ModItems.MANA_ALLOY_CHESTPLATE_BASE.get()),
                List.of(
                        Ingredient.of(Items.SHIELD),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_REINFORCED_PLATE.get()),
                        Ingredient.of(ModItems.MANA_REINFORCED_PLATE.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_WIRE.get())
                ),
                new ItemStack(ModItems.MANA_ALLOY_CHESTPLATE.get()),
                15000, 200, 1
        );

        // 護腿激活 T1 祭壇：底殼 + 兔足 + 黏液球 ×2 + 晶片 ×2 + 魔力線 ×2 + 魔力錠
        save(output, "mana_alloy_leggings",
                Ingredient.of(ModItems.MANA_ALLOY_LEGGINGS_BASE.get()),
                List.of(
                        Ingredient.of(Items.RABBIT_FOOT),
                        Ingredient.of(Items.SLIME_BALL),
                        Ingredient.of(Items.SLIME_BALL),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_WIRE.get()),
                        Ingredient.of(ModItems.MANA_WIRE.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get())
                ),
                new ItemStack(ModItems.MANA_ALLOY_LEGGINGS.get()),
                12000, 180, 1
        );
    }

    private static void registerBoots(RecipeOutput output) {
        // 魔力衝刺靴底殼 T1 祭壇
        save(output, "mana_sprint_boots_base",
                Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                List.of(
                        Ingredient.of(Items.LEATHER_BOOTS),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(Items.FEATHER),
                        Ingredient.of(Items.FEATHER)
                ),
                new ItemStack(ModItems.MANA_SPRINT_BOOTS_BASE.get()),
                10000, 160, 1
        );

        // 魔力衝刺靴激活 T1 祭壇（底殼→完成）
        save(output, "mana_sprint_boots_activate",
                Ingredient.of(ModItems.MANA_SPRINT_BOOTS_BASE.get()),
                List.of(
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_WIRE.get()),
                        Ingredient.of(ModItems.MANA_WIRE.get()),
                        Ingredient.of(Items.FEATHER),
                        Ingredient.of(Items.FEATHER)
                ),
                new ItemStack(ModItems.MANA_SPRINT_BOOTS.get()),
                8000, 120, 1
        );
    }

    private static void registerConduits(RecipeOutput output) {
        // T1 進階導管 ×8（需要 1 層升級環）
        save(output, "advanced_arcane_conduit",
                Ingredient.of(ModItems.MANA_INGOT.get()),
                List.of(
                        Ingredient.of(ModBlocks.BASIC_ARCANE_CONDUIT.get()),
                        Ingredient.of(ModBlocks.BASIC_ARCANE_CONDUIT.get()),
                        Ingredient.of(ModBlocks.BASIC_ARCANE_CONDUIT.get()),
                        Ingredient.of(ModBlocks.BASIC_ARCANE_CONDUIT.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.GOLD_INGOT)
                ),
                new ItemStack(ModBlocks.ADVANCED_ARCANE_CONDUIT.get(), 8),
                20000, 240, 1
        );

        // T3 精英導管 ×8（需要 3 層升級環）
        save(output, "elite_arcane_conduit",
                Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                List.of(
                        Ingredient.of(ModBlocks.ADVANCED_ARCANE_CONDUIT.get()),
                        Ingredient.of(ModBlocks.ADVANCED_ARCANE_CONDUIT.get()),
                        Ingredient.of(ModBlocks.ADVANCED_ARCANE_CONDUIT.get()),
                        Ingredient.of(ModBlocks.ADVANCED_ARCANE_CONDUIT.get()),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.AMETHYST_SHARD),
                        Ingredient.of(Items.AMETHYST_SHARD)
                ),
                new ItemStack(ModBlocks.ELITE_ARCANE_CONDUIT.get(), 8),
                40000, 400, 3
        );
    }

    private static void registerSkillSystem(RecipeOutput output) {
        // 本源編碼基板激活（T2 祭壇）：空白基板（催化）+ 鏡核碎片儀式 → 可編碼的本源編碼基板。
        // 鏡像本質在祭壇被綁進基板；鏡核碎片只在這一步出現，是整套技能系統的單一 boss 閘門。
        save(output, "aspect_codec_board",
                Ingredient.of(ModItems.ASPECT_CODEC_BOARD_BASE.get()),
                List.of(
                        Ingredient.of(ModItems.MIRROR_CORE_SHARD.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get())
                ),
                new ItemStack(ModItems.ASPECT_CODEC_BOARD.get()),
                12000, 200, 2
        );

        // 本源編碼台（T3 祭壇）：技能系統的工作站，吃激活後的本源編碼基板組成。
        save(output, "skill_encoder",
                Ingredient.of(ModItems.HIGH_DENSITY_MANA_CORE.get()),
                List.of(
                        Ingredient.of(ModItems.ASPECT_CODEC_BOARD.get()),
                        Ingredient.of(ModItems.ASPECT_CODEC_BOARD.get()),
                        Ingredient.of(ModItems.PRECISION_MANA_CIRCUIT.get()),
                        Ingredient.of(ModItems.PRECISION_MANA_CIRCUIT.get()),
                        Ingredient.of(ModItems.MANA_WAFER.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_SUBSTRATE.get())
                ),
                new ItemStack(ModBlocks.SKILL_ENCODER.get()),
                50000, 400, 3
        );
    }

    private static void registerKnowledgeItems(RecipeOutput output) {
        // 共識眼鏡（催化：紫水晶塊，底座：玻璃板×2 + 魔力水晶 + 魔力錠）T1
        save(output, "consensus_glasses",
                Ingredient.of(Items.AMETHYST_BLOCK),
                List.of(
                        Ingredient.of(Items.GLASS_PANE),
                        Ingredient.of(Items.GLASS_PANE),
                        Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get())
                ),
                new ItemStack(ModItems.CONSENSUS_GLASSES.get()),
                10000, 300, 1
        );
    }

    private static void save(RecipeOutput output, String name,
                              Ingredient catalyst, List<Ingredient> ingredients,
                              ItemStack result, int manaCost, int processingTime) {
        save(output, name, catalyst, ingredients, result, manaCost, processingTime, 0);
    }

    private static void save(RecipeOutput output, String name,
                              Ingredient catalyst, List<Ingredient> ingredients,
                              ItemStack result, int manaCost, int processingTime, int minTier) {
        AltarRecipe recipe = new AltarRecipe(catalyst, ingredients, result, manaCost, processingTime, minTier);
        output.accept(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "altar/" + name),
                recipe, null
        );
    }
}
