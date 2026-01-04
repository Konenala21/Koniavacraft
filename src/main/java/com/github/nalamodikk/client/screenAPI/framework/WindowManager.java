package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class WindowManager {
    private final List<AbstractDraggableWindow> windows = new ArrayList<>();

    public void addWindow(AbstractDraggableWindow window) {
        windows.add(window);
    }

    public void removeWindow(AbstractDraggableWindow window) {
        windows.remove(window);
    }

    public void renderAll(GuiGraphics graphics, int mouseX, int mouseY) {
        for (AbstractDraggableWindow window : windows) {
            window.render(graphics, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = windows.size() - 1; i >= 0; i--) { // 從最上層開始檢查
            AbstractDraggableWindow window = windows.get(i);
            if (window.contains((int) mouseX, (int) mouseY)) {
                bringToFront(window);
                return window.mouseClicked((int) mouseX, (int) mouseY, button);
            }
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (AbstractDraggableWindow window : windows) {
            window.mouseReleased((int) mouseX, (int) mouseY, button);
        }
    }

    public void mouseDragged(double mouseX, double mouseY) {
        for (AbstractDraggableWindow window : windows) {
            window.mouseDragged((int) mouseX, (int) mouseY);
        }
    }

    private void bringToFront(AbstractDraggableWindow window) {
        windows.remove(window);
        windows.add(window);
    }

    public List<AbstractDraggableWindow> getWindows() {
        return windows;
    }
}
