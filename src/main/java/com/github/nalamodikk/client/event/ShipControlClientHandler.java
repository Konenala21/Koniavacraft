package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        if (ship.getControllingPassenger() != mc.player) return; // 只有駕駛算移動

        // client 權威：駕駛 client 直接把輸入設到本地船，tick 在這端算移動，vanilla
        // 再用 MoveVehiclePacket 把位置同步給 server。不用自訂封包。
        Options o = mc.options;
        float forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        float strafe = (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
        int vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keySprint.isDown() ? 1 : 0);
        ship.setControlInput(forward, strafe, vertical, mc.player.getYRot());
    }
}
