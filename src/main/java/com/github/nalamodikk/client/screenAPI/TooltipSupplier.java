package com.github.nalamodikk.client.screenAPI;

import net.minecraft.network.chat.Component;

import java.util.List;

@FunctionalInterface
public interface TooltipSupplier {
    List<Component> getTooltip();

    // 新增：支援滑鼠位置的 tooltip lambda
    @FunctionalInterface
    interface Positioned {
        List<Component> getTooltip(double mouseX, double mouseY);
    }
}
