package com.github.nalamodikk.common.item;

import com.github.nalamodikk.client.particle.FormationParticleOptions;
import com.github.nalamodikk.common.particle.FormationEffectManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public class DevRenderTestItem3 extends Item {

    private static final Random RNG = new Random();

    public DevRenderTestItem3(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.literal("Right-click: toggle orbiting formation"));
        tips.add(Component.literal("Shift+Right-click: stamp formation on ground"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResultHolder.success(player.getItemInHand(hand));

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = serverPlayer.serverLevel();

        if (player.isShiftKeyDown()) {
            spawnFormation(serverLevel, serverPlayer);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 0.8f);
        } else {
            boolean activated = FormationEffectManager.toggle(serverPlayer);
            float pitch = activated ? 1.4f : 0.8f;
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    activated ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.5f, pitch);
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    // ── Formation geometry ────────────────────────────────────────────────────
    // Heights (Y offsets): outer rings flat on ground → inner layers rise upward
    //   +0.00  outer double ring
    //   +0.05  outer symbol ring
    //   +0.22  inner geometric ring
    //   +0.50  enneagram {9/4} (floating star)
    //   +0.58  inner symbol ring
    //   +1.20  top ring
    //   +1.30  center glow
    //
    // Image-sampled points: run scripts/formation_sampler.py [img.png] and paste output here.
    // u = player-right, v = player-forward, coordinates in script units (scale=1.5 by default).
    // spawnFromPoints() multiplies by POINTS_SCALE to convert to world blocks.
    private static final float POINTS_SCALE = 6.5f;
    private static final float[][] FORMATION_POINTS = {
        // paste float[][] output from formation_sampler.py here
    };

    private static void spawnFormation(ServerLevel level, ServerPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        double rx = Math.cos(yaw),  rz = Math.sin(yaw);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);

        double cx = player.getX();
        double base = player.getY() + 0.05;
        double cz = player.getZ();

        if (FORMATION_POINTS.length > 0) {
            spawnFromPoints(level, cx, base + 0.10, cz, rx, rz, fx, fz, FORMATION_POINTS, POINTS_SCALE);
        } else {
            spawnRingLayer (level, cx, base + 0.00, cz, rx, rz, fx, fz, 9.5,  430);
            spawnRingLayer (level, cx, base + 0.00, cz, rx, rz, fx, fz, 10.0, 450);
            spawnGlyphRing (level, cx, base + 0.05, cz, rx, rz, fx, fz, 8.0,  22, 0.30f);
            spawnRingLayer (level, cx, base + 0.22, cz, rx, rz, fx, fz, 5.5,  250);
            spawnEnneagram (level, cx, base + 0.50, cz, rx, rz, fx, fz, 4.5,  30);
            spawnGlyphRing (level, cx, base + 0.58, cz, rx, rz, fx, fz, 2.8,  14, 0.22f);
            spawnRingLayer (level, cx, base + 1.20, cz, rx, rz, fx, fz, 1.4,  80);
            spawnRingLayer (level, cx, base + 1.30, cz, rx, rz, fx, fz, 0.30, 20);
        }
    }

    private static void spawnFromPoints(ServerLevel level,
            double cx, double cy, double cz,
            double rx, double rz, double fx, double fz,
            float[][] points, float scale) {
        for (float[] p : points) {
            double u = p[0] * scale;
            double v = p[1] * scale;
            level.sendParticles(
                    new FormationParticleOptions(cx, cy, cz, FormationParticleOptions.STATIC, 0f, 0f),
                    cx + u * rx + v * fx, cy, cz + u * rz + v * fz,
                    1, 0, 0, 0, 0);
        }
    }

    private static void spawnRingLayer(ServerLevel level,
            double cx, double cy, double cz,
            double rx, double rz, double fx, double fz,
            double radius, int count) {
        for (int i = 0; i < count; i++) {
            double theta = i * 2 * Math.PI / count;
            double u = Math.cos(theta) * radius;
            double v = Math.sin(theta) * radius;
            level.sendParticles(
                    new FormationParticleOptions(cx, cy, cz, FormationParticleOptions.STATIC, (float) radius, 0f),
                    cx + u * rx + v * fx, cy, cz + u * rz + v * fz,
                    1, 0, 0, 0, 0);
        }
    }

    // Stroke-based rune glyphs: each glyph = waypoints [{tang, rad}, ...]
    // Float.NaN = pen-up (lift stroke, don't connect to next point)
    // tang+ = clockwise along ring,  rad+ = outward from center
    // Coordinate range roughly [-2.5, 2.5]; scale applied at spawn time
    private static final float[][][] GLYPHS = {
        // 0: arch ∩  (doorway / portal)
        {{-1.5f,2.5f},{-1.5f,-0.5f},{-0.5f,-2f},{0.5f,-2f},{1.5f,-0.5f},{1.5f,2.5f}},

        // 1: ᚠ fehu-like  (vertical + 2 right branches)
        {{0f,-2.5f},{0f,2.5f},
         {Float.NaN,0},{0f,0.8f},{2f,2.2f},
         {Float.NaN,0},{0f,-0.6f},{2f,0.5f}},

        // 2: ᛏ tiwaz  (vertical + V notch at top)
        {{-1.5f,-0.8f},{0f,-2.5f},{1.5f,-0.8f},
         {Float.NaN,0},{0f,-2.5f},{0f,2.5f}},

        // 3: diamond ◇
        {{0f,-2.5f},{-2f,0f},{0f,2.5f},{2f,0f},{0f,-2.5f}},

        // 4: Ψ psi  (vertical + two curved arms)
        {{0f,-2.5f},{0f,2.5f},
         {Float.NaN,0},{-1.5f,-1.5f},{-1.5f,0.5f},{0f,1.5f},
         {Float.NaN,0},{1.5f,-1.5f},{1.5f,0.5f},{0f,1.5f}},

        // 5: Λ lambda with crossbar
        {{-2f,2.5f},{0f,-2.5f},{2f,2.5f},
         {Float.NaN,0},{-0.8f,0.5f},{0.8f,0.5f}},

        // 6: double horizontal bars  =
        {{-2f,-1f},{2f,-1f},
         {Float.NaN,0},{-2f,1f},{2f,1f}},

        // 7: hook with top serif  (ᚾ-like)
        {{0f,-2.5f},{0f,1f},{1.5f,1f},{1.5f,-0.5f},
         {Float.NaN,0},{-1.5f,-2.5f},{1.5f,-2.5f}},

        // 8: lightning zigzag  ⚡
        {{-2f,-2.5f},{1f,-0.5f},{-1f,0.5f},{2f,2.5f}},

        // 9: three-ray star  ✳ (Y shape)
        {{0f,0f},{0f,-2.5f},
         {Float.NaN,0},{0f,0f},{-2.2f,1.5f},
         {Float.NaN,0},{0f,0f},{2.2f,1.5f}},
    };

    // Renders each glyph as interpolated stroke particles.
    // particlesPerUnit = how many particles per glyphScale-unit of stroke length.
    private static void spawnGlyphRing(ServerLevel level,
            double cx, double cy, double cz,
            double rx, double rz, double fx, double fz,
            double radius, int glyphCount, float glyphScale) {
        final int PPU = 6; // particles per unit → with scale 0.30 → ~20 per block → solid line
        for (int g = 0; g < glyphCount; g++) {
            double theta = g * 2 * Math.PI / glyphCount;
            double sinT = Math.sin(theta), cosT = Math.cos(theta);
            float[][] glyph = GLYPHS[RNG.nextInt(GLYPHS.length)];

            float prevTang = Float.NaN, prevRad = Float.NaN;
            for (float[] pt : glyph) {
                float curTang = pt[0], curRad = pt[1];
                boolean penUp = Float.isNaN(curTang);

                if (!penUp && !Float.isNaN(prevTang)) {
                    double dTang = curTang - prevTang, dRad = curRad - prevRad;
                    int n = Math.max(2, (int)(Math.sqrt(dTang*dTang + dRad*dRad) * PPU));
                    for (int p = 0; p <= n; p++) {
                        double t = (double) p / n;
                        double du = (prevTang + t * dTang) * glyphScale;
                        double dv = (prevRad  + t * dRad)  * glyphScale;
                        double u = (radius + dv) * cosT - du * sinT;
                        double v = (radius + dv) * sinT + du * cosT;
                        level.sendParticles(
                                new FormationParticleOptions(cx, cy, cz, FormationParticleOptions.STATIC, (float)radius, 0f),
                                cx + u * rx + v * fx, cy, cz + u * rz + v * fz,
                                1, 0, 0, 0, 0);
                    }
                }
                prevTang = penUp ? Float.NaN : curTang;
                prevRad  = penUp ? Float.NaN : curRad;
            }
        }
    }

    // Enneagram {9/4}: 9 vertices, connect every 4th, interpolate pointsPerEdge particles per edge
    private static void spawnEnneagram(ServerLevel level,
            double cx, double cy, double cz,
            double rx, double rz, double fx, double fz,
            double radius, int pointsPerEdge) {
        int n = 9, step = 4;
        double[] vu = new double[n], vv = new double[n];
        for (int k = 0; k < n; k++) {
            double theta = k * 2 * Math.PI / n - Math.PI / 2;
            vu[k] = Math.cos(theta) * radius;
            vv[k] = Math.sin(theta) * radius;
        }
        int cur = 0;
        for (int e = 0; e < n; e++) {
            int nxt = (cur + step) % n;
            for (int p = 0; p < pointsPerEdge; p++) {
                double t = (double) p / pointsPerEdge;
                double u = vu[cur] + t * (vu[nxt] - vu[cur]);
                double v = vv[cur] + t * (vv[nxt] - vv[cur]);
                level.sendParticles(
                        new FormationParticleOptions(cx, cy, cz, FormationParticleOptions.STATIC, (float) radius, 0f),
                        cx + u * rx + v * fx, cy, cz + u * rz + v * fz,
                        1, 0, 0, 0, 0);
            }
            cur = nxt;
        }
    }
}
