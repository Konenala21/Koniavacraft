package com.github.nalamodikk.client.renderer.turret;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurretHitEffectManager {

    public static final int DURATION_TICKS = 20;

    public record HitEffect(Vec3 pos, float chargeRatio, long spawnTick) {}

    private static final List<HitEffect> ACTIVE = new ArrayList<>();

    public static void addEffect(Vec3 pos, float chargeRatio) {
        // gameTime is not directly available here; the renderer will pass current time at spawn
        // We store client's System.nanoTime converted to a stable game-tick-based value instead.
        // Actually: we grab level gameTime on the render thread via Minecraft.getInstance().level
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        ACTIVE.add(new HitEffect(pos, chargeRatio, gameTime));
    }

    public static void prune(long currentGameTime) {
        ACTIVE.removeIf(e -> currentGameTime - e.spawnTick() > DURATION_TICKS + 2);
    }

    public static List<HitEffect> getActive() {
        return Collections.unmodifiableList(ACTIVE);
    }
}
