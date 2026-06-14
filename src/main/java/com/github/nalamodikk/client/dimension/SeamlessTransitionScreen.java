package com.github.nalamodikk.client.dimension;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;

import java.util.function.BooleanSupplier;

/**
 * 透明的維度轉場「載入世界」畫面，用在飛船 OVERWORLD &lt;-&gt; SPACE 無縫切換。
 *
 * <p>changeDimension 期間 vanilla 用 {@link ReceivingLevelScreen} 蓋住空檔（畫不透明背景 + DOWNLOADING
 * TERRAIN 文字），那期間我們的太空天空根本沒在 render → 漏出載入閃屏。透過 NeoForge
 * {@code DimensionTransitionScreenManager}（RegisterDimensionTransitionScreenEvent）把 OVERWORLD&lt;-&gt;SPACE
 * 的轉場畫面換成這個：什麼都不畫 → 背後的世界 + 太空天空直接透出來。仍繼承本體的 tick()，區塊到齊
 * （levelReceived）時照樣自動關閉。比攔 ScreenEvent 可靠（官方擴充點，不靠封包/時序）。
 */
public class SeamlessTransitionScreen extends ReceivingLevelScreen {

    public SeamlessTransitionScreen(BooleanSupplier levelReceived, Reason reason) {
        super(levelReceived, reason);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 不畫任何東西(連背景都不畫) → 世界 + 太空天空透出來,沒有載入閃屏
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // no-op:別畫 dirt/blur 背景
    }
}
