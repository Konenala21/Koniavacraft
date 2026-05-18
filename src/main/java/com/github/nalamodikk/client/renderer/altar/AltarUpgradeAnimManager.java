package com.github.nalamodikk.client.renderer.altar;

import net.minecraft.core.BlockPos;
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

    public static void clientTick() {
        Iterator<Map.Entry<BlockPos, AnimState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, AnimState> entry = it.next();
            AnimState next = entry.getValue().advance();
            if (next.isDone()) it.remove();
            else entry.setValue(next);
        }
    }

    public static void clear() { ACTIVE.clear(); }
}
