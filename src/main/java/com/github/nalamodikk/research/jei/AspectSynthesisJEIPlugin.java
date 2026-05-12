package com.github.nalamodikk.research.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.client.AspectSynthesisScreen;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.common.item.research.AspectTokenItem;
import com.github.nalamodikk.research.jei.client.AspectIngredientRenderer;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JeiPlugin
public class AspectSynthesisJEIPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "aspect_synthesis_jei_plugin");
    private static final Logger LOGGER = LoggerFactory.getLogger(AspectSynthesisJEIPlugin.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("[JEI] Registering AspectSynthesisRecipeCategory.");
        registration.addRecipeCategories(new AspectSynthesisRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.RESEARCH_TABLE.get()),
                AspectSynthesisRecipeCategory.RECIPE_TYPE);
        for (Aspect aspect : ModAspects.all()) {
            registration.addRecipeCatalyst(
                    AspectTokenItem.forAspect(aspect, !aspect.isPrimary()),
                    AspectSynthesisRecipeCategory.RECIPE_TYPE
            );
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.ASPECT_TOKEN.get(), (ItemStack stack, UidContext context) -> {
            ResourceLocation aspectId = AspectTokenItem.getAspectId(stack);
            return aspectId != null ? aspectId.toString() : "";
        });
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        Collection<Aspect> aspects = ModAspects.all();
        registration.register(
                AspectIngredientType.INSTANCE,
                aspects,
                new AspectIngredientHelper(),
                new AspectIngredientRenderer(),
                AspectIngredientType.CODEC
        );
        LOGGER.info("[JEI] Registered {} aspect ingredients.", aspects.size());
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        for (Aspect aspect : ModAspects.all()) {
            if (aspect.isPrimary() || ClientResearchCache.hasDiscovered(aspect.getId())) {
                registration.addAliases(AspectIngredientType.INSTANCE, aspect, List.of(
                        aspect.getId().toString(),
                        aspect.getName().getString()
                ));
            }
        }
        LOGGER.info("[JEI] Registered aspect ingredient aliases.");
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Aspect aspect : ModAspects.all()) {
            stacks.add(AspectTokenItem.forAspect(aspect, !aspect.isPrimary()));
        }
        registration.addExtraItemStacks(stacks);
        LOGGER.info("[JEI] Registered {} extra aspect token item stacks.", stacks.size());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<AspectSynthesisRecipe> recipes = new ArrayList<>();
        for (Aspect aspect : ModAspects.all()) {
            List<Aspect> components = aspect.getComponents();
            if (!aspect.isPrimary() && components.size() >= 2) {
                recipes.add(new AspectSynthesisRecipe(components.get(0), components.get(1), aspect));
            }
        }

        registration.addRecipes(AspectSynthesisRecipeCategory.RECIPE_TYPE, recipes);
        LOGGER.info("[JEI] Registered {} aspect synthesis recipes.", recipes.size());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(AspectSynthesisScreen.class, 108, 8, 28, 20,
                AspectSynthesisRecipeCategory.RECIPE_TYPE);
    }
}
