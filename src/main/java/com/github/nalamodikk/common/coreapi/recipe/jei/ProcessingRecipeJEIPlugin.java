package com.github.nalamodikk.common.coreapi.recipe.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressRecipe;
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressScreen;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderScreen;
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

/**
 * 加工配方 JEI 整合插件
 */
@JeiPlugin
public class ProcessingRecipeJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "processing_recipe_jei_plugin");
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingRecipeJEIPlugin.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    /**
     * 📝 註冊 JEI 配方分類
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("[JEI] Registering processing recipe categories.");

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
                new GrinderRecipeCategory(guiHelper),
                new PlatePressRecipeCategory(guiHelper)
        );

        LOGGER.info("[JEI] Processing recipe categories registered.");
    }

    /**
     * 🔨 註冊催化劑（機器方塊）
     *
     * 在 JEI 中，催化劑是指能執行該配方的機器
     * 用戶點擊配方時，會顯示哪個機器能做這個配方
     */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        LOGGER.info("[JEI] Registering processing machine catalysts.");

        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.MANA_GRINDER.get()),
                GrinderRecipeCategory.RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.MANA_PLATE_PRESS.get()),
                PlatePressRecipeCategory.RECIPE_TYPE
        );

    }

    /**
     * 🖱️ 註冊 GUI 點擊區域
     *
     * 讓玩家點擊 GUI 中特定區域就能打開對應的 JEI 配方
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
                ManaGrinderScreen.class,
                149, 4,
                21, 15,
                GrinderRecipeCategory.RECIPE_TYPE
        );
        registration.addRecipeClickArea(
                ManaPlatePressScreen.class,
                67, 36,
                44, 12,
                PlatePressRecipeCategory.RECIPE_TYPE
        );
    }

    /**
     * 📖 註冊配方到 JEI
     *
     * 從遊戲中讀取所有配方，按機器類型分類後註冊到 JEI
     */
    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOGGER.warn("[JEI] Minecraft level is null. Skipping processing recipe registration.");
            return;
        }

        // 獲取所有加工配方
        List<ProcessingRecipe> allRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        if (allRecipes.isEmpty()) {
            LOGGER.warn("[JEI] No grinder processing recipes found.");
        }

        // 按機器類型分類
        List<ProcessingRecipe> grinderRecipes = allRecipes.stream()
                .filter(r -> "grinder".equals(r.getMachineType()))
                .toList();

        if (!grinderRecipes.isEmpty()) {
            LOGGER.info("[JEI] Registering {} grinder recipes.", grinderRecipes.size());
            registration.addRecipes(GrinderRecipeCategory.RECIPE_TYPE, grinderRecipes);
        }

        List<ManaPlatePressRecipe> platePressRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.PLATE_PRESS_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        if (!platePressRecipes.isEmpty()) {
            LOGGER.info("[JEI] Registering {} plate press recipes.", platePressRecipes.size());
            registration.addRecipes(PlatePressRecipeCategory.RECIPE_TYPE, platePressRecipes);
        }

        LOGGER.info("[JEI] All processing recipes registered.");
    }
}
