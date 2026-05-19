package com.github.nalamodikk.client.renderer.altar;

import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class AltarUpgradeAnimManager {

    public record AnimState(int tier, float tick, float totalTicks, boolean triggerDialogue) {
        public float progress() { return totalTicks > 0 ? tick / totalTicks : 1f; }
        public boolean isDone() { return tick >= totalTicks; }
        AnimState advance() { return new AnimState(tier, tick + 1f, totalTicks, triggerDialogue); }
    }

    private static final Map<BlockPos, AnimState> ACTIVE = new HashMap<>();

    // 每個 tier 的動畫總長度（ticks）
    // T1-T3：6 秒；T4-T5：30 秒；T6：90 秒（前 600t 重播 T4-T5，後 1200t 為 T6 高潮）
    public static float getDuration(int tier) {
        return switch (tier) {
            case 1, 2, 3 -> 120f;
            case 4, 5    -> 600f;
            case 6       -> 1700f;
            default      -> 120f;
        };
    }

    // T6 動畫中 T4-T5 階段結束、T6 高潮開始的偏移量
    public static final float T6_PHASE_OFFSET = 600f;

    public static void startAnimation(BlockPos pos, int tier, boolean triggerDialogue) {
        ACTIVE.put(pos, new AnimState(tier, 0f, getDuration(tier), triggerDialogue));
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

    private static List<Map.Entry<BlockPos, AnimState>> filterActive(Predicate<AnimState> pred) {
        List<Map.Entry<BlockPos, AnimState>> list = new ArrayList<>();
        for (Map.Entry<BlockPos, AnimState> e : ACTIVE.entrySet()) {
            AnimState s = e.getValue();
            if (!s.isDone() && pred.test(s)) list.add(e);
        }
        return list;
    }

    // Returns T1-T3 entries for the simple shockwave+pillar shader.
    public static List<Map.Entry<BlockPos, AnimState>> getActiveShockwaveEntries() {
        return filterActive(s -> s.tier() <= 3);
    }

    // Returns T4-T5 entries, plus T6 during its T4-T5 prefix phase (tick < T6_PHASE_OFFSET).
    public static List<Map.Entry<BlockPos, AnimState>> getActiveT45Entries() {
        return filterActive(s -> s.tier() == 4 || s.tier() == 5
                || (s.tier() == 6 && s.tick() < T6_PHASE_OFFSET));
    }

    // Returns T6 entries currently in the climax phase (tick >= T6_PHASE_OFFSET).
    public static List<Map.Entry<BlockPos, AnimState>> getActiveT6ClimaxEntries() {
        return filterActive(s -> s.tier() == 6 && s.tick() >= T6_PHASE_OFFSET);
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<BlockPos, AnimState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, AnimState> entry = it.next();
            AnimState current = entry.getValue();
            if (mc.level == null) continue;

            BlockPos p = entry.getKey();
            float t = current.tick();

            if (current.tier() < 6) {
                // T1-T5：升級完成音效
                float soundTick = getSoundTick(current.tier());
                if (t < soundTick && t + 1f >= soundTick) {
                    mc.level.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                }
            } else {
                // T6：圓陣消失後立刻叮（relative 1020t = absolute 1620t），娜拉緊接其後
                // 前奏（0-600t）不觸發任何音效，視覺重播而已
                if (t < 1620f && t + 1f >= 1620f) {
                    mc.level.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                }
                if (t < 1630f && t + 1f >= 1630f && current.triggerDialogue()) {
                    NaraTutorialFlow.start(NaraTutorialFlow.ALTAR_T6);
                }
            }

            AnimState next = current.advance();
            if (next.isDone()) it.remove();
            else entry.setValue(next);
        }
    }

    private static float getSoundTick(int tier) {
        return switch (tier) {
            case 4, 5 -> 580f;
            default   -> 110f;
        };
    }

    public static void clear() { ACTIVE.clear(); }

    // 回傳目前畫面應有的黑幕 alpha（0.0 = 無遮蔽，1.0 = 全黑）
    // FadeRenderer 用 EventPriority.HIGH 先渲染，效果 shader 在黑幕上疊加
    public static float getScreenFadeAlpha() {
        for (AnimState s : ACTIVE.values()) {
            if (s.tier() < 4) continue;
            float t = s.tick();
            if (s.tier() == 6) {
                if (t < T6_PHASE_OFFSET) {
                    // T6 prefix: same timing as standalone T4/T5, no fade-out before climax
                    float peak = Math.min(smoothstep(60f, 155f, t), 1f - smoothstep(205f, 320f, t)) * 0.94f;
                    float base = smoothstep(60f, 155f, t) * 0.9f;
                    return Math.max(peak, base);
                }
                // T6 climax: starts at 0.25 (matching prefix base), ramps to 0.94, fades at 700-870t
                float t6 = t - T6_PHASE_OFFSET;
                float base6 = (0.9f + 0.04f * smoothstep(0f, 22f, t6)) * (1f - smoothstep(700f, 870f, t6));
                if (base6 > 0.001f) return base6;
                continue;
            } else {
                float base = calcT45FadeAlpha(t);
                if (base > 0.001f) return base;
            }
        }
        return 0f;
    }

    private static float calcT45FadeAlpha(float t) {
        float peak = Math.min(smoothstep(60f, 155f, t), 1f - smoothstep(205f, 320f, t)) * 0.94f;
        float base = smoothstep(60f, 155f, t) * (1f - smoothstep(550f, 600f, t)) * 0.9f;
        return Math.max(peak, base);
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }
}
