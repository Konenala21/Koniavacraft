package com.github.nalamodikk.client;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class ManaFogTracker {

    @Nullable
    private static BlockPos nearestFog = null;

    public static void setNearestFog(@Nullable BlockPos pos) {
        nearestFog = pos;
    }

    @Nullable
    public static BlockPos getNearestFog() {
        return nearestFog;
    }

    public static void clear() {
        nearestFog = null;
    }
}
