package com.github.nalamodikk.client.screen.ship;

import com.github.nalamodikk.common.network.packet.server.ship.ShipThrottlePacket;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 飛船控制台（右鍵核心開）：油門滑桿（設巡航油門）+ 燃料/引擎/速度上限讀數。
 * client 端 Screen，油門改動走 ShipThrottlePacket（帶船 ID，server 權威 + 同步回 HUD）。
 */
public class ShipControlScreen extends Screen {
    private final ShipEntity ship;

    public ShipControlScreen(ShipEntity ship) {
        super(Component.translatable("screen.koniava.ship_control.title"));
        this.ship = ship;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 24;
        addRenderableWidget(new ThrottleSlider(cx - 100, y, 200, 20, ship.getThrottle()));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 50, y + 64, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        if (ship.isRemoved()) { onClose(); return; }
        int cx = width / 2;
        g.drawCenteredString(font, getTitle(), cx, height / 2 - 54, 0xFFFFFFFF);
        int y = height / 2 + 6;
        g.drawCenteredString(font, Component.translatable("screen.koniava.ship_control.fuel",
                ship.getDisplayFuel(), ship.getMaxFuel()), cx, y, 0xFFAACCFF);
        g.drawCenteredString(font, Component.translatable("screen.koniava.ship_control.engines",
                ship.getEngineCount()), cx, y + 12, 0xFFCCCCCC);
    }

    @Override
    public boolean isPauseScreen() { return false; } // 不暫停(船可能在動/補燃料)

    private class ThrottleSlider extends AbstractSliderButton {
        ThrottleSlider(int x, int y, int w, int h, double init) {
            super(x, y, w, h, Component.empty(), init);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.koniava.ship_control.throttle", Math.round(value * 100)));
        }

        @Override
        protected void applyValue() {
            float t = Mth.clamp((float) value, 0f, 1f);
            ship.setThrottle(t);                               // 本地即時
            ShipThrottlePacket.sendToServer(ship.getId(), t);  // server 權威
        }
    }
}
