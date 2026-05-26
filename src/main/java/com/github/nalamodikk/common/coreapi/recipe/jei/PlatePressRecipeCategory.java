package com.github.nalamodikk.common.coreapi.recipe.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressRecipe;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

import mezz.jei.api.gui.builder.ITooltipBuilder;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

public class PlatePressRecipeCategory implements IRecipeCategory<ManaPlatePressRecipe> {

    public static final RecipeType<ManaPlatePressRecipe> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "plate_press", ManaPlatePressRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_plate_press_gui.png");

    private static final ResourceLocation MANA_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_bar_full.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 76;
    private static final int BAR_X = 9;
    private static final int BAR_Y = 17;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 48;
    private static final int INPUT_X = 48;
    private static final int INPUT_Y = 35;
    private static final int OUTPUT_X = 122;
    private static final int OUTPUT_Y = 34;
    private static final int ARROW_X = 67;
    private static final int ARROW_Y = 36;
    private static final int ARROW_WIDTH = 44;
    private static final int ARROW_HEIGHT = 12;

    private final IGuiHelper guiHelper;
    private final IDrawableStatic background;
    private final IDrawable icon;
    private final IDrawableStatic arrowStatic;
    private final Component localizedName;
    private int maxManaCostCache = -1;

    public PlatePressRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.background = guiHelper.drawableBuilder(TEXTURE, 0, 0, WIDTH, HEIGHT)
                .setTextureSize(256, 256)
                .build();
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.MANA_PLATE_PRESS.get())
        );
        this.localizedName = Component.translatable("block.koniava.mana_plate_press");
        this.arrowStatic = guiHelper.drawableBuilder(TEXTURE, 176, 52, ARROW_WIDTH, ARROW_HEIGHT)
                .setTextureSize(256, 256)
                .build();
    }

    @Override
    public @NotNull RecipeType<ManaPlatePressRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return localizedName;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ManaPlatePressRecipe recipe, IFocusGroup focuses) {
        builder.addDrawable(background, 0, 0);
        int duration = Math.max(1, recipe.getPressingTime());
        IDrawableAnimated arrowAnimated = guiHelper.createAnimatedDrawable(
                arrowStatic, duration, IDrawableAnimated.StartDirection.LEFT, false);
        builder.addDrawable(arrowAnimated, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ManaPlatePressRecipe recipe, @NotNull IFocusGroup focuses) {
        int count = recipe.getInputCount();
        List<ItemStack> inputStacks = Arrays.stream(recipe.getInput().getItems())
                .map(s -> { ItemStack copy = s.copy(); copy.setCount(count); return copy; })
                .toList();
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addItemStacks(inputStacks);
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(@NotNull ManaPlatePressRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int manaCost = recipe.getManaCost();
        int maxMana = Math.max(getMaxManaCost(), manaCost);

        int filledHeight = maxMana > 0 ? (int) ((manaCost / (float) maxMana) * BAR_HEIGHT) : 0;
        if (manaCost > 0 && filledHeight == 0) filledHeight = 3;
        int yOffset = BAR_HEIGHT - filledHeight;

        if (filledHeight > 0) {
            guiGraphics.blit(MANA_BAR_TEXTURE,
                    BAR_X, BAR_Y + yOffset,
                    0, yOffset,
                    BAR_WIDTH, filledHeight,
                    BAR_WIDTH, BAR_HEIGHT
            );
        }
    }

    @Override
    public void getTooltip(@NotNull ITooltipBuilder tooltipBuilder, @NotNull ManaPlatePressRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (isInArea(mouseX, mouseY, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            tooltipBuilder.add(Component.translatable("tooltip.koniava.mana_cost", recipe.getManaCost()));
        }
    }

    private boolean isInArea(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int getMaxManaCost() {
        if (maxManaCostCache > 0) return maxManaCostCache;
        var level = Minecraft.getInstance().level;
        if (level == null) return 0;
        maxManaCostCache = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.PLATE_PRESS_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .mapToInt(ManaPlatePressRecipe::getManaCost)
                .max()
                .orElse(0);
        return maxManaCostCache;
    }
}
