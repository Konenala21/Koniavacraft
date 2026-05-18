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
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AltarRecipeCategory implements IRecipeCategory<AltarRecipe> {

    public static final RecipeType<AltarRecipe> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "altar", AltarRecipe.class);

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/full-arrow.png");

    // 環形佈局參數（像 TC4 聚合祭壇）
    // 催化物在圓心，材料繞一圈，箭頭往右，結果在右邊
    private static final int CAT_CX   = 30;   // 圓心 X
    private static final int CAT_CY   = 33;   // 圓心 Y
    private static final int RING_R   = 22;   // 材料槽圓心到圓心距離（px）
    private static final int HALF     = 8;    // 16px slot 的一半
    private static final int CAT_X    = CAT_CX - HALF;  // 催化物槽左上 X
    private static final int CAT_Y    = CAT_CY - HALF;  // 催化物槽左上 Y
    private static final int ARROW_X  = 64;
    private static final int ARROW_Y  = CAT_CY - 8;
    private static final int ARROW_W  = 22;
    private static final int ARROW_H  = 16;
    private static final int OUT_X    = ARROW_X + ARROW_W + 2;
    private static final int OUT_Y    = CAT_Y;
    private static final int TEXT_Y   = 60;
    private static final int WIDTH    = OUT_X + 16 + 6;   // 110
    private static final int HEIGHT   = 82;

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

    @Override public @NotNull RecipeType<AltarRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("jei.koniava.altar.title"); }
    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

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
        List<Ingredient> ings = recipe.getIngredientList();
        int n = Math.min(ings.size(), 8);

        // 材料槽：繞催化物環形排列，從頂端開始順時針
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2.0 + i * 2.0 * Math.PI / n;
            int sx = (int) Math.round(CAT_CX + RING_R * Math.cos(angle)) - HALF;
            int sy = (int) Math.round(CAT_CY + RING_R * Math.sin(angle)) - HALF;
            builder.addSlot(RecipeIngredientRole.INPUT, sx, sy)
                    .addIngredients(ings.get(i));
        }

        // 催化物（圓心）
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
        gfx.drawString(font,
                Component.translatable("jei.koniava.altar.mana", recipe.getManaCost()),
                1, TEXT_Y, 0x3355AA, false);
        gfx.drawString(font,
                Component.translatable("jei.koniava.altar.time", Math.round(recipe.getProcessingTime() / 20f)),
                1, TEXT_Y + 9, 0x555555, false);
        if (recipe.getMinTier() > 0) {
            gfx.drawString(font,
                    Component.translatable("jei.koniava.altar.tier", recipe.getMinTier()),
                    1, TEXT_Y + 18, 0xAA6600, false);
        }
    }
}
