package com.github.nalamodikk.research.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.utils.gui.GuiRenderUtils;
import com.github.nalamodikk.common.item.research.AspectTokenItem;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.skill.reaction.ReactionInfo;
import com.github.nalamodikk.register.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JEI「本源反應」分頁:輸入本源(代表) → 反應名稱 + 效果說明。資料來自 {@link ReactionInfo}。
 * 讓玩家在遊戲內查得到隱藏的化學反應(放對哪兩三個本源會觸發什麼)。
 */
public class ReactionCategory implements IRecipeCategory<ReactionInfo> {

    public static final RecipeType<ReactionInfo> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "aspect_reaction", ReactionInfo.class);

    private static final ResourceLocationRef HEX = new ResourceLocationRef("textures/gui/research/hex_cell.png");
    private static final int WIDTH = 168;
    private static final int HEIGHT = 46;
    private static final int CELL = 18;
    private static final int CELL_Y = 4;
    private static final int TEXT_X = 4 + 3 * (CELL + 2) + 10; // 輸入區之後

    private final IDrawable background;
    private final IDrawable icon;

    public ReactionCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModItems.SPELL_CORE.get()));
    }

    @Override public @NotNull RecipeType<ReactionInfo> getRecipeType() { return RECIPE_TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("jei.koniava.aspect_reaction"); }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, ReactionInfo recipe, IFocusGroup focuses) {
        builder.addDrawable(background, 0, 0);
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ReactionInfo recipe, @NotNull IFocusGroup focuses) {
        // 用本源代幣當可點 ingredient(右鍵能查),視覺另外用 hex cell 畫。
        var slot = builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST);
        for (Aspect a : recipe.inputs()) {
            slot.addItemStack(AspectTokenItem.forAspect(a, !a.isPrimary()));
        }
    }

    @Override
    public void draw(@NotNull ReactionInfo recipe, @NotNull IRecipeSlotsView slots,
                     @NotNull GuiGraphics g, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        List<Aspect> inputs = recipe.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            drawHex(g, inputs.get(i), 4 + i * (CELL + 2), CELL_Y);
        }
        // 反應名稱(亮色) + 說明(換行)
        Component name = Component.translatable(recipe.nameKey());
        g.drawString(font, name, TEXT_X, 4, 0xFFE6C84A, false);
        int y = 4 + font.lineHeight + 3;
        int wrap = WIDTH - TEXT_X - 2;
        for (FormattedText line : font.getSplitter().splitLines(
                Component.translatable(recipe.descKey()), wrap, net.minecraft.network.chat.Style.EMPTY)) {
            g.drawString(font, line.getString(), TEXT_X, y, 0xFFB0B0B0, false);
            y += font.lineHeight + 1;
        }
    }

    private void drawHex(GuiGraphics g, Aspect aspect, int x, int y) {
        int color = aspect.getColor();
        GuiRenderUtils ru = new GuiRenderUtils(g.pose());
        ru.blitWithColor(HEX.loc, x, y, CELL, CELL, 0f, 0f, 1f, 1f,
                ch(color, 16), ch(color, 8), ch(color, 0), 1.0f);
    }

    private static float ch(int color, int shift) { return ((color >> shift) & 0xFF) / 255.0F; }

    /** 小包裝避免在欄位宣告處寫 inline 全路徑。 */
    private record ResourceLocationRef(net.minecraft.resources.ResourceLocation loc) {
        ResourceLocationRef(String path) {
            this(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path));
        }
    }
}
