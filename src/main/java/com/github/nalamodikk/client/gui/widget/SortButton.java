package com.github.nalamodikk.client.gui.widget;

import com.github.nalamodikk.common.inventory.sort.SortMode;
import com.github.nalamodikk.common.inventory.sort.SortTarget;
import com.github.nalamodikk.common.network.packet.server.inventory.SortContainerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SortButton extends AbstractButton {

    private static SortMode sharedMode = SortMode.BY_TYPE;

    private final SortTarget target;

    public SortButton(int x, int y, int width, int height, SortTarget target) {
        super(x, y, width, height, Component.literal("↕"));
        this.target = target;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        if (!this.clicked(mouseX, mouseY)) return false;
        if (button == 0) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            SortContainerPacket.sendToServer(sharedMode, target);
            return true;
        }
        if (button == 1) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            sharedMode = sharedMode.next();
            return true;
        }
        return false;
    }

    @Override
    public Tooltip getTooltip() {
        String targetKey = target == SortTarget.CONTAINER
            ? "tooltip.koniava.sort_container"
            : "tooltip.koniava.sort_player_inventory";
        String modeKey = "tooltip.koniava.sort_mode." + sharedMode.name().toLowerCase();
        return Tooltip.create(
            Component.translatable(targetKey)
                .append("\n")
                .append(Component.translatable(modeKey))
                .append("\n")
                .append(Component.translatable("tooltip.koniava.sort_right_click_hint"))
        );
    }

    @Override
    public void onPress() {}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

}
