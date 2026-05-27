package com.github.nalamodikk.client.renderer.armor;

import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ManaShieldEffectManager {

    public static final int DURATION_TICKS = 15;

    private static final Map<Integer, Long> ACTIVE = new HashMap<>();

    public static void addEffect(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        ACTIVE.put(entityId, gameTime);
    }

    public static void prune(long currentGameTime) {
        ACTIVE.entrySet().removeIf(e -> currentGameTime - e.getValue() > DURATION_TICKS + 2);
    }

    public static void clear() { ACTIVE.clear(); }

    public static Map<Integer, Long> getActive() {
        return Collections.unmodifiableMap(ACTIVE);
    }
}
