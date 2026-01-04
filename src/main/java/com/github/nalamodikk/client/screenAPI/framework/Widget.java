package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.gui.GuiGraphics;

public interface Widget {
    void render(GuiGraphics graphics, int mouseX, int mouseY);
    boolean mouseClicked(int mouseX, int mouseY, int button);
    void mouseReleased(int mouseX, int mouseY, int button);
}
