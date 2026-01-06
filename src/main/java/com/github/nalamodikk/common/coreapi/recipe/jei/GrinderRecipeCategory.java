package com.github.nalamodikk.common.coreapi.recipe.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import com.github.nalamodikk.common.block.blockentity.ore_grinder.OreGrinderBlockEntity;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

/**
 * ⚙️ 粉碎機 JEI 分類顯示
 *
 * 在 JEI 中顯示粉碎機配方的佈局和邏輯
 */
public class GrinderRecipeCategory implements IRecipeCategory<ProcessingRecipe> {

    public static final RecipeType<ProcessingRecipe> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "grinder", ProcessingRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/ore_grinder_gui.png");

    private static final ResourceLocation MANA_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_bar_full.png");

    private static final int WIDTH = 171;
    private static final int HEIGHT = 77;
    private static final int BAR_X = 9;
    private static final int BAR_Y = 17;
    private static final int BAR_WIDTH = 11;
    private static final int BAR_HEIGHT = 49;
    private static final int TIME_Y = 65;
    private static final int OUTPUT_X = 114;
    private static final int OUTPUT_Y = 26;
    private static final int CHANCE_ROW_Y = 44;
    private static final int SLOT_SIZE = 16;
    private static final int ARROW_X = 72;
    private static final int ARROW_Y = 37;
    private static final int ARROW_WIDTH = 34;
    private static final int ARROW_HEIGHT = 8;

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final Component localizedName;
    private int maxManaCostCache = -1;

    public GrinderRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(TEXTURE, 0, 0, WIDTH, HEIGHT)
                .setTextureSize(256, 256)
                .build();

        // 圖標（用於分類選擇面板）
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ORE_GRINDER.get())
        );

        this.localizedName = Component.translatable("block.koniava.ore_grinder");
    }

    @Override
    public @NotNull RecipeType<ProcessingRecipe> getRecipeType() {
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
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ProcessingRecipe recipe, IFocusGroup focuses) {
    }

    /**
     * 🎯 設定配方的槽位佈局
     *
     * 定義輸入、輸出槽位在 JEI GUI 中的位置
     */
    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ProcessingRecipe recipe, @NotNull IFocusGroup focuses) {
        // 輸入槽位（左側）
        if (!recipe.getInputs().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 30, 35)
                    .addIngredients(recipe.getInputs().get(0));
        }

        if (recipe.getInputs().size() > 1) {
            builder.addSlot(RecipeIngredientRole.INPUT, 48, 35)
                    .addIngredients(recipe.getInputs().get(1));
        }

        // 主輸出槽位（右側）
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 26)
                .addItemStack(recipe.getMainOutput());

        // 副輸出槽位（下方）
        int chanceOutputX = 114;
        for (int i = 0; i < recipe.getChanceOutputs().size() && i < 2; i++) {
            ProcessingRecipe.ChanceOutput chanceOutput = recipe.getChanceOutputs().get(i);
            int chancePercent = Math.round(chanceOutput.getChance() * 100);
            IRecipeSlotBuilder slotBuilder = builder.addSlot(RecipeIngredientRole.OUTPUT, chanceOutputX + i * 18, 44)
                    .addItemStack(chanceOutput.getOutput());
            slotBuilder.addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.koniava.grinder.chance_detail", chancePercent))
            );
        }
    }

    /**
     * 🖼️ 自定義渲染（顯示魔力消耗等資訊）
     */
    @Override
    public void draw(@NotNull ProcessingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // 繪製處理時間文字 (置中)
        Component timeText = Component.translatable("jei.koniava.grinder.time", recipe.getProcessingTime());
        int timeX = Math.max(0, (WIDTH - font.width(timeText)) / 2);
        guiGraphics.drawString(font, timeText, timeX, TIME_Y, 0x404040, false);

        // 繪製魔力條 (依照魔力合成台的方式)
        int manaCost = recipe.getManaCost();
        int maxMana = Math.max(getMaxManaCost(), manaCost);

        int filledHeight = maxMana > 0 ? (int) ((manaCost / (float) maxMana) * BAR_HEIGHT) : 0;
        if (manaCost > 0 && filledHeight == 0) {
            filledHeight = 3;
        }
        int filledY = BAR_Y + BAR_HEIGHT - filledHeight;

        if (filledHeight > 0) {
            int textureHeight = 49;
            int v = BAR_HEIGHT - filledHeight;
            guiGraphics.blit(MANA_BAR_TEXTURE,
                    BAR_X, filledY,
                    0, v,
                    BAR_WIDTH, filledHeight,
                    BAR_WIDTH, textureHeight
            );
        }

        // 進度箭頭（以配方時間循環動畫）
        int totalTicks = Math.max(1, recipe.getProcessingTime());
        long nowTicks = System.currentTimeMillis() / 50L;
        int progress = (int) (nowTicks % totalTicks);
        int fillWidth = Math.max(0, (ARROW_WIDTH * progress) / totalTicks);
        if (fillWidth > 0) {
            guiGraphics.blit(TEXTURE,
                    ARROW_X, ARROW_Y,
                    176, 54,
                    fillWidth, ARROW_HEIGHT,
                    256, 256
            );
        }
    }

    @Override
    public void getTooltip(@NotNull ITooltipBuilder tooltipBuilder, @NotNull ProcessingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (isInArea(mouseX, mouseY, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            tooltipBuilder.add(Component.translatable("tooltip.koniava.mana_cost", recipe.getManaCost()));
            return;
        }

        int chanceBaseX = OUTPUT_X;
        for (int i = 0; i < recipe.getChanceOutputs().size() && i < 2; i++) {
            if (isInArea(mouseX, mouseY, chanceBaseX + i * 18, CHANCE_ROW_Y, SLOT_SIZE, SLOT_SIZE)) {
                int chancePercent = Math.round(recipe.getChanceOutputs().get(i).getChance() * 100);
                tooltipBuilder.add(Component.translatable("jei.koniava.grinder.chance_detail", chancePercent));
                return;
            }
        }
    }

    private boolean isInArea(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int getMaxManaCost() {
        if (maxManaCostCache > 0) {
            return maxManaCostCache;
        }

        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }

        maxManaCostCache = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(recipe -> "grinder".equals(recipe.getMachineType()))
                .mapToInt(ProcessingRecipe::getManaCost)
                .max()
                .orElse(0);

        return maxManaCostCache;
    }
}
