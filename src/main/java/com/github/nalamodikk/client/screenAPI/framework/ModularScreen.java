package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

public abstract class ModularScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    
    protected Panel rootPanel;

    public ModularScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // 初始化 RootPanel，預設使用背景圖大小
        this.rootPanel = new Panel(leftPos, topPos, imageWidth, imageHeight);
        
        // 建構 GUI (由子類別實作)
        buildGui(rootPanel);
    }

    /**
     *在此方法中組裝您的 GUI。
     * 用法：rootPanel.add(new EnergyWidget(...));
     */
    protected abstract void buildGui(Panel root);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // 繪製 Tooltip (在最上層)
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 1. 繪製 RootPanel (背景層與 Widget)
        // 注意：renderBg 的 mouseX, mouseY 是絕對座標
        // RootPanel 已經設置在 (leftPos, topPos)
        // 它的 render 會 translate 到正確位置
        
        rootPanel.render(graphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 繪製標題和背包標籤
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        super.renderTooltip(graphics, x, y);
        
        // 檢查 Widget 的 Tooltip
        List<Component> tooltip = rootPanel.getChildrenTooltip(x, y);
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, x, y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先讓 Panel 處理
        if (rootPanel.mouseClicked((int)mouseX, (int)mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        rootPanel.mouseReleased((int)mouseX, (int)mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
