package com.github.nalamodikk.common.coreapi.recipe.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/**
 * ✨ 富集機 JEI 分類顯示
 *
 * 在 JEI 中顯示富集機配方的佈局和邏輯
 */
public class EnricherRecipeCategory implements IRecipeCategory<ProcessingRecipe> {

    public static final RecipeType<ProcessingRecipe> RECIPE_TYPE =
            RecipeType.create(KoniavacraftMod.MOD_ID, "enricher", ProcessingRecipe.class);

    private static final int WIDTH = 150;
    private static final int HEIGHT = 80;

    private final IDrawable icon;
    private final Component localizedName;

    public EnricherRecipeCategory(IGuiHelper guiHelper) {
        // 圖標（用於分類選擇面板）
        // TODO: 改為 ModBlocks.ORE_ENRICHER.get() 當 Block 註冊後
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(Items.LAPIS_LAZULI) // 暫時用青金石代替
        );

        this.localizedName = Component.translatable("jei.koniava.enricher");
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

    /**
     * 🎯 設定配方的槽位佈局
     *
     * 定義輸入、輸出槽位在 JEI GUI 中的位置
     */
    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ProcessingRecipe recipe, @NotNull IFocusGroup focuses) {
        // 輸入槽位（左側）
        if (!recipe.getInputs().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 30)
                    .addIngredients(recipe.getInputs().get(0));
        }

        if (recipe.getInputs().size() > 1) {
            builder.addSlot(RecipeIngredientRole.INPUT, 30, 30)
                    .addIngredients(recipe.getInputs().get(1));
        }

        // 主輸出槽位（右側）
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 30)
                .addItemStack(recipe.getMainOutput());

        // 副輸出槽位（下方）
        int chanceOutputX = 90;
        for (int i = 0; i < recipe.getChanceOutputs().size() && i < 2; i++) {
            ProcessingRecipe.ChanceOutput chanceOutput = recipe.getChanceOutputs().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, chanceOutputX + i * 20, 50)
                    .addItemStack(chanceOutput.getOutput());
        }
    }

    /**
     * 🖼️ 自定義渲染（顯示魔力消耗等資訊）
     */
    @Override
    public void draw(@NotNull ProcessingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 繪製魔力消耗文字
        String manaText = "§b魔力: §e" + recipe.getManaCost();
        guiGraphics.drawString(null, manaText, 10, 10, 0xFFFFFF, false);

        // 繪製處理時間文字
        String timeText = "§b時間: §e" + recipe.getProcessingTime() + " ticks";
        guiGraphics.drawString(null, timeText, 10, 65, 0xFFFFFF, false);

        // 繪製副產物機率
        for (int i = 0; i < recipe.getChanceOutputs().size() && i < 2; i++) {
            ProcessingRecipe.ChanceOutput output = recipe.getChanceOutputs().get(i);
            String chanceText = String.format("%.0f%%", output.getChance() * 100);
            guiGraphics.drawString(null, chanceText, 90 + i * 20, 60, 0xFFFFFF, false);
        }
    }
}
