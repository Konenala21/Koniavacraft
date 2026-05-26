package com.github.nalamodikk.client.renderer.turret;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurretHitEffectManager {

    public static final int DURATION_TICKS = 12;
    private static final int MAX_ACTIVE = 128;

    public record HitEffect(Vec3 pos, float chargeRatio, long spawnTick) {}

    private static final List<HitEffect> ACTIVE = new ArrayList<>();

    public static void addEffect(Vec3 pos, float chargeRatio) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        if (ACTIVE.size() >= MAX_ACTIVE) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(new HitEffect(pos, chargeRatio, gameTime));
    }

    public static void prune(long currentGameTime) {
        ACTIVE.removeIf(e -> currentGameTime - e.spawnTick() > DURATION_TICKS + 2);
    }

    public static void clear() { ACTIVE.clear(); }

    public static List<HitEffect> getActive() {
        return Collections.unmodifiableList(ACTIVE);
    }
}
