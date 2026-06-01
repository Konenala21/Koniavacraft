package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.WorldAspectSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        List<Aspect> cached = data.getItemMapping(id);
        if (cached != null) return cached;

        // Start Inference
        List<Aspect> result = infer(item, level, data.getGenomeSeed(), 0, new HashSet<>());

        // Semantic pool + world-seed dealing (guaranteed carriers + seeded pick):
        // plausible aspects from the name, varied per world, every aspect guaranteed a source.
        if (result.isEmpty()) {
            result = SemanticDealer.resolve(id, data.getGenomeSeed(), 2);
        }

        // If result is empty, check Base Registry again as a safety fallback
        if (result.isEmpty()) {
            result = BaseMaterialRegistry.getFallbackAspects(item, data.getGenomeSeed());
        }

        if (!result.isEmpty()) {
            data.putItemMapping(id, result);
        }
        
        return result;
    }

    private static List<Aspect> infer(Item item, ServerLevel level, long genomeSeed, int depth, Set<ResourceLocation> visited) {
        if (depth > MAX_DEPTH) return List.of();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!visited.add(id)) {
            return List.of();
        }

        // 1. Check Base Registry (Atoms)
        List<Aspect> base = BaseMaterialRegistry.getSemanticAspects(item, genomeSeed);
        if (!base.isEmpty()) {
            visited.remove(id);
            return base;
        }

        // 2. Scan Recipes
        RecipeManager recipeManager = level.getRecipeManager();
        List<RecipeHolder<?>> recipes = recipeManager.getRecipes().stream()
                .filter(r -> r.value().getResultItem(level.registryAccess()).getItem() == item)
                .toList();

        if (recipes.isEmpty()) {
            visited.remove(id);
            return List.of();
        }

        // Use the first available recipe (usually the primary one)
        RecipeHolder<?> recipe = recipes.get(0);
        Map<Aspect, Integer> aspectCounts = new HashMap<>();

        for (Ingredient ingredient : recipe.value().getIngredients()) {
            if (ingredient.isEmpty()) continue;
            
            // Pick a representative item from the ingredient
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length > 0) {
                List<Aspect> ingredientAspects = infer(stacks[0].getItem(), level, genomeSeed, depth + 1, visited);
                for (Aspect aspect : ingredientAspects) {
                    aspectCounts.put(aspect, aspectCounts.getOrDefault(aspect, 0) + 1);
                }
            }
        }

        // Collapse to top 2-3 aspects to avoid aspect bloat
        List<Aspect> result = aspectCounts.entrySet().stream()
                .sorted(Map.Entry.<Aspect, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(capacityForRecipe(item))
                .map(Map.Entry::getKey)
                .toList();
        visited.remove(id);
        if (!result.isEmpty() && isMechanicalRecipe(recipe)) {
            return AspectExpression.express(id, result, List.of(ModAspects.MECHANISM), genomeSeed, capacityForRecipe(item));
        }
        return result;
    }

    private static boolean isMechanicalRecipe(RecipeHolder<?> recipe) {
        ResourceLocation recipeId = recipe.id();
        String path = recipeId.getPath();
        return path.contains("piston")
                || path.contains("rail")
                || path.contains("redstone")
                || path.contains("dispenser")
                || path.contains("dropper")
                || path.contains("observer")
                || path.contains("crafter");
    }

    private static int capacityForRecipe(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        if (path.contains("mana") || path.contains("magic") || path.contains("enchanted")) {
            return 4;
        }
        if (path.contains("piston") || path.contains("redstone") || path.contains("rail")) {
            return 3;
        }
        return 3;
    }
}
