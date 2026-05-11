package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.research.ResearchGate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * A widget that displays a "Locked" overlay if a certain research is not completed.
 * It usually covers the whole machine area or specific functional parts.
 */
public class ResearchLockWidget extends AbstractWidget {

    private final String machineId;

    public ResearchLockWidget(int x, int y, int width, int height, String machineId) {
        super(x, y, width, height);
        this.machineId = machineId;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
        if (!ResearchGate.isUnlockedOnClient(machineId)) {
            // Render a semi-transparent dark overlay
            graphics.fill(0, 0, width, height, 0xAA000000);
            
            // Draw a lock icon or text in the center
            String text = "LOCKED";
            int textWidth = graphics.bufferSource().getFont().width(text);
            graphics.drawString(graphics.bufferSource().getFont(), text, 
                    (width - textWidth) / 2, (height - 8) / 2, 0xFFFF5555, false);
        }
    }

    @Override
    public List<Component> getTooltip() {
        if (!ResearchGate.isUnlockedOnClient(machineId)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("research.koniava.locked_gui").withStyle(net.minecraft.ChatFormatting.RED));
            
            ResourceLocation required = ResearchGate.getRequiredResearch(machineId);
            if (required != null) {
                tooltip.add(Component.translatable("research.koniava.requires")
                        .append(": ")
                        .append(Component.translatable("research." + required.getNamespace() + "." + required.getPath()))
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
            return tooltip;
        }
        return List.of();
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Only block interactions if locked
        return !ResearchGate.isUnlockedOnClient(machineId) && super.isMouseOver(mouseX, mouseY);
    }
}
