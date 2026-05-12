package com.github.nalamodikk.research.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.research.AspectTokenItem;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.client.utils.gui.GuiRenderUtils;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.register.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AspectSynthesisRecipeCategory implements IRecipeCategory<AspectSynthesisRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "aspect_synthesis");
    public static final RecipeType<AspectSynthesisRecipe> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "aspect_synthesis", AspectSynthesisRecipe.class);

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/gui/research/aspect_synthesis.png");
    private static final ResourceLocation HEX_CELL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 76;
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int CELL_SIZE = 32;
    private static final int INPUT_ONE_X = 24;
    private static final int INPUT_TWO_X = 80;
    private static final int RESULT_X = 137;
    private static final int INPUT_Y = 34;

    private final IDrawable icon;

    public AspectSynthesisRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.RESEARCH_TABLE.get()));
    }

    @Override
    public @NotNull RecipeType<AspectSynthesisRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.koniava.aspect_synthesis");
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
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull AspectSynthesisRecipe recipe, @NotNull IFocusGroup focuses) {
        IIngredientAcceptor<?> inputAspectIngredients = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        inputAspectIngredients.addIngredients(AspectIngredientType.INSTANCE, List.of(recipe.first(), recipe.second()));

        IIngredientAcceptor<?> outputAspectIngredients = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        outputAspectIngredients.addIngredient(AspectIngredientType.INSTANCE, recipe.result());
        outputAspectIngredients.addIngredient(AspectIngredientType.INSTANCE, recipe.result());

        IIngredientAcceptor<?> inputTokenIngredients = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        inputTokenIngredients.addItemStack(AspectTokenItem.forAspect(recipe.first(), !recipe.first().isPrimary()));
        inputTokenIngredients.addItemStack(AspectTokenItem.forAspect(recipe.second(), !recipe.second().isPrimary()));

        IIngredientAcceptor<?> outputTokenIngredients = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        outputTokenIngredients.addItemStack(AspectTokenItem.forAspect(recipe.result(), !recipe.result().isPrimary()));
        outputTokenIngredients.addItemStack(AspectTokenItem.forAspect(recipe.result(), !recipe.result().isPrimary()));

        builder.createFocusLink(
                inputAspectIngredients,
                outputAspectIngredients,
                inputTokenIngredients,
                outputTokenIngredients
        );
    }

    @Override
    public void draw(@NotNull AspectSynthesisRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, WIDTH, HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        drawAspectCell(graphics, recipe.first(), INPUT_ONE_X, INPUT_Y, mouseX, mouseY);
        drawAspectCell(graphics, recipe.second(), INPUT_TWO_X, INPUT_Y, mouseX, mouseY);
        drawAspectCell(graphics, recipe.result(), RESULT_X, INPUT_Y, mouseX, mouseY);
    }

    @Override
    public void getTooltip(@NotNull ITooltipBuilder tooltipBuilder, @NotNull AspectSynthesisRecipe recipe,
                           @NotNull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (isInArea(mouseX, mouseY, INPUT_ONE_X, INPUT_Y, CELL_SIZE, CELL_SIZE)) {
            tooltipBuilder.add(Component.translatable("jei.koniava.aspect_synthesis.input", displayName(recipe.first())));
            return;
        }
        if (isInArea(mouseX, mouseY, INPUT_TWO_X, INPUT_Y, CELL_SIZE, CELL_SIZE)) {
            tooltipBuilder.add(Component.translatable("jei.koniava.aspect_synthesis.input", displayName(recipe.second())));
            return;
        }
        if (isInArea(mouseX, mouseY, RESULT_X, INPUT_Y, CELL_SIZE, CELL_SIZE)) {
            tooltipBuilder.add(Component.translatable("jei.koniava.aspect_synthesis.result", displayName(recipe.result())));
        }
    }

    private void drawAspectCell(GuiGraphics graphics, Aspect aspect, int x, int y, double mouseX, double mouseY) {
        boolean unknown = !aspect.isPrimary() && !ClientResearchCache.hasDiscovered(aspect.getId());
        if (unknown) {
            graphics.renderItem(AspectTokenItem.forAspect(aspect, true), x + 8, y + 8);
        } else {
            int color = aspect.getColor();
            GuiRenderUtils renderUtils = new GuiRenderUtils(graphics.pose());
            renderUtils.blitWithColor(
                    HEX_CELL_TEXTURE,
                    x, y,
                    CELL_SIZE, CELL_SIZE,
                    0f, 0f, 1f, 1f,
                    channel(color, 16), channel(color, 8), channel(color, 0), 1.0f
            );
        }
        if (isInArea(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) {
            graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x44FFFFFF);
        }
    }

    private Component displayName(Aspect aspect) {
        if (!aspect.isPrimary() && !ClientResearchCache.hasDiscovered(aspect.getId())) {
            return Component.translatable("jei.koniava.aspect_synthesis.unknown_aspect");
        }
        return aspect.getName();
    }

    private static boolean isInArea(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static float channel(int color, int shift) {
        return ((color >> shift) & 0xFF) / 255.0F;
    }
}
