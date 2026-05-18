package com.github.nalamodikk.client.renderer.altar;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class AltarCameraController {

    // Pitch lock window: 160-1050t
    private static final float LOCK_START  = 160f;
    private static final float BLEND_END   = 280f;
    private static final float HOLD_END    = 850f;
    private static final float RELEASE_END = 1050f;

    // Shake timeline
    private static final float SHAKE_RISE_START = 60f;
    private static final float SHAKE_RAMP_END   = 200f;
    private static final float SHAKE_PEAK_END   = 400f;
    private static final float SHAKE_HOLD_END   = 700f;
    private static final float SHAKE_DROP_END   = 950f;
    private static final float SHAKE_FADE_END   = 1100f;

    private static float capturedYaw   = 0f;
    private static boolean wasT6Active = false;

    // Used by MouseHandlerT6Mixin to decide whether to suppress input
    public static boolean isCameraLocked() {
        AltarUpgradeAnimManager.AnimState s = AltarUpgradeAnimManager.getActiveT6State();
        if (s == null) return false;
        // Camera effects only apply in the T6 climax phase (after T4-T5 prefix)
        float tick = s.tick() - AltarUpgradeAnimManager.T6_PHASE_OFFSET;
        return tick >= LOCK_START && tick < RELEASE_END;
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        AltarUpgradeAnimManager.AnimState state = AltarUpgradeAnimManager.getActiveT6State();
        boolean isActive = state != null && !state.isDone();

        // Capture yaw when the T6 climax phase begins
        float rawTick = isActive ? state.tick() + (float) event.getPartialTick() : 0f;
        boolean inClimax = isActive && rawTick >= AltarUpgradeAnimManager.T6_PHASE_OFFSET;
        if (inClimax && !wasT6Active) {
            capturedYaw = event.getYaw();
        }
        wasT6Active = inClimax;

        if (!inClimax) return;

        // Shift tick so all curves start from 0 at the T6 climax begin
        float tick  = rawTick - AltarUpgradeAnimManager.T6_PHASE_OFFSET;
        float blend = computePitchBlend(tick);
        float shake = computeShakeAmp(tick);

        // Pitch: smoothly override toward -80 (looking at sky)
        float newPitch = event.getPitch() + (-80f - event.getPitch()) * blend;

        // Yaw: smoothly lock onto captured direction, preventing drift
        float newYaw = event.getYaw() + (capturedYaw - event.getYaw()) * blend;

        // Deterministic shake (incommensurate frequencies to avoid repetition)
        float sp = shake * (float)(Math.sin(tick * 0.83) * 0.50 + Math.cos(tick * 1.37) * 0.30 + Math.sin(tick * 2.11) * 0.20);
        float sy = shake * (float)(Math.cos(tick * 0.91) * 0.40 + Math.sin(tick * 1.73) * 0.40 + Math.cos(tick * 2.31) * 0.20);

        event.setPitch(newPitch + sp);
        event.setYaw(newYaw + sy);
    }

    // Blend factor 0-1 for pitch/yaw override
    private static float computePitchBlend(float tick) {
        if (tick < LOCK_START)   return 0f;
        if (tick < BLEND_END)    return smoothstep((tick - LOCK_START) / (BLEND_END - LOCK_START));
        if (tick < HOLD_END)     return 1f;
        if (tick < RELEASE_END)  return 1f - smoothstep((tick - HOLD_END) / (RELEASE_END - HOLD_END));
        return 0f;
    }

    // Shake amplitude in degrees
    private static float computeShakeAmp(float tick) {
        if (tick < SHAKE_RISE_START)  return 0f;
        if (tick < SHAKE_RAMP_END)    return lerp(0f, 1f,  (tick - SHAKE_RISE_START) / (SHAKE_RAMP_END - SHAKE_RISE_START));
        if (tick < SHAKE_PEAK_END)    return lerp(1f, 4f,  (tick - SHAKE_RAMP_END)   / (SHAKE_PEAK_END - SHAKE_RAMP_END));
        if (tick < SHAKE_HOLD_END)    return 4f;
        if (tick < SHAKE_DROP_END)    return lerp(4f, 0.5f,(tick - SHAKE_HOLD_END)   / (SHAKE_DROP_END - SHAKE_HOLD_END));
        if (tick < SHAKE_FADE_END)    return lerp(0.5f, 0f,(tick - SHAKE_DROP_END)   / (SHAKE_FADE_END - SHAKE_DROP_END));
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
