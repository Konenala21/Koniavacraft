package com.github.nalamodikk.common.block.blockentity.altar.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AltarMultiblockCategory implements IRecipeCategory<AltarStructureInfo> {

    public static final RecipeType<AltarStructureInfo> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "altar_multiblock", AltarStructureInfo.class);

    private static final int WIDTH  = 168;
    private static final int HEIGHT = 112;

    // Cell = 16px item + 2px gap = 18px; 3x3 grid = 52px wide
    private static final int CELL   = 18;
    private static final int GRID_W = 3 * CELL - 2;

    // 3 layer columns: Y=-2 (left), Y=-1 (mid), Y=0 (right)
    private static final int[] COL_X  = { 4, 60, 116 };
    private static final int   GRID_Y = 18;

    // Toggle button
    private static final int BTN_X = 49;
    private static final int BTN_Y = 93;
    private static final int BTN_W = 72;
    private static final int BTN_H = 14;

    private boolean showFormed = false;

    private final IDrawable icon;

    public AltarMultiblockCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ASPECT_ALTAR.get()));
    }

    @Override public @NotNull RecipeType<AltarStructureInfo> getRecipeType() { return RECIPE_TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("jei.koniava.altar_multiblock.title"); }
    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder,
                          @NotNull AltarStructureInfo recipe,
                          @NotNull IFocusGroup focuses) {
        // Structure guide — no ingredient slots
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder,
                                   @NotNull AltarStructureInfo recipe,
                                   @NotNull IFocusGroup focuses) {
        builder.addGuiEventListener(new ToggleWidget());
    }

    @Override
    public void draw(@NotNull AltarStructureInfo recipe, @NotNull IRecipeSlotsView slotsView,
                     @NotNull GuiGraphics gfx, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // Layer labels
        String[] labels = { "Y-2", "Y-1", "Y+0" };
        for (int i = 0; i < 3; i++) {
            int lx = COL_X[i] + GRID_W / 2 - font.width(labels[i]) / 2;
            gfx.drawString(font, labels[i], lx, 8, 0x888888, false);
        }

        // Layer grids
        drawLayer(gfx, COL_X[0], GRID_Y, buildLayerMinus2(showFormed));
        drawLayer(gfx, COL_X[1], GRID_Y, buildLayerMinus1(showFormed));
        drawLayer(gfx, COL_X[2], GRID_Y, buildLayerZero());

        // Toggle button
        boolean hovered = mouseX >= BTN_X && mouseX < BTN_X + BTN_W
                       && mouseY >= BTN_Y && mouseY < BTN_Y + BTN_H;
        int bg = showFormed ? 0xFF2E6E42 : (hovered ? 0xFF4A4A6A : 0xFF333355);
        gfx.fill(BTN_X, BTN_Y, BTN_X + BTN_W, BTN_Y + BTN_H, bg);
        gfx.renderOutline(BTN_X, BTN_Y, BTN_W, BTN_H, 0xFF888888);

        String btnText = Component.translatable(
                showFormed ? "jei.koniava.altar_multiblock.btn_unformed"
                           : "jei.koniava.altar_multiblock.btn_formed").getString();
        gfx.drawString(font, btnText,
                BTN_X + BTN_W / 2 - font.width(btnText) / 2,
                BTN_Y + (BTN_H - font.lineHeight) / 2,
                0xFFFFFF, false);
    }

    private void drawLayer(GuiGraphics gfx, int ox, int oy, ItemStack[][] grid) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int x = ox + c * CELL;
                int y = oy + r * CELL;
                gfx.fill(x, y, x + 16, y + 16, 0x55000000);
                gfx.renderOutline(x, y, 16, 16, 0x33888888);
                ItemStack stack = grid[r][c];
                if (stack != null && !stack.isEmpty()) {
                    gfx.renderItem(stack, x, y);
                }
            }
        }
    }

    private ItemStack[][] buildLayerZero() {
        ItemStack[][] g = empty();
        g[1][1] = new ItemStack(ModBlocks.ASPECT_ALTAR.get());
        return g;
    }

    private ItemStack[][] buildLayerMinus1(boolean formed) {
        ItemStack[][] g = empty();
        ItemStack corner = formed
                ? new ItemStack(ModBlocks.ALTAR_PILLAR.get())
                : new ItemStack(ModBlocks.MANA_BLOCK.get());
        g[0][0] = g[0][2] = g[2][0] = g[2][2] = corner.copy();
        return g;
    }

    private ItemStack[][] buildLayerMinus2(boolean formed) {
        ItemStack[][] g = buildLayerMinus1(formed);
        g[1][1] = new ItemStack(ModBlocks.ASPECT_PEDESTAL.get());
        return g;
    }

    private static ItemStack[][] empty() {
        return new ItemStack[3][3];
    }

    private class ToggleWidget implements IJeiGuiEventListener {
        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(BTN_X, BTN_Y, BTN_W, BTN_H);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0) {
                showFormed = !showFormed;
                return true;
            }
            return false;
        }
    }
}
