package com.github.nalamodikk.research.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.skill.reaction.ReactionInfo;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * JEI 插件:本源反應分頁(化學反應表)。讀 {@link ReactionInfo#ALL}。
 */
@JeiPlugin
public class ReactionJEIPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "reaction_jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ReactionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        registration.addRecipes(ReactionCategory.RECIPE_TYPE, ReactionInfo.ALL);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 法術核心/編碼台當入口圖示
        registration.addRecipeCatalyst(new ItemStack(ModItems.SPELL_CORE.get()), ReactionCategory.RECIPE_TYPE);
    }
}
