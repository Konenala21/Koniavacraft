package com.github.nalamodikk.common.block.blockentity.altar.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AltarRecipe;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class AltarJEIPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "altar_jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new AltarRecipeCategory(helper),
                new AltarMultiblockCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        List<AltarRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ALTAR_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        registration.addRecipes(AltarRecipeCategory.RECIPE_TYPE, recipes);
        registration.addRecipes(AltarMultiblockCategory.RECIPE_TYPE,
                List.of(AltarStructureInfo.INSTANCE));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ASPECT_ALTAR.get()),
                AltarRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ASPECT_PEDESTAL.get()),
                AltarRecipeCategory.RECIPE_TYPE);

        // 多方塊結構指引：進階法杖、結構建造法杖、祭壇本身都能查看
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.ADVANCED_TECH_WAND.get()),
                AltarMultiblockCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.STRUCTURE_BUILD_WAND.get()),
                AltarMultiblockCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ASPECT_ALTAR.get()),
                AltarMultiblockCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.MANA_BLOCK.get(), 8),
                AltarMultiblockCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ASPECT_PEDESTAL.get(), 5),
                AltarMultiblockCategory.RECIPE_TYPE);
    }
}
