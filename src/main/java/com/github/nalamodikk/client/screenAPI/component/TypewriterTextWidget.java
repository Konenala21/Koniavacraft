package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/**
 * 逐字顯示的文字元件 (打字機效果)。
 */
public class TypewriterTextWidget extends AbstractWidget {
    private Component fullText;
    private int charDelay;
    private int color;
    private boolean centered;

    private int charIndex = 0;
    private int tickCounter = 0;
    private boolean completed = false;

    /**
     * @param x 相對於父容器的 X
     * @param y 相對於父容器的 Y
     * @param fullText 完整文字
     * @param charDelay 每隔幾個 tick 顯示一個字 (1 = 最快)
     * @param color 文字顏色 (例如 0xFFFFFF)
     * @param centered 是否置中顯示
     */
    public TypewriterTextWidget(int x, int y, Component fullText, int charDelay, int color, boolean centered) {
        super(x, y, 0, 0); // 寬高會根據文字動態計算
        this.fullText = fullText;
        this.charDelay = charDelay;
        this.color = color;
        this.centered = centered;
    }

    /**
     * 更新文字內容並重置打字進度。
     */
    public void setText(Component text) {
        this.fullText = text;
        this.charIndex = 0;
        this.tickCounter = 0;
        this.completed = false;
    }

    /**
     * 跳過打字動畫，直接顯示完整文字。
     */
    public void skip() {
        this.charIndex = fullText.getString().length();
        this.completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY) {
        Font font = Minecraft.getInstance().font;
        String rawString = fullText.getString();
        
        // 更新打字進度
        if (!completed) {
            tickCounter++;
            if (tickCounter >= charDelay) {
                tickCounter = 0;
                charIndex++;
                
                // 播放音效 (每 2 個字播放一次，避免太吵)
                if (charIndex % 2 == 0) {
                    Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.5F, 0.3F)
                    );
                }

                if (charIndex >= rawString.length()) {
                    charIndex = rawString.length();
                    completed = true;
                }
            }
        }

        String toDraw = rawString.substring(0, charIndex);
        int drawX = 0;
        if (centered) {
            drawX = -font.width(toDraw) / 2;
        }

        graphics.drawString(font, toDraw, drawX, 0, color, true);
    }
// ... existing code ...
