package com.github.nalamodikk.common.particle;

import com.github.nalamodikk.client.particle.FormationParticleOptions;
import com.github.nalamodikk.register.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class FormationEffectManager {

    private static final Map<UUID, ActiveFormation> activeFormations = new HashMap<>();

    // Ring definitions: radius, angular speed (rad/tick), particles per ring
    private static final float[] RADII  = {0.60f, 1.20f, 1.90f};
    private static final float[] SPEEDS = {0.07f, -0.05f, 0.035f};
    private static final int[]   COUNTS = {6, 8, 12};

    public static boolean toggle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (activeFormations.containsKey(uuid)) {
            activeFormations.remove(uuid);
            return false;
        } else {
            activeFormations.put(uuid, new ActiveFormation());
            return true;
        }
    }

    public static boolean isActive(UUID uuid) {
        return activeFormations.containsKey(uuid);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, ActiveFormation>> it = activeFormations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveFormation> entry = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }
            entry.getValue().tick(player);
        }
    }

    private static class ActiveFormation {
        private int tick = 0;
        private final double[] ringAngles = new double[RADII.length];

        void tick(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            double cx = player.getX();
            double cy = player.getY() + 0.05;
            double cz = player.getZ();

            for (int ri = 0; ri < RADII.length; ri++) {
                float r = RADII[ri];
                float spd = SPEEDS[ri];
                int n = COUNTS[ri];
                ringAngles[ri] += spd;
                double base = ringAngles[ri];

                for (int i = 0; i < n; i++) {
                    double angle = base + i * Math.PI * 2.0 / n;
                    double px = cx + r * Math.cos(angle);
                    double pz = cz + r * Math.sin(angle);
                    level.sendParticles(
                            new FormationParticleOptions(cx, cy, cz, FormationParticleOptions.POINT, r, spd),
                            px, cy + 0.06, pz,
                            1, 0, 0, 0, 0
                    );
                }
            }

            // 每 4 tick 噴一批 RISE 粒子
            if (tick % 4 == 0) {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2.0;
                    double r = 0.3 + Math.random() * 1.7;
                    level.sendParticles(
                            new FormationParticleOptions(cx, cy, cz, FormationParticleOptions.RISE, 0f, 0f),
                            cx + r * Math.cos(angle), cy, cz + r * Math.sin(angle),
                            1, 0, 0, 0, 0
                    );
                }
            }

            tick++;
        }
    }
}
