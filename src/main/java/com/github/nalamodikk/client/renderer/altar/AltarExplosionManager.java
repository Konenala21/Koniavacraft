package com.github.nalamodikk.client.renderer.altar;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarExplosionManager {

    public static final float DURATION = 80f; // 4 seconds

    private static final Map<BlockPos, Float> ACTIVE = new HashMap<>();

    public static void start(BlockPos pos) {
        ACTIVE.put(pos, 0f);
    }

    public static List<Map.Entry<BlockPos, Float>> getActive() {
        List<Map.Entry<BlockPos, Float>> list = new ArrayList<>();
        for (Map.Entry<BlockPos, Float> e : ACTIVE.entrySet()) {
            if (e.getValue() < DURATION) list.add(e);
        }
        return list;
    }

    public static void clientTick() {
        ACTIVE.entrySet().removeIf(e -> {
            float next = e.getValue() + 1f;
            if (next >= DURATION) return true;
            e.setValue(next);
            return false;
        });
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
