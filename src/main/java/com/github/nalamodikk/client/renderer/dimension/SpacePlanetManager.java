package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.space.orbit.PlanetDef;
import com.github.nalamodikk.space.orbit.StarDef;
import com.github.nalamodikk.space.orbit.StarSystem;
import com.github.nalamodikk.space.orbit.StarSystemRegistry;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

public class SpacePlanetManager {

    private static final PlanetRenderer atmosphereRenderer = new PlanetRenderer(PlanetRenderer.Type.ATMOSPHERE);
    private static final PlanetRenderer rockyRenderer      = new PlanetRenderer(PlanetRenderer.Type.ROCKY);
    private static final PlanetRenderer sunRenderer        = new PlanetRenderer(PlanetRenderer.Type.SUN);
    private static boolean initialized = false;

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)) return;

        if (!initialized) init();

        float partial  = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float gameTime = (mc.level.getGameTime() + partial) / 20.0f;
        long  tick     = mc.level.getGameTime();
        var   target   = mc.getMainRenderTarget();

        float[] invProj = new float[16];
        float[] invView = new float[16];
        new Matrix4f(event.getProjectionMatrix()).invert().get(invProj);
        new Matrix4f(event.getModelViewMatrix()).invert().get(invView);

        // 玩家在太空維度的實際位置
        Vector3f playerPos = new Vector3f(
            (float) mc.player.getX(),
            (float) mc.player.getY(),
            (float) mc.player.getZ()
        );

        // 相機前向（用於背面剔除）
        Vector3f camFwd = new Vector3f(-invView[8], -invView[9], -invView[10]).normalize();

        for (StarSystem system : StarSystemRegistry.ALL) {
            renderSystem(system, playerPos, camFwd, tick, gameTime,
                         invProj, invView, target.width, target.height);
        }
    }

    private static void renderSystem(StarSystem system, Vector3f playerPos, Vector3f camFwd,
                                     long tick, float gameTime,
                                     float[] invProj, float[] invView, int w, int h) {
        Vector3f starPos  = system.worldPos();
        Vector3f sunLight = system.combinedLightDir(tick, playerPos);

        // ── 收集可見天體（恆星 + 行星），由遠到近排序 ────────────────────
        record Body(float dist, Runnable draw) {}
        List<Body> bodies = new ArrayList<>();

        // 每顆恆星個別渲染
        for (StarDef star : system.stars()) {
            Vector3f sPos  = star.worldPositionAt(tick, starPos);
            Vector3f toStar = new Vector3f(sPos).sub(playerPos);
            float sDist = Math.max(toStar.length(), 0.1f);
            Vector3f sDir = new Vector3f(toStar).normalize();

            float starAngDeg = (float) Math.toDegrees(Math.atan(star.radius() / sDist));
            if (starAngDeg < 0.01f) continue;
            if (sDir.dot(camFwd) < -(float)Math.sin(Math.toRadians(starAngDeg * 3.5f))) continue;

            float cosAng = (float) Math.cos(Math.toRadians(starAngDeg));
            final float fSDist = sDist; final Vector3f fSDir = sDir; final float fCosAng = cosAng;
            final Vector3f fColor = star.color();
            bodies.add(new Body(sDist, () ->
                sunRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
                    fSDir, fSDist, fCosAng, fSDir, fColor,
                    new Vector3f(1.0f, 0.6f, 0.2f), 1.5f, 0.0f, false)
            ));
        }

        // 行星
        for (PlanetDef planet : system.planets()) {
            Vector3f planetPos = planet.worldPositionAt(tick, starPos);
            Vector3f toPlanet  = new Vector3f(planetPos).sub(playerPos);
            float    dist      = toPlanet.length();
            if (dist < 0.1f) continue;
            Vector3f dir = new Vector3f(toPlanet).normalize();

            // 動態視角大小
            float angDeg = (float) Math.toDegrees(Math.atan(planet.physicalRadius() / dist));
            if (angDeg < 0.02f) continue;  // 太小看不到

            // 背面剔除（含大氣層範圍）
            float atmoFac = 1.0f + planet.atmoHeight();
            float cullSin = (float) Math.sin(Math.toRadians(angDeg * atmoFac * 1.5f));
            if (dir.dot(camFwd) < -cullSin) continue;

            float cosAng = (float) Math.cos(Math.toRadians(angDeg));
            final float fDist = dist;
            final Vector3f fDir = dir;
            final float fCosAng = cosAng;

            bodies.add(new Body(dist, () -> {
                if (planet.shaderType() == PlanetRenderer.Type.ATMOSPHERE) {
                    atmosphereRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCosAng, sunLight,
                        planet.colorA(), planet.colorB(),
                        planet.atmoDensity(), planet.atmoHeight(), false);
                } else {
                    rockyRenderer.renderRocky(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCosAng,
                        planet.colorA(), planet.colorB(),
                        planet.heatColor(), planet.heatAmount());
                }
            }));
        }

        // 遠的先畫（畫家算法）
        bodies.sort((a, b) -> Float.compare(b.dist(), a.dist()));
        bodies.forEach(b -> b.draw().run());
    }

    private static void init() {
        KoniavacraftMod.LOGGER.info("[SpacePlanet] init renderers");
        atmosphereRenderer.init();
        rockyRenderer.init();
        sunRenderer.init();
        initialized = true;
    }

    public static void reload() {
        atmosphereRenderer.release();
        rockyRenderer.release();
        sunRenderer.release();
        initialized = false;
    }
}
