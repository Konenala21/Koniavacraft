package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.knowledge.WorldAspectSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The heart of the "Alchemical Inheritance" system.
 * Recursively derives aspects from recipes and world seeds.
 */
public class RecipeInferenceEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeInferenceEngine.class);
    private static final int MAX_DEPTH = 5; // Prevent stack overflow or infinite loops

    /**
     * Resolves the aspects for an item in the context of a specific world.
     */
    public static List<Aspect> resolve(Item item, ServerLevel level) {
        WorldAspectSavedData data = WorldAspectSavedData.get(level);
        List<Aspect> cached = data.getMapping(BuiltInRegistries.ITEM.getKey(item));
        if (cached != null) return cached;

        // Start Inference
        List<Aspect> result = infer(item, level, 0);
        
        // If result is empty, check Base Registry again as a safety fallback
        if (result.isEmpty()) {
            result = BaseMaterialRegistry.getBaseAspects(item, level.getSeed());
        }

        if (!result.isEmpty()) {
            data.putMapping(BuiltInRegistries.ITEM.getKey(item), result);
        }
        
        return result;
    }

    private static List<Aspect> infer(Item item, ServerLevel level, int depth) {
        if (depth > MAX_DEPTH) return List.of();

        // 1. Check Base Registry (Atoms)
        List<Aspect> base = BaseMaterialRegistry.getBaseAspects(item, level.getSeed());
        if (!base.isEmpty()) return base;

        // 2. Scan Recipes
        var recipeManager = level.getRecipeManager();
        List<RecipeHolder<?>> recipes = recipeManager.getRecipes().stream()
                .filter(r -> r.value().getResultItem(level.registryAccess()).getItem() == item)
                .toList();

        if (recipes.isEmpty()) return List.of();

        // Use the first available recipe (usually the primary one)
        RecipeHolder<?> recipe = recipes.get(0);
        Map<Aspect, Integer> aspectCounts = new HashMap<>();

        for (var ingredient : recipe.value().getIngredients()) {
            if (ingredient.isEmpty()) continue;
            
            // Pick a representative item from the ingredient
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length > 0) {
                List<Aspect> ingredientAspects = infer(stacks[0].getItem(), level, depth + 1);
                for (Aspect a : ingredientAspects) {
                    aspectCounts.put(a, aspectCounts.getOrDefault(a, 0) + 1);
                }
            }
        }

        // Collapse to top 2-3 aspects to avoid aspect bloat
        return aspectCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }
}
