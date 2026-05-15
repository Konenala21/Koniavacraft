package com.github.nalamodikk.common.block.blockentity.altar.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AltarMultiblockCategory implements IRecipeCategory<AltarStructureInfo> {

    public static final RecipeType<AltarStructureInfo> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "altar_multiblock", AltarStructureInfo.class);

    private static final int WIDTH  = 168;
    private static final int HEIGHT = 128;

    // Isometric projection constants (origin = altar at world 0,0,0)
    // screenX = ISO_OX + (wx - wz) * ISO_H
    // screenY = ISO_OY + (wx + wz) * ISO_V - wy * ISO_UP
    private static final int ISO_OX = 84;
    private static final int ISO_OY = 46;
    private static final int ISO_H  = 9;   // horizontal spread per block
    private static final int ISO_V  = 5;   // vertical contribution per x/z block
    private static final int ISO_UP = 14;  // vertical pixels per y-block

    // Toggle button
    private static final int BTN_X = 48;
    private static final int BTN_Y = 112;
    private static final int BTN_W = 72;
    private static final int BTN_H = 14;

    // Block positions rendered in painter's-algorithm order:
    // sorted by (wx+wz) ASC (farther first), then wy ASC (lower first)
    // {wx, wy, wz}
    private static final int[][] RENDER_ORDER = {
        {-3,-2,-3},  // NW pillar bottom   depth=-6
        {-3,-1,-3},  // NW pillar top      depth=-6
        {-3,-2, 0},  // West pedestal      depth=-3
        { 0,-2,-3},  // North pedestal     depth=-3
        {-3,-2, 3},  // SW pillar bottom   depth= 0
        { 3,-2,-3},  // NE pillar bottom   depth= 0
        { 0,-2, 0},  // Center pedestal    depth= 0
        {-3,-1, 3},  // SW pillar top      depth= 0
        { 3,-1,-3},  // NE pillar top      depth= 0
        { 0, 0, 0},  // Altar core         depth= 0  y=0
        { 0,-2, 3},  // South pedestal     depth=+3
        { 3,-2, 0},  // East pedestal      depth=+3
        { 3,-2, 3},  // SE pillar bottom   depth=+6
        { 3,-1, 3},  // SE pillar top      depth=+6
    };

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
                          @NotNull IFocusGroup focuses) {}

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder,
                                   @NotNull AltarStructureInfo recipe,
                                   @NotNull IFocusGroup focuses) {
        builder.addGuiEventListener(new ToggleWidget());
    }

    @Override
    public void draw(@NotNull AltarStructureInfo recipe, @NotNull IRecipeSlotsView slotsView,
                     @NotNull GuiGraphics gfx, double mouseX, double mouseY) {

        drawIsometricStructure(gfx);

        // Toggle button
        var font = Minecraft.getInstance().font;
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

    private void drawIsometricStructure(GuiGraphics gfx) {
        ItemStack pillarStack = showFormed
                ? new ItemStack(ModBlocks.ALTAR_PILLAR.get())
                : new ItemStack(ModBlocks.MANA_BLOCK.get());
        ItemStack pedStack   = new ItemStack(ModBlocks.ASPECT_PEDESTAL.get());
        ItemStack altarStack = new ItemStack(ModBlocks.ASPECT_ALTAR.get());

        for (int[] pos : RENDER_ORDER) {
            int wx = pos[0], wy = pos[1], wz = pos[2];
            ItemStack stack = resolveItem(wx, wy, wz, pillarStack, pedStack, altarStack);
            if (stack.isEmpty()) continue;

            int sx = isoScreenX(wx, wz);
            int sy = isoScreenY(wx, wy, wz);
            gfx.renderItem(stack, sx - 8, sy - 8);
        }
    }

    private ItemStack resolveItem(int wx, int wy, int wz,
                                  ItemStack pillar, ItemStack ped, ItemStack altar) {
        if (wx == 0 && wy == 0 && wz == 0) return altar.copy();
        if (Math.abs(wx) == 3 && Math.abs(wz) == 3) return pillar.copy(); // corner pillars
        return ped.copy(); // all other rendered positions are pedestals
    }

    private static int isoScreenX(int wx, int wz) {
        return ISO_OX + (wx - wz) * ISO_H;
    }

    private static int isoScreenY(int wx, int wy, int wz) {
        return ISO_OY + (wx + wz) * ISO_V - wy * ISO_UP;
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
