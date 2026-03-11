package com.github.nalamodikk.common.block.blockentity.mana_generator.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorScreen;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.ManaGenFuelRecipe;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@JeiPlugin
public class ManaGeneratorJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID =  ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_generator_jei_plugin");
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorJEIPlugin.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("[JEI] Registering FuelRecipeCategory.");

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(new ManaGeneratorFuelRecipeCategory(guiHelper));
    }


    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        LOGGER.info("[JEI] Registering Mana Generator as a FuelRecipeCategory catalyst.");
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.MANA_GENERATOR.get()), ManaGeneratorFuelRecipeCategory.manaGenFuelRecipeType
        );
    }



    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(ManaGeneratorScreen.class,30,15 ,25,45, ManaGeneratorFuelRecipeCategory.manaGenFuelRecipeType);
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {

            LOGGER.warn("[JEI] Minecraft level is null. Unable to load fuel recipes.");
            return;
        }

        List<ManaGenFuelRecipe> manaGenFuelRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.MANA_FUEL_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        if (manaGenFuelRecipes.isEmpty()) {
            LOGGER.error("[JEI] Fuel recipe count is 0. Verify that JSON files were generated under `mana_recipes/fuel/`.");
            return;
        }

        LOGGER.info("[JEI] Found {} fuel recipes. Registering them now.", manaGenFuelRecipes.size());

        // ✅ 正確：使用已取得的清單，不要再重抓一次！
        registration.addRecipes(
                ManaGeneratorFuelRecipeCategory.manaGenFuelRecipeType,
                manaGenFuelRecipes
        );

        LOGGER.info("[JEI] FuelRecipeCategory recipes registered.");
    }

}
