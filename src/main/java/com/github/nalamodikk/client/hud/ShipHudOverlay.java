package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 駕駛飛船時的 HUD：油門 % + 引擎數 + 燃料條。只有坐在駕駛位的玩家會看到。
 * 油門用滾輪調（ShipThrottleClientHandler），燃料由 server 同步（DATA_FUEL）。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipHudOverlay {
    private ShipHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        int seat = ship.seatIndexOf(mc.player);
        if (seat < 0 || seat >= ShipEntity.MAX_DRIVERS) return; // 只有駕駛看到

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int barW = 160;
        int x = mc.getWindow().getGuiScaledWidth() / 2 - barW / 2;
        int y = mc.getWindow().getGuiScaledHeight() - 72;

        int throttlePct = Math.round(ship.getThrottle() * 100f);
        int fuel = ship.getDisplayFuel();
        int maxFuel = ship.getMaxFuel();

        g.drawString(font, Component.translatable("hud.koniava.ship.throttle", throttlePct + "%", ship.getEngineCount()),
                x, y, 0xFFFFFFFF, true);

        int barH = 6, by = y + 12;
        g.fill(x - 1, by - 1, x + barW + 1, by + barH + 1, 0xFF000000);
        g.fill(x, by, x + barW, by + barH, 0xFF333333);
        if (maxFuel > 0) {
            int fillW = (int) (barW * Math.min(1.0, (double) fuel / maxFuel));
            int color = fuel <= 0 ? 0xFFFF5555 : 0xFF55AAFF; // 空=紅、有燃料=藍
            g.fill(x, by, x + fillW, by + barH, color);
        }
        g.drawString(font, Component.translatable("hud.koniava.ship.fuel", fuel, maxFuel),
                x, by + barH + 2, 0xFFAAAAAA, true);
    }
}
