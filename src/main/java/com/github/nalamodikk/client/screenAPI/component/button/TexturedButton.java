package com.github.nalamodikk.client.screenAPI.component.button;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TexturedButton extends AbstractButton {
    private final ResourceLocation texture;
    private final int texWidth;
    private final int texHeight;
    private final OnPress onPress;

    /**
     * Constructs a UniversalTexturedButton.
     *
     * @param x The x position of the button.
     * @param y The y position of the button.
     * @param width The width of the button.
     * @param height The height of the button.
     * @param message The text label on the button.
     * @param texture The texture to use for the button background.
     * @param texWidth The actual width of the texture.
     * @param texHeight The actual height of the texture.
     * @param onPress The action to perform when the button is pressed.
     */
    public TexturedButton(int x, int y, int width, int height, Component message, ResourceLocation texture, int texWidth, int texHeight, OnPress onPress) {
        super(x, y, width, height, message);
        this.texture = texture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();

        // Draw button background texture
        RenderSystem.setShaderTexture(0, this.texture);
        graphics.blit(this.texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.texWidth, this.texHeight);

        // Draw button label text
        int textColor = this.isHoveredOrFocused() ? 0xFFFFFF : 0xAAAAAA;
        graphics.drawCenteredString(minecraft.font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        this.defaultButtonNarrationText(pNarrationElementOutput);
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(AbstractButton button);
    }
}
