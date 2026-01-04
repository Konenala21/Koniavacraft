package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDraggableWindow {
    protected int x, y, width, height;
    protected int minWidth = 60, minHeight = 40;
    protected boolean dragging = false;
    protected boolean resizing = false;
    protected int dragOffsetX, dragOffsetY;
    protected int resizeMargin = 8;
    protected final List<Widget> widgets = new ArrayList<>();

    public AbstractDraggableWindow(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        initWidgets();
    }

    protected abstract void initWidgets();

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(x, y, x + width, y + height, 0xAA222244);
        graphics.renderOutline(x, y, width, height, 0xFFFFFFFF);
        for (Widget widget : widgets) {
            widget.render(graphics, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (inResizeCorner(mouseX, mouseY)) {
            resizing = true;
            return true;
        } else if (inHeader(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }
        for (Widget w : widgets) {
            if (w.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
        resizing = false;
        for (Widget widget : widgets) {
            widget.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseDragged(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        } else if (resizing) {
            width = Math.max(minWidth, mouseX - x);
            height = Math.max(minHeight, mouseY - y);
        }
    }

    protected boolean inHeader(int mx, int my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + 12;
    }

    protected boolean inResizeCorner(int mx, int my) {
        return mx >= x + width - resizeMargin && mx <= x + width && my >= y + height - resizeMargin && my <= y + height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void addWidget(Widget widget) {
        this.widgets.add(widget);
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}

// ➕ 接下來你可以擴充：
// public class MyCraftingWindow extends AbstractDraggableWindow {
//     public MyCraftingWindow(...) {
//         super(...);
//     }
//     @Override
//     protected void initWidgets() {
//         this.addWidget(new Button(x + 5, y + 5, 40, 20, Component.literal("Click"), btn -> {...}));
//     }
// }
