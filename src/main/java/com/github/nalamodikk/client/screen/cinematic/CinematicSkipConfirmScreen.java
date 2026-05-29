package com.github.nalamodikk.client.screen.cinematic;

import com.github.nalamodikk.common.config.ModClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 玩家按 R 跳過 cinematic 時跳出的確認視窗。
 * 設計：
 *   isPauseScreen() = false，cinematic 不會被卡住
 *   是   → 執行傳入的 onConfirm；若勾選「以後不再提示」就寫進 config
 *   否   → 純關閉 (cinematic 繼續)
 *   ESC  → 同「否」
 * 觸發點集中在 {@link CinematicSkipHelper#requestSkip}，所有 cinematic 共用同一個 prompt。
 */
public class CinematicSkipConfirmScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 130;

    private final Runnable onConfirm;
    private Checkbox dontAskBox;

    public CinematicSkipConfirmScreen(Runnable onConfirm) {
        super(Component.translatable("gui.koniava.skip_confirm.title"));
        this.onConfirm = onConfirm;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        int btnY = cy + PANEL_H / 2 - 28;
        int gap = 10;
        int btnW = 90;

        // 是 → 執行跳過；若 don't-ask 勾上就寫 config
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.koniava.skip_confirm.yes"),
                        b -> {
                            if (dontAskBox != null && dontAskBox.selected()) {
                                ModClientConfig.INSTANCE.cinematicSkipDontAsk.set(true);
                            }
                            this.onClose();
                            this.onConfirm.run();
                        })
                .pos(cx - btnW - gap / 2, btnY)
                .size(btnW, 20)
                .build());

        // 否 → 純關閉（cinematic 繼續）
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.koniava.skip_confirm.no"),
                        b -> this.onClose())
                .pos(cx + gap / 2, btnY)
                .size(btnW, 20)
                .build());

        // 以後不再提示
        Checkbox box = Checkbox.builder(
                        Component.translatable("gui.koniava.skip_confirm.dont_ask"),
                        this.font)
                .pos(cx - PANEL_W / 2 + 20, btnY - 28)
                .build();
        this.dontAskBox = box;
        this.addRenderableWidget(box);
    }

    /**
     * 把 dim + panel + 標題 都畫進 renderBackground，而不是 render。
     * 原因：vanilla {@code Screen.render} 第一行就會呼叫 {@code this.renderBackground}，
     * 如果在 render 內呼叫 super.renderBackground 又呼叫 super.render，後者會再觸發一次
     * renderBackground 把先前畫好的 panel 一併模糊掉。
     * 改成只覆寫 renderBackground → vanilla Screen.render 自然會用我們改造過的背景，
     * 然後在 widgets（按鈕、勾選框）之前完成，順序為：背景 → panel → widgets（最上層）。
     */
    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        gg.fill(0, 0, this.width, this.height, 0x66000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        gg.fill(px, py, px + PANEL_W, py + PANEL_H, 0xE0101820);
        gg.renderOutline(px, py, PANEL_W, PANEL_H, 0xFFBFA060);

        gg.drawCenteredString(this.font,
                Component.translatable("gui.koniava.skip_confirm.title"),
                cx, py + 12, 0xFFFFD080);
        gg.drawCenteredString(this.font,
                Component.translatable("gui.koniava.skip_confirm.body"),
                cx, py + 36, 0xFFE0E0E0);
    }
}
