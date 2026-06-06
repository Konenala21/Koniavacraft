package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.event.MoonCoreTraversalHandler;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 月球核心過渡時的相機翻轉：穿過核心往另一面「掉」時，相機平滑 roll 180°，
 * 製造「重力翻轉穿過地心」的體感；冒出地表（過渡結束）轉回 0°。
 *
 * 單人遊戲 client/server 共用 MoonCoreTraversalHandler 的靜態狀態，故 client
 * 能直接讀本地玩家是否在過渡中。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class MoonCameraController {

    private static float currentRoll = 0f; // 目前相機 roll（度）
    private static final float LERP = 0.10f; // 平滑速度（約 1 秒到位）

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.level.dimension().equals(ModDimensions.MOON)) {
            currentRoll = 0f;
            return;
        }

        float target = MoonCoreTraversalHandler.isInTransit(mc.player.getUUID()) ? 180f : 0f;
        currentRoll += (target - currentRoll) * LERP;
        if (Math.abs(currentRoll) < 0.05f) currentRoll = 0f;

        if (currentRoll != 0f) {
            event.setRoll(event.getRoll() + currentRoll);
        }
    }
}
