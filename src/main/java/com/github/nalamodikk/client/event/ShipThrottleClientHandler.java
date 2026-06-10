package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.ship.ShipThrottlePacket;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * 駕駛飛船時用滾輪調油門（取代切換快捷欄）。每格 ±10%。本地先設(即時 HUD)再送 server 權威同步。
 * 只有坐在駕駛位的玩家有效；非駕駛/沒駕駛飛船時滾輪維持原本切快捷欄。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipThrottleClientHandler {
    private ShipThrottleClientHandler() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        int seat = ship.seatIndexOf(mc.player);
        if (seat < 0 || seat >= ShipEntity.MAX_DRIVERS) return; // 只有駕駛能調油門
        double dy = event.getScrollDeltaY();
        if (dy == 0) return;
        float t = Mth.clamp(ship.getThrottle() + (dy > 0 ? 0.1f : -0.1f), 0f, 1f);
        ship.setThrottle(t);                  // 本地即時設(HUD 馬上反應)，server 會權威同步回來
        ShipThrottlePacket.sendToServer(ship.getId(), t);
        event.setCanceled(true);              // 駕駛中滾輪 = 調油門，不切快捷欄
    }
}
