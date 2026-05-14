package com.github.nalamodikk.common.block.blockentity.altar.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AltarRecipe;
import com.github.nalamodikk.register.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AltarRecipeCategory implements IRecipeCategory<AltarRecipe> {

    public static final RecipeType<AltarRecipe> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "altar", AltarRecipe.class);

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/full-arrow.png");

    // 佈局：左側 2×2 底座輸入，中間催化物，箭頭，右側結果
    // [I1][I2]
    // [I3][I4]  [CAT]  →  [OUT]
    //           Mana: x  Time: xs
    private static final int WIDTH  = 148;
    private static final int HEIGHT = 58;

    private static final int[] ING_X = {1, 20, 1, 20};
    private static final int[] ING_Y = {1, 1, 20, 20};

    private static final int CAT_X   = 50;
    private static final int CAT_Y   = 10;
    private static final int ARROW_X = 72;
    private static final int ARROW_Y = 14;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 16;
    private static final int OUT_X   = 98;
    private static final int OUT_Y   = 10;
    private static final int TEXT_Y  = 42;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;
    private final IDrawableStatic arrowStatic;

    public AltarRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ASPECT_ALTAR.get())
        );
        this.arrowStatic = guiHelper.drawableBuilder(ARROW_TEXTURE, 0, 0, ARROW_W, ARROW_H)
                .setTextureSize(ARROW_W, ARROW_H)
                .build();
    }

    @Override
    public @NotNull RecipeType<AltarRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.koniava.altar.title");
    }

    @Override
    public int getWidth()  { return WIDTH; }

    @Override
    public int getHeight() { return HEIGHT; }

    @Override
    public @NotNull IDrawable getIcon() { return icon; }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, AltarRecipe recipe, IFocusGroup focuses) {
        int duration = Math.max(1, recipe.getProcessingTime());
        IDrawableAnimated animatedArrow = guiHelper.createAnimatedDrawable(
                arrowStatic, duration, IDrawableAnimated.StartDirection.LEFT, false);
        builder.addDrawable(animatedArrow, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder,
                          @NotNull AltarRecipe recipe,
                          @NotNull IFocusGroup focuses) {
        // 底座材料（無順序限制，最多 4 個）
        var ings = recipe.getIngredientList();
        for (int i = 0; i < Math.min(ings.size(), 4); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, ING_X[i], ING_Y[i])
                    .addIngredients(ings.get(i));
        }

        // 催化物（CATALYST role，JEI 用特殊框顯示）
        builder.addSlot(RecipeIngredientRole.CATALYST, CAT_X, CAT_Y)
                .addIngredients(recipe.getCatalyst())
                .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.koniava.altar.catalyst")));

        // 結果
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(@NotNull AltarRecipe recipe, @NotNull IRecipeSlotsView slotsView,
                     @NotNull GuiGraphics gfx, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        Component manaText = Component.translatable("jei.koniava.altar.mana", recipe.getManaCost());
        Component timeText = Component.translatable("jei.koniava.altar.time",
                Math.round(recipe.getProcessingTime() / 20f));
        gfx.drawString(font, manaText, 1, TEXT_Y,     0x3355AA, false);
        gfx.drawString(font, timeText, 1, TEXT_Y + 9, 0x555555, false);
    }
}
