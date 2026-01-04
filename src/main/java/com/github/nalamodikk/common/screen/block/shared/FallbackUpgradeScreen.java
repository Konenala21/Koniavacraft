package com.github.nalamodikk.common.screen.block.shared;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FallbackUpgradeScreen extends UpgradeScreen{
    public FallbackUpgradeScreen(UpgradeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        // ✅ 顯示錯誤提示訊息（紅色）
        Component errorText = Component.translatable("screen.koniava.upgrade.fallback_error");
        graphics.drawString(this.font, errorText, 8, 6, 0xFF5555, false);
    }

}
