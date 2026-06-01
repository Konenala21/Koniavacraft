package com.github.nalamodikk.common.loot.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.loot.BossDrop;
import com.github.nalamodikk.common.loot.BossLootEntry;
import com.github.nalamodikk.common.loot.BossLootRegistry;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JEI "Boss Drops" page. One row per {@link BossLootEntry} from
 * {@link BossLootRegistry}: a boss icon on the left, its possible drops in a
 * grid. The drops come from the same shared definition that builds the chest
 * loot table, so the page can never drift from what the boss actually gives.
 * Adding a future boss needs no JEI change: it appears automatically.
 */
public class BossLootCategory implements IRecipeCategory<BossLootEntry> {

    public static final RecipeType<BossLootEntry> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "boss_loot", BossLootEntry.class);

    private static final int COLS = 6;
    private static final int SLOT = 18;
    private static final int TITLE_Y = 2;
    private static final int ICON_X = 4;
    private static final int ICON_Y = 16;
    private static final int DROPS_X = 28;
    private static final int DROPS_Y = 16;

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final int width;
    private final int height;

    public BossLootCategory(IGuiHelper guiHelper) {
        int maxDrops = BossLootRegistry.all().stream().mapToInt(e -> e.drops().size()).max().orElse(COLS);
        int rows = Math.max(1, (maxDrops + COLS - 1) / COLS);
        this.width = DROPS_X + COLS * SLOT;
        this.height = DROPS_Y + rows * SLOT + 2;
        this.background = guiHelper.createBlankDrawable(width, height);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, BossLootRegistry.MIRROR_BOSS.icon());
    }

    @Override
    public @NotNull RecipeType<BossLootEntry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.koniava.boss_loot.title");
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BossLootEntry entry, IFocusGroup focuses) {
        builder.addDrawable(background, 0, 0);
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull BossLootEntry entry, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, ICON_X, ICON_Y).addItemStack(entry.icon());

        List<BossDrop> drops = entry.drops();
        for (int i = 0; i < drops.size(); i++) {
            BossDrop d = drops.get(i);
            int col = i % COLS;
            int row = i / COLS;
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, DROPS_X + col * SLOT, DROPS_Y + row * SLOT)
                    .addItemStack(d.displayStack());
            if (d.guaranteed()) {
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.koniava.boss_loot.guaranteed")));
            } else {
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.koniava.boss_loot.random")));
            }
        }
    }

    @Override
    public void draw(@NotNull BossLootEntry entry, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable(entry.titleKey()), ICON_X, TITLE_Y, 0x404040, false);
    }
}
