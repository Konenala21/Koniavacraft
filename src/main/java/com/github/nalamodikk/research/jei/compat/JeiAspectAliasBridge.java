package com.github.nalamodikk.research.jei.compat;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.research.jei.AspectIngredientType;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.ingredients.IIngredientType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public final class JeiAspectAliasBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(JeiAspectAliasBridge.class);

    private static final String INGREDIENT_MANAGER_CLASS_NAME = "mezz.jei.library.ingredients.IngredientManager";
    private static final String REGISTERED_INGREDIENTS_CLASS_NAME = "mezz.jei.library.ingredients.RegisteredIngredients";
    private static final String INGREDIENT_INFO_CLASS_NAME = "mezz.jei.library.ingredients.IngredientInfo";

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static volatile boolean warned;

    private static Class<?> ingredientManagerClass;
    private static Field registeredIngredientsField;
    private static Method getIngredientInfoMethod;
    private static Field aliasesField;
    private static Method clearAliasesMethod;
    private static Method addIngredientAliasesMethod;

    private JeiAspectAliasBridge() {
    }

    public static void refresh(IJeiRuntime runtime) {
        if (runtime == null || !initialize()) {
            return;
        }

        try {
            Object ingredientManager = runtime.getIngredientManager();
            if (!ingredientManagerClass.isInstance(ingredientManager)) {
                return;
            }

            Object registeredIngredients = registeredIngredientsField.get(ingredientManager);
            Object ingredientInfo = getIngredientInfoMethod.invoke(registeredIngredients, AspectIngredientType.INSTANCE);
            Object aliases = aliasesField.get(ingredientInfo);
            clearAliasesMethod.invoke(aliases);

            Collection<Aspect> aspects = ModAspects.all();
            for (Aspect aspect : aspects) {
                if (shouldExposeAlias(aspect)) {
                    addIngredientAliasesMethod.invoke(ingredientInfo, aspect, List.of(
                            aspect.getId().toString(),
                            aspect.getName().getString()
                    ));
                }
            }

            LOGGER.debug("[JEI] Refreshed aspect aliases.");
        } catch (ReflectiveOperationException e) {
            if (!warned) {
                warned = true;
                LOGGER.warn("[JEI] Failed to refresh aspect aliases dynamically.", e);
            }
        }
    }

    private static boolean shouldExposeAlias(Aspect aspect) {
        return aspect.isPrimary() || ClientResearchCache.hasDiscovered(aspect.getId());
    }

    private static boolean initialize() {
        if (initialized) {
            return available;
        }

        synchronized (JeiAspectAliasBridge.class) {
            if (initialized) {
                return available;
            }
            try {
                ingredientManagerClass = Class.forName(INGREDIENT_MANAGER_CLASS_NAME);
                registeredIngredientsField = ingredientManagerClass.getDeclaredField("registeredIngredients");
                registeredIngredientsField.setAccessible(true);

                Class<?> registeredIngredientsClass = Class.forName(REGISTERED_INGREDIENTS_CLASS_NAME);
                getIngredientInfoMethod = registeredIngredientsClass.getMethod("getIngredientInfo", IIngredientType.class);

                Class<?> ingredientInfoClass = Class.forName(INGREDIENT_INFO_CLASS_NAME);
                aliasesField = ingredientInfoClass.getDeclaredField("aliases");
                aliasesField.setAccessible(true);
                clearAliasesMethod = aliasesField.getType().getMethod("clear");
                addIngredientAliasesMethod = ingredientInfoClass.getMethod("addIngredientAliases", Object.class, Collection.class);

                available = true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                available = false;
                if (!warned) {
                    warned = true;
                    LOGGER.warn("[JEI] Dynamic aspect alias bridge is unavailable.", e);
                }
            } finally {
                initialized = true;
            }
        }
        return available;
    }
}
