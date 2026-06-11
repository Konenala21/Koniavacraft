package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 駕駛飛船時的 HUD：螢幕左側兩條「直立」bar — 油門 + 燃料，bar 由下往上填，中文標籤直排（每字一列），
 * 數字顯示在 bar 下方。只有坐在駕駛位的玩家會看到。油門用滾輪/控制台調，燃料由 server 同步(DATA_FUEL)。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipHudOverlay {
    private ShipHudOverlay() {}

    private static final int BAR_W = 9;
    private static final int BAR_H = 90;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        int seat = ship.seatIndexOf(mc.player);
        if (seat < 0 || seat >= ShipEntity.MAX_DRIVERS) return; // 只有駕駛看到

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int top = mc.getWindow().getGuiScaledHeight() / 2 - BAR_H / 2;
        int x = 16; // 離左邊緣

        int fuel = ship.getDisplayFuel(), maxFuel = ship.getMaxFuel();
        float fuelFrac = maxFuel > 0 ? (float) fuel / maxFuel : 0f;
        int throttlePct = Math.round(ship.getThrottle() * 100f);
        int fuelPct = Math.round(fuelFrac * 100f);

        vbar(g, font, x, top, ship.getThrottle(), 0xFF55DD66,
                Component.translatable("hud.koniava.ship.throttle").getString(), throttlePct + "%");
        vbar(g, font, x + BAR_W + 18, top, fuelFrac, fuel <= 0 ? 0xFFFF5555 : 0xFF55AAFF,
                Component.translatable("hud.koniava.ship.fuel").getString(), fuelPct + "%");

        // 第三條:曲速能量(只有船上有完整曲速結構才畫,紫色)
        if (ship.getWarpDriveCount() > 0) {
            int warp = ship.getDisplayWarpFuel(), maxWarp = ship.getMaxWarpFuel();
            float warpFrac = maxWarp > 0 ? (float) warp / maxWarp : 0f;
            vbar(g, font, x + 2 * (BAR_W + 18), top, warpFrac, warp <= 0 ? 0xFFFF5555 : 0xFFB066FF,
                    Component.translatable("hud.koniava.ship.warp").getString(), Math.round(warpFrac * 100f) + "%");
        }

        // 引擎數(直立 bar 下方,橫排小字)
        g.drawString(font, Component.translatable("hud.koniava.ship.engines", ship.getEngineCount()),
                x, top + BAR_H + 14, 0xFFCCCCCC, true);
    }

    /** 一條直立 bar：下往上填 + 上方直排中文標籤 + 下方數字。 */
    private static void vbar(GuiGraphics g, Font font, int x, int top, float frac, int color, String label, String num) {
        g.fill(x - 1, top - 1, x + BAR_W + 1, top + BAR_H + 1, 0xFF000000);
        g.fill(x, top, x + BAR_W, top + BAR_H, 0xFF2A2A2E);
        int fh = (int) (BAR_H * Mth.clamp(frac, 0f, 1f));
        g.fill(x, top + BAR_H - fh, x + BAR_W, top + BAR_H, color);
        // 直排標籤（每字一列）放 bar 上方，底部固定在 bar 上緣 4px 處，任意長度(中/英)都不會壓到 bar
        int labelTop = top - 4 - label.length() * 9;
        for (int i = 0; i < label.length(); i++) {
            String ch = String.valueOf(label.charAt(i));
            g.drawString(font, ch, x + (BAR_W - font.width(ch)) / 2, labelTop + i * 9, 0xFFDDDDDD, true);
        }
        // 數字置中在 bar 下方
        g.drawString(font, num, x + (BAR_W - font.width(num)) / 2, top + BAR_H + 3, 0xFFFFFFFF, true);
    }
}
