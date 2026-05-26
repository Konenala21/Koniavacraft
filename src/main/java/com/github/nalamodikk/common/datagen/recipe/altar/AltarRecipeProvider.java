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

        // 魔力錠（催化：魔力水晶，底座：鐵錠×4）
        save(output, "mana_ingot_ritual",
                Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT)
                ),
                new ItemStack(ModItems.MANA_INGOT.get(), 4),
                6000, 100
        );

        // 金蘋果（催化：魔力錠，底座：金錠×4）
        save(output, "golden_apple_ritual",
                Ingredient.of(ModItems.MANA_INGOT.get()),
                List.of(
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.APPLE)
                ),
                new ItemStack(Items.GOLDEN_APPLE),
                12000, 160
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
        // 魔力合金頭盔 T1 祭壇
        save(output, "mana_alloy_helmet",
                Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                List.of(
                        Ingredient.of(Items.IRON_HELMET),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get())
                ),
                new ItemStack(ModItems.MANA_ALLOY_HELMET.get()),
                8000, 140, 1
        );

        // 魔力合金胸甲 T1 祭壇
        save(output, "mana_alloy_chestplate",
                Ingredient.of(ModItems.MANA_CRYSTAL.get()),
                List.of(
                        Ingredient.of(Items.IRON_CHESTPLATE),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get())
                ),
                new ItemStack(ModItems.MANA_ALLOY_CHESTPLATE.get()),
                12000, 180, 1
        );

        // 魔力合金護腿 T1 祭壇
        save(output, "mana_alloy_leggings",
                Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                List.of(
                        Ingredient.of(Items.IRON_LEGGINGS),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_INGOT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.MANA_CRYSTAL_FRAGMENT.get()),
                        Ingredient.of(ModItems.REFINED_MANA_DUST.get())
                ),
                new ItemStack(ModItems.MANA_ALLOY_LEGGINGS.get()),
                10000, 160, 1
        );
    }

    private static void registerBoots(RecipeOutput output) {
        // 魔力衝刺靴（未完成）T1 祭壇
        save(output, "mana_sprint_boots_unfinished",
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
                new ItemStack(ModItems.MANA_SPRINT_BOOTS_UNFINISHED.get()),
                10000, 160, 1
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
