package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 駕駛會側翻的飛船時，把滑鼠轉向 delta 按「鏡頭 roll」旋轉成世界 yaw/pitch，讓操作跟翻轉後的畫面對齊：
 * 顛倒(roll 180°) → 左右/上下都反；roll 90° → 左右變上下。否則輸入是世界座標、畫面被 roll 翻了，
 * 顛倒時左右會感覺反。攔 turnPlayer 裡的 player.turn() 呼叫；非駕駛飛船 / roll=0 時原樣放行。
 *
 * 鏡頭 roll = +ship.getRoll()(見 ShipRiderViewHandler 的 ComputeCameraAngles)。若實機發現中間角度
 * (如 90°)左右反了，把下面 sin 的正負對調即可(180° 不受 sin 影響，必對)。
 */
@Mixin(MouseHandler.class)
public class MouseHandlerShipRollMixin {

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void koniava$rollAwareTurn(LocalPlayer player, double yaw, double pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.player.getVehicle() instanceof ShipEntity ship
                && ship.getControllingPassenger() == mc.player) {
            float roll = ship.getRoll();
            if (roll != 0f) {
                double r = Math.toRadians(roll);
                double cos = Math.cos(r), sin = Math.sin(r);
                player.turn(yaw * cos - pitch * sin, yaw * sin + pitch * cos);
                return;
            }
        }
        player.turn(yaw, pitch);
    }
}
