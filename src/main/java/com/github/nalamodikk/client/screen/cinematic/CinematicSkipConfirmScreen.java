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

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // 用 vanilla 模糊背景（renderBlurredBackground 由 super.renderBackground 觸發）
        // 之上再蓋一層深色 dim 讓 panel 對比清楚
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        gg.fill(0, 0, this.width, this.height, 0x66000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        // 視覺面板邊框
        gg.fill(px, py, px + PANEL_W, py + PANEL_H, 0xE0101820);
        gg.renderOutline(px, py, PANEL_W, PANEL_H, 0xFFBFA060);

        // 標題
        gg.drawCenteredString(this.font,
                Component.translatable("gui.koniava.skip_confirm.title"),
                cx, py + 12, 0xFFFFD080);

        // 內文
        gg.drawCenteredString(this.font,
                Component.translatable("gui.koniava.skip_confirm.body"),
                cx, py + 36, 0xFFE0E0E0);

        super.render(gg, mouseX, mouseY, partialTick);
    }
}
