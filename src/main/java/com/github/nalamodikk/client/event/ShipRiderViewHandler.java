package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 駕駛/乘坐飛船時，讓「鏡頭」與「玩家模型」跟著船的 pitch/roll 傾斜，否則 A/D 翻滾只有船轉、人沒轉，看起來分家。
 * <ul>
 *   <li>鏡頭：{@link ViewportEvent.ComputeCameraAngles} 設 roll(第一/第三人稱都套)，畫面跟著船側翻。</li>
 *   <li>玩家模型：{@link RenderPlayerEvent} 在玩家的朝向框內套 pitch(繞右軸)+roll(繞前軸)，第三人稱看得到人跟著傾。</li>
 * </ul>
 * sign/量實機看了再調。只在駕駛位/乘坐 ShipEntity 時生效。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipRiderViewHandler {

    private ShipRiderViewHandler() {}

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        // 船的 roll 直接當畫面 roll；pitch 不套到鏡頭(會跟滑鼠視角打架)，只讓船頭俯仰由視角自己帶。
        event.setRoll((float) (event.getRoll() + ship.getRoll()));
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Entity v = event.getEntity().getVehicle();
        if (!(v instanceof ShipEntity ship)) return;
        float roll = ship.getRoll();
        float pitch = ship.getXRot();
        if (roll == 0f && pitch == 0f) return;
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), event.getEntity().yBodyRotO, event.getEntity().yBodyRot);
        float h = 0.3f; // 坐姿傾斜支點放座位(屁股)附近，不是身體中心：繞中心轉 pitch 會把腳/屁股往前後甩→偏離座位
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // 在玩家朝向框內套傾斜(繞身體中心)：進朝向框 → pitch(右軸 X) + roll(前軸 Z) → 出框
        pose.translate(0, h, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        pose.translate(0, -h, 0);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Entity v = event.getEntity().getVehicle();
        if (!(v instanceof ShipEntity ship)) return;
        if (ship.getRoll() == 0f && ship.getXRot() == 0f) return;
        event.getPoseStack().popPose();
    }
}
