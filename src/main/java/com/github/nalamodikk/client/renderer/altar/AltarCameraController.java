package com.github.nalamodikk.client.renderer.altar;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class AltarCameraController {

    // Pitch lock: ramps in 160-280t, holds to 870t, follows descent 870-980t, releases 980-1020t
    private static final float LOCK_START  = 160f;
    private static final float BLEND_END   = 280f;
    private static final float HOLD_END    = 870f;
    private static final float DESCENT_END = 980f;
    private static final float RELEASE_END = 1020f;

    // Shake timeline: fully gone by 850t so camera is stable during circle descent
    private static final float SHAKE_RISE_START = 60f;
    private static final float SHAKE_RAMP_END   = 200f;
    private static final float SHAKE_PEAK_END   = 400f;
    private static final float SHAKE_HOLD_END   = 580f;
    private static final float SHAKE_DROP_END   = 770f;
    private static final float SHAKE_FADE_END   = 850f;

    // Set to true while actively controlling pitch; cleared after stamping xRot at release end
    private static boolean pendingStamp = false;

    public static void reset() {
        pendingStamp = false;
    }

    // Used by MouseHandlerT6Mixin to suppress mouse input during the climax
    public static boolean isCameraLocked() {
        AltarUpgradeAnimManager.AnimState s = AltarUpgradeAnimManager.getActiveT6State();
        if (s == null) return false;
        float tick = s.tick() - AltarUpgradeAnimManager.T6_PHASE_OFFSET;
        return tick >= LOCK_START && tick < RELEASE_END;
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        AltarUpgradeAnimManager.AnimState state = AltarUpgradeAnimManager.getActiveT6State();
        if (state == null || state.isDone()) return;

        float rawTick = state.tick() + (float) event.getPartialTick();
        if (rawTick < AltarUpgradeAnimManager.T6_PHASE_OFFSET) return;

        float tick = rawTick - AltarUpgradeAnimManager.T6_PHASE_OFFSET;
        if (tick >= RELEASE_END) {
            if (pendingStamp) {
                // Stamp pitch to 0° (horizon) so camera stays there instead of snapping back
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) mc.player.setXRot(0f);
                pendingStamp = false;
            }
            return;
        }

        pendingStamp = true;

        float shake = computeShakeAmp(tick);

        // Pitch only — no yaw override, player looks freely sideways
        float newPitch;
        if (tick >= DESCENT_END) {
            float t = (tick - DESCENT_END) / (RELEASE_END - DESCENT_END);
            newPitch = lerp(-10f, 0f, t);
        } else {
            float blend = computePitchBlend(tick);
            newPitch = event.getPitch() + (computeTargetPitch(tick) - event.getPitch()) * blend;
        }

        float sp = shake * (float)(Math.sin(tick * 0.83) * 0.50 + Math.cos(tick * 1.37) * 0.30 + Math.sin(tick * 2.11) * 0.20);
        event.setPitch(newPitch + sp);
    }

    private static float computePitchBlend(float tick) {
        if (tick < LOCK_START) return 0f;
        if (tick < BLEND_END)  return smoothstep((tick - LOCK_START) / (BLEND_END - LOCK_START));
        return 1f;
    }

    // Tracks circle from sky (-80°) down to near-horizon (-10°) during descent
    private static float computeTargetPitch(float tick) {
        if (tick < HOLD_END) return -80f;
        if (tick < DESCENT_END) {
            float t = (tick - HOLD_END) / (DESCENT_END - HOLD_END);
            return lerp(-80f, -10f, t * t);
        }
        return -10f;
    }

    private static float computeShakeAmp(float tick) {
        if (tick < SHAKE_RISE_START) return 0f;
        if (tick < SHAKE_RAMP_END)   return lerp(0f,   1f,   (tick - SHAKE_RISE_START) / (SHAKE_RAMP_END - SHAKE_RISE_START));
        if (tick < SHAKE_PEAK_END)   return lerp(1f,   4f,   (tick - SHAKE_RAMP_END)   / (SHAKE_PEAK_END - SHAKE_RAMP_END));
        if (tick < SHAKE_HOLD_END)   return 4f;
        if (tick < SHAKE_DROP_END)   return lerp(4f,   0.5f, (tick - SHAKE_HOLD_END)   / (SHAKE_DROP_END - SHAKE_HOLD_END));
        if (tick < SHAKE_FADE_END)   return lerp(0.5f, 0f,   (tick - SHAKE_DROP_END)   / (SHAKE_FADE_END - SHAKE_DROP_END));
        return 0f;
    }

    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }
}
