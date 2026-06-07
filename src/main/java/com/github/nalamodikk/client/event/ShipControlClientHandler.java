package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.sound.ShipFlySoundInstance;
import com.github.nalamodikk.common.network.packet.server.ship.ShipInputPacket;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 駕駛飛船時每 client tick 讀按鍵狀態送 ShipControlPacket。
 * WASD = 前後左右（相對視角），跳躍 = 上升，疾跑 = 下降。Shift 仍是 vanilla 下船。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ShipControlClientHandler {

    private static SoundInstance flySound; // 飛行循環音（駕駛時）

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        int seat = ship.seatIndexOf(mc.player);
        if (seat < 0 || seat >= ShipEntity.MAX_DRIVERS) return; // 只有坐駕駛位（前 N 個座位）的人送輸入

        // 駕駛中確保飛行音在播（音效自己 tick，下船會自動 stop；停了就重建）
        if (flySound == null || mc.getSoundManager().isActive(flySound) == false) {
            flySound = new ShipFlySoundInstance(ship, mc.player);
            mc.getSoundManager().play(flySound);
        }

        // server 權威：駕駛只送輸入，server 算移動、廣播位置，client 用 lerp 平滑跟隨（不自己預測）。
        Options o = mc.options;
        float forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        float strafe = (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
        int vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keySprint.isDown() ? 1 : 0);
        ShipInputPacket.sendToServer(forward, strafe, vertical, mc.player.getYRot());
    }
}
