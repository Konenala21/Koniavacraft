package com.github.nalamodikk.client.screenAPI.component.button;

import com.github.nalamodikk.client.screenAPI.TooltipSupplier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class TooltipButton extends AbstractButton {
    private ResourceLocation texture;
    private int texWidth;
    private int texHeight;
    private final OnPress onPress;
    private final TooltipSupplier tooltipSupplier;
    public boolean hovering; // 新增的狀態變量，用於跟踪是否需要顯示工具提示

    /**
     * Constructs a GenericButtonWithTooltip.
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
     * @param tooltipSupplier The supplier for the tooltip text.
     */
    public TooltipButton(int x, int y, int width, int height, Component message, ResourceLocation texture, int texWidth, int texHeight, OnPress onPress, TooltipSupplier tooltipSupplier) {
        super(x, y, width, height, message);
        this.texture = texture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.onPress = onPress;
        this.tooltipSupplier = tooltipSupplier;
        this.hovering = false; // 初始為不懸停
    }


    public TooltipButton(
            int x, int y, int width, int height,
            Component message,
            ResourceLocation texture,
            int texWidth, int texHeight,
            OnPress onPress,
            TooltipSupplier.Positioned tooltipPositioned
    ) {
        this(x, y, width, height, message, texture, texWidth, texHeight, onPress,
                () -> tooltipPositioned.getTooltip(0, 0)); // 包成舊的 tooltipSupplier
    }

    /**
     * Updates the button's texture to a new texture.
     *
     * @param newTexture The new texture to set.
     * @param newTexWidth The width of the new texture.
     * @param newTexHeight The height of the new texture.
     */
    public void setTexture(ResourceLocation newTexture, int newTexWidth, int newTexHeight) {
        this.texture = newTexture;
        this.texWidth = newTexWidth;
        this.texHeight = newTexHeight;
    }

    @Override
    public Tooltip getTooltip() {
        List<Component> components = tooltipSupplier.getTooltip();
        if (components.isEmpty()) {
            return null;
        }

        // 拼接工具提示中的所有行，並返回
        MutableComponent combinedComponent = Component.empty();
        for (int i = 0; i < components.size(); i++) {
            combinedComponent = combinedComponent.append(components.get(i));
            if (i < components.size() - 1) {
                combinedComponent = combinedComponent.append("\n");
            }
        }

        return Tooltip.create(combinedComponent);
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
