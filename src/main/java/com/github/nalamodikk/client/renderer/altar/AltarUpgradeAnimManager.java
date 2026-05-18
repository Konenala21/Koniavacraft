package com.github.nalamodikk.client.renderer.altar;

import com.github.nalamodikk.client.renderer.OrbitalTestShaderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AltarUpgradeAnimManager {

    public record AnimState(int tier, float tick, float totalTicks) {
        public float progress() { return totalTicks > 0 ? tick / totalTicks : 1f; }
        public boolean isDone() { return tick >= totalTicks; }
        AnimState advance() { return new AnimState(tier, tick + 1f, totalTicks); }
    }

    private static final Map<BlockPos, AnimState> ACTIVE = new HashMap<>();

    // 每個 tier 的動畫總長度（ticks）
    // T1-T3：6 秒；T4-T5：30 秒；T6：60 秒
    public static float getDuration(int tier) {
        return switch (tier) {
            case 1, 2, 3 -> 120f;
            case 4, 5    -> 600f;
            case 6       -> 1200f;
            default      -> 120f;
        };
    }

    public static void startAnimation(BlockPos pos, int tier) {
        ACTIVE.put(pos, new AnimState(tier, 0f, getDuration(tier)));
    }

    public static boolean isAnimating(BlockPos pos) {
        AnimState s = ACTIVE.get(pos);
        return s != null && !s.isDone();
    }

    public static AnimState getState(BlockPos pos) {
        return ACTIVE.get(pos);
    }

    public static AnimState getActiveT6State() {
        for (AnimState s : ACTIVE.values()) {
            if (s.tier() == 6 && !s.isDone()) return s;
        }
        return null;
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<BlockPos, AnimState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, AnimState> entry = it.next();
            AnimState current = entry.getValue();
            if (mc.level != null) {
                BlockPos p = entry.getKey();
                float t = current.tick();

                // T4-T5：tick 150 觸發球體 orbital shader
                if (current.tier() >= 4 && current.tier() <= 5) {
                    if (t < 150f && t + 1f >= 150f) {
                        OrbitalTestShaderRenderer.currentMode = OrbitalTestShaderRenderer.Mode.ORBITAL_SPHERE_SATS;
                        OrbitalTestShaderRenderer.spawnEffect(
                                Vec3.atCenterOf(p), mc.level.getGameTime());
                    }
                }

                // 叮 音效觸發
                float soundTick = getSoundTick(current.tier());
                if (t < soundTick && t + 1f >= soundTick) {
                    mc.level.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                }
            }
            AnimState next = current.advance();
            if (next.isDone()) it.remove();
            else entry.setValue(next);
        }
    }

    private static float getSoundTick(int tier) {
        return switch (tier) {
            case 1, 2, 3 -> 110f;
            case 4, 5    -> 580f;
            case 6       -> 1180f;
            default      -> 110f;
        };
    }

    public static void clear() { ACTIVE.clear(); }

    // 回傳目前畫面應有的黑幕 alpha（0.0 = 無遮蔽，1.0 = 全黑）
    // 只看 tier >= 4 的動畫
    public static float getScreenFadeAlpha() {
        for (AnimState s : ACTIVE.values()) {
            if (s.tier() < 4) continue;
            float t = s.tick();
            // 60-140t：漸黑（0→1）
            if (t >= 60f && t < 140f) return (t - 60f) / 80f;
            // 140-200t：維持全黑
            if (t >= 140f && t < 200f) return 1.0f;
            // 200-270t：漸亮（1→0）
            if (t >= 200f && t < 270f) return 1.0f - (t - 200f) / 70f;
        }
        return 0f;
    }
}
