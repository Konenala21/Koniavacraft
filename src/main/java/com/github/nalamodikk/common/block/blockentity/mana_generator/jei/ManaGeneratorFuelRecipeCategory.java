package com.github.nalamodikk.common.block.blockentity.mana_generator.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.ManaGenFuelRecipe;
import com.github.nalamodikk.register.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ManaGeneratorFuelRecipeCategory implements IRecipeCategory<ManaGenFuelRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_fuel");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/jei_mana_gen_fuel.png");
    private static final ResourceLocation MANA_BAR = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final ResourceLocation ENERGY_BAR =  ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/energy_bar_full.png");
    public static  RecipeType<ManaGenFuelRecipe> manaGenFuelRecipeType =
            new RecipeType<>(UID, ManaGenFuelRecipe.class);
    private static final int RECIPE_WIDTH = 182;
    private static final int RECIPE_HEIGHT = 80;

    @Override
    public int getWidth() {
        return RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return RECIPE_HEIGHT;
    }




    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorFuelRecipeCategory.class);

    private final IDrawableStatic background;



    private final IDrawable icon; // ✅ 修改類型為 IDrawable

    public ManaGeneratorFuelRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 182, 80);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MANA_GENERATOR.get()));
    }

    @Override
    public RecipeType<ManaGenFuelRecipe> getRecipeType() {
        return manaGenFuelRecipeType;
    }


    @Override
    public Component getTitle() {
        return Component.translatable("jei.koniava.fuel");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ManaGenFuelRecipe recipe, IFocusGroup focuses) {
        ItemStack[] stacks = recipe.getIngredient().getItems();

        if (stacks.length == 0) {
            LOGGER.error("[JEI] ❌ FuelRecipeCategory 中 ingredient 無有效物品！(Ingredient has no matching ItemStacks to display in JEI)");
            return;
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 80, 40)
                .addItemStacks(List.of(stacks)); // 讓 JEI 自動處理多物品顯示（ex: 木炭或煤）
    }


    @Override
    public void draw(ManaGenFuelRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, 0, 0, 0, 0, 182, 80);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int manaRate = recipe.getManaRate();     // 魔力消耗/t
        int energyRate = recipe.getEnergyRate(); // 能量產出/t
        int burnTime = recipe.getBurnTime(); // ← 你要從這裡取
        Component burnTimeText = Component.translatable("jei.koniava.burn_time", burnTime);
        graphics.drawString(font, burnTimeText, 45, 27, 0x555555, false);
        double efficiency = manaRate > 0 ? (double) energyRate / manaRate : 0;
        // 🔹 顯示 tooltip：滑鼠滑到魔力條 → 顯示魔力資訊
        if (mouseX >= 8 && mouseX <= 19 && mouseY >= 10 && mouseY <= 70) {
            graphics.renderTooltip(font,
                    List.of(Component.translatable("jei.koniava.fuel.mana", manaRate)),
                    Optional.empty(),
                    (int) mouseX,
                    (int) mouseY
            );
        }

        // 🔹 顯示 tooltip：滑鼠滑到能量條 → 顯示能量資訊
        if (mouseX >= 150 && mouseX <= 170 && mouseY >= 10 && mouseY <= 70) {
            graphics.renderTooltip(font,
                    List.of(Component.translatable("jei.koniava.fuel.energy", energyRate)),
                    Optional.empty(),
                    (int) mouseX,
                    (int) mouseY
            );
        }
    }





    public static ResourceLocation getTexture() {
        return TEXTURE;
    }

}
