package com.github.nalamodikk.client.renderer.altar;

import com.github.nalamodikk.client.renderer.OrbitalTestShaderRenderer;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
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
    // T1-T3：6 秒；T4-T5：30 秒；T6：90 秒（前 600t 重播 T4-T5，後 1200t 為 T6 高潮）
    public static float getDuration(int tier) {
        return switch (tier) {
            case 1, 2, 3 -> 120f;
            case 4, 5    -> 600f;
            case 6       -> 1800f;
            default      -> 120f;
        };
    }

    // T6 動畫中 T4-T5 階段結束、T6 高潮開始的偏移量
    public static final float T6_PHASE_OFFSET = 600f;

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

    // Returns active T1-T3 entries for the shockwave shader renderer
    public static java.util.List<java.util.Map.Entry<BlockPos, AnimState>> getActiveLowTierEntries() {
        java.util.List<java.util.Map.Entry<BlockPos, AnimState>> list = new java.util.ArrayList<>();
        for (java.util.Map.Entry<BlockPos, AnimState> e : ACTIVE.entrySet()) {
            if (e.getValue().tier() <= 3 && !e.getValue().isDone()) list.add(e);
        }
        return list;
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

                // T4-T5 及 T6 前 600t（T4-T5 階段）：tick 150 觸發球體 orbital shader
                if (current.tier() >= 4 && (current.tier() <= 5 || t < T6_PHASE_OFFSET)) {
                    if (t < 150f && t + 1f >= 150f) {
                        OrbitalTestShaderRenderer.currentMode = OrbitalTestShaderRenderer.Mode.ORBITAL_SPHERE_SATS;
                        OrbitalTestShaderRenderer.spawnEffect(
                                Vec3.atCenterOf(p), mc.level.getGameTime());
                    }
                }

                // T6：T4-T5 階段結束音效（tick 580）+ 高潮結束音效（tick 1780）
                // T6：娜拉對話觸發（tick 1750）
                if (current.tier() == 6) {
                    if (t < 580f && t + 1f >= 580f) {
                        mc.level.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                    }
                    if (t < 1750f && t + 1f >= 1750f) {
                        NaraTutorialFlow.start(NaraTutorialFlow.ALTAR_T6);
                    }
                }

                // 主音效觸發（T1-T5 用，T6 改為上方獨立處理）
                if (current.tier() < 6) {
                    float soundTick = getSoundTick(current.tier());
                    if (t < soundTick && t + 1f >= soundTick) {
                        mc.level.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                    }
                } else {
                    // T6 最終音效
                    if (t < 1780f && t + 1f >= 1780f) {
                        mc.level.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                    }
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
    public static float getScreenFadeAlpha() {
        for (AnimState s : ACTIVE.values()) {
            if (s.tier() < 4) continue;
            float t = s.tick();
            if (s.tier() == 6) {
                // T4-T5 階段（0-600t）：與 T4-T5 相同的黑幕窗口
                if (t >= 60f  && t < 140f) return (t - 60f) / 80f;
                if (t >= 140f && t < 200f) return 1.0f;
                if (t >= 200f && t < 270f) return 1.0f - (t - 200f) / 70f;
                // T6 高潮階段（相對於 T6_PHASE_OFFSET）
                float t6 = t - T6_PHASE_OFFSET;
                if (t6 >= 120f && t6 < 160f) return (t6 - 120f) / 40f;
                if (t6 >= 160f && t6 < 280f) return 1.0f;
                if (t6 >= 280f && t6 < 360f) return 1.0f - (t6 - 280f) / 80f;
            } else {
                // T4-T5：60-270t
                if (t >= 60f  && t < 140f) return (t - 60f) / 80f;
                if (t >= 140f && t < 200f) return 1.0f;
                if (t >= 200f && t < 270f) return 1.0f - (t - 200f) / 70f;
            }
        }
        return 0f;
    }
}
