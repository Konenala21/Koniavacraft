package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * 基礎元件實作。
 * 解決座標痛點的核心：所有座標都是「相對」於父容器的。
 */
public abstract class AbstractWidget implements Widget {
    protected int x, y;
    protected int width, height;
    protected boolean visible = true;
    protected Widget parent; // 🔗 連結父容器

    public AbstractWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 獲取在螢幕上的絕對 X 座標。
     * 自動加上所有父容器的偏移量。
     */
    public int getScreenX() {
        return (parent instanceof AbstractWidget parentWidget ? parentWidget.getScreenX() : 0) + x;
    }

    /**
     * 獲取在螢幕上的絕對 Y 座標。
     */
    public int getScreenY() {
        return (parent instanceof AbstractWidget parentWidget ? parentWidget.getScreenY() : 0) + y;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!visible) return;
        
        // 🎨 傳入的 graphics 通常已經處理了 PoseStack，
        // 但為了保險，我們使用計算出的絕對座標來繪製內容
        // 或者，我們可以在這裡 pushPose() -> translate(x, y) -> draw -> popPose()
        // 這樣 renderContent 裡面就可以永遠從 (0,0) 開始畫！這才是解決座標痛點的終極方案。
        
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        
        // 轉換滑鼠座標為相對座標，方便子元件判斷 hover
        renderWidget(graphics, mouseX - x, mouseY - y, mouseX, mouseY);
        
        graphics.pose().popPose();
    }

    /**
     * 子類別實作此方法來繪製內容。
     * 💡 重點：在這裡，(0, 0) 就是元件的左上角！不用管 guiLeft 或父容器在哪！
     */
    protected abstract void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY);

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        // mouseX/mouseY 是絕對座標，需要轉換為相對於此 Widget 的座標
        int screenX = getScreenX();
        int screenY = getScreenY();
        return onMouseClicked(mouseX - screenX, mouseY - screenY, button);
    }
    
    protected boolean onMouseClicked(int localMouseX, int localMouseY, int button) {
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (visible) {
            // mouseX/mouseY 是絕對座標，需要轉換為相對於此 Widget 的座標
            int screenX = getScreenX();
            int screenY = getScreenY();
            onMouseReleased(mouseX - screenX, mouseY - screenY, button);
        }
    }
    
    protected void onMouseReleased(int localMouseX, int localMouseY, int button) {}

    public boolean isMouseOver(int mouseX, int mouseY) {
        // 這裡 mouseX/Y 通常是傳入的相對座標 (如果父容器有正確處理)
        // 但為了安全，我們假設傳入的是相對座標 (因為 render 裡傳了 mouseX - x)
        // 等等，原版 Minecraft 事件傳的是絕對座標。
        // 我們需要在 Parent 傳遞時進行座標轉換。
        
        // 簡單起見，我們檢查絕對座標：
        int absX = getScreenX();
        int absY = getScreenY();
        return mouseX >= absX && mouseX < absX + width && mouseY >= absY && mouseY < absY + height;
    }

    public void setParent(Widget parent) {
        this.parent = parent;
    }
    
    public List<Component> getTooltip() {
        return Collections.emptyList();
    }
    
    // --- 鏈式設定方法 (Builder Pattern) ---
    public AbstractWidget setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public AbstractWidget setSize(int w, int h) {
        this.width = w;
        this.height = h;
        return this;
    }

    // --- Public Getters/Setters (for FlexLayout) ---
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
}
