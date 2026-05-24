package com.github.nalamodikk.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientInteractiveFormationManager {

    public static final int STAGE_OFF        = 0;
    public static final int STAGE_SINGLE     = 1;
    public static final int STAGE_RING       = 2;
    public static final int STAGE_RING_SEQ   = 3;
    public static final int STAGE_DOUBLE_RING = 4;

    private static final int   RING_N      = 80;  // instant ring — dense enough to look connected
    private static final int   RING_N_SEQ  = 80;  // sequential ring — same density as instant ring, one dot per tick
    private static final int   RING_N_OUTER = 128; // outer ring in double-ring stage
    private static final float RING_R  = 0.65f;
    private static final float RING_R2 = 1.15f;

    private static final Map<UUID, FormationState> states = new HashMap<>();

    public static void setStage(UUID uuid, int stage) {
        if (stage == STAGE_OFF) {
            FormationState removed = states.remove(uuid);
            if (removed != null) removed.generation++;
            return;
        }

        FormationState state = states.computeIfAbsent(uuid, k -> new FormationState());
        state.stage = stage;
        state.generation++;
        state.clientTick = 0;
        state.revealCount = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Player target = mc.level.getPlayerByUUID(uuid);
        if (target == null) return;

        Basis b = Basis.fromPlayer(target);

        switch (stage) {
            case STAGE_SINGLE     -> spawnPoint(mc.level, uuid, state.generation, b, 0, 0);
            case STAGE_RING       -> spawnFullRing(mc.level, uuid, state.generation, b, RING_N, RING_R, 0);
            case STAGE_DOUBLE_RING -> {
                spawnFullRing(mc.level, uuid, state.generation, b, RING_N, RING_R,  0);
                spawnFullRing(mc.level, uuid, state.generation, b, RING_N_OUTER, RING_R2, Math.PI / 8.0);
            }
            // STAGE_RING_SEQ: reveal one-by-one in onClientTick
        }
    }

    public static int getGeneration(UUID uuid) {
        FormationState s = states.get(uuid);
        return s == null ? -1 : s.generation;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (states.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Iterator<Map.Entry<UUID, FormationState>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, FormationState> entry = it.next();
            FormationState state = entry.getValue();
            if (state.stage != STAGE_RING_SEQ) continue;

            UUID uuid = entry.getKey();
            Player target = mc.level.getPlayerByUUID(uuid);
            if (target == null) continue;

            if (state.revealCount < RING_N_SEQ) {
                Basis b = Basis.fromPlayer(target);
                spawnRingParticle(mc.level, uuid, state.generation, b, state.revealCount, RING_N_SEQ, RING_R, 0);
                state.revealCount++;
            }
            state.clientTick++;
        }
    }

    // --- internal helpers ---

    private static FormationParticleOptions makeOpts(UUID uuid, int generation, double initialAngle, float radius) {
        return new FormationParticleOptions(
                Double.longBitsToDouble(uuid.getMostSignificantBits()),
                Double.longBitsToDouble(uuid.getLeastSignificantBits()),
                initialAngle,
                FormationParticleOptions.TRACK_PLAYER,
                radius,
                0f
        );
    }

    private static void spawnPoint(ClientLevel level, UUID uuid, int generation, Basis b, double angle, float radius) {
        double ax = b.cx + radius * (Math.cos(angle) * b.rx + Math.sin(angle) * b.ux);
        double ay = b.cy + radius * (Math.cos(angle) * b.ry + Math.sin(angle) * b.uy);
        double az = b.cz + radius * (Math.cos(angle) * b.rz + Math.sin(angle) * b.uz);
        level.addParticle(makeOpts(uuid, generation, angle, radius), ax, ay, az, 0, 0, 0);
    }

    private static void spawnFullRing(ClientLevel level, UUID uuid, int generation,
                                       Basis b, int n, float radius, double angleOff) {
        for (int i = 0; i < n; i++) {
            double angle = angleOff + i * Math.PI * 2.0 / n;
            spawnPoint(level, uuid, generation, b, angle, radius);
        }
    }

    private static void spawnRingParticle(ClientLevel level, UUID uuid, int generation,
                                           Basis b, int idx, int total, float radius, double angleOff) {
        double angle = angleOff + idx * Math.PI * 2.0 / total;
        spawnPoint(level, uuid, generation, b, angle, radius);
    }

    // --- inner types ---

    private static class FormationState {
        int stage;
        int generation;
        int clientTick;
        int revealCount;
    }

    static class Basis {
        final double cx, cy, cz;
        final double rx, ry, rz; // player's right in look-plane
        final double ux, uy, uz; // look × right = "up" in look-plane

        private Basis(double cx, double cy, double cz,
                      double rx, double ry, double rz,
                      double ux, double uy, double uz) {
            this.cx = cx; this.cy = cy; this.cz = cz;
            this.rx = rx; this.ry = ry; this.rz = rz;
            this.ux = ux; this.uy = uy; this.uz = uz;
        }

        static Basis fromPlayer(Player player) {
            double yaw   = Math.toRadians(player.getYRot());
            double pitch = Math.toRadians(player.getXRot());
            double lx = -Math.sin(yaw) * Math.cos(pitch);
            double ly = -Math.sin(pitch);
            double lz =  Math.cos(yaw) * Math.cos(pitch);

            double dist = 2.5;
            double cx = player.getX()    + lx * dist;
            double cy = player.getEyeY() + ly * dist;
            double cz = player.getZ()    + lz * dist;

            double rx, ry, rz;
            if (Math.abs(ly) > 0.99) {
                rx = 1; ry = 0; rz = 0;
            } else {
                rx = lz; ry = 0; rz = -lx;
                double len = Math.sqrt(rx * rx + rz * rz);
                rx /= len; rz /= len;
            }

            // up = look × right (gives world-up when looking horizontally)
            double ux = ly * rz - lz * ry;
            double uy = lz * rx - lx * rz;
            double uz = lx * ry - ly * rx;

            return new Basis(cx, cy, cz, rx, ry, rz, ux, uy, uz);
        }
    }
}
