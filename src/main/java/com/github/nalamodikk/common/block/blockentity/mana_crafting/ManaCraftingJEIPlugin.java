package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@JeiPlugin
public class ManaCraftingJEIPlugin implements IModPlugin {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_crafting");
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaCraftingJEIPlugin.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 注册自定义配方类别 (魔力合成台)
        LOGGER.info("[JEI] 正在註冊 ManaCraftingTableCategory...");

        registration.addRecipeCategories(new ManaCraftingTableCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 注册魔力合成台作为配方的催化剂 (即 JEI 中显示的合成台图标)
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()),
                ManaCraftingTableCategory.MANA_CRAFTING_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOGGER.warn("[JEI] Minecraft 尚未進入世界，跳過 mana_crafting 配方註冊！");
            return;
        }

        RecipeManager recipeManager = minecraft.level.getRecipeManager();

        List<ManaCraftingTableRecipe> manaCraftingTableRecipes =
                recipeManager.getAllRecipesFor(ModRecipes.MANA_CRAFTING_TYPE.get())
                .stream()
                        .map(RecipeHolder::value)
                        .toList();

        registration.addRecipes(ManaCraftingTableCategory.MANA_CRAFTING_TYPE, manaCraftingTableRecipes);
        LOGGER.info("[JEI] 成功註冊 {} 筆 mana_crafting 配方", manaCraftingTableRecipes.size());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 注册 GUI 的点击区域，使玩家能够在 JEI 中点击魔力合成台的配方以查看
        registration.addRecipeClickArea(ManaCraftingScreen.class, 100, 40, 20, 30,
                ManaCraftingTableCategory.MANA_CRAFTING_TYPE);
    }
}
