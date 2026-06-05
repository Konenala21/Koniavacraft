package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.space.orbit.PlanetDef;
import com.github.nalamodikk.space.orbit.StarDef;
import com.github.nalamodikk.space.orbit.StarSystem;
import com.github.nalamodikk.space.orbit.StarSystemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpacePlanetManager {

    private static final PlanetRenderer atmosphereRenderer = new PlanetRenderer(PlanetRenderer.Type.ATMOSPHERE);
    private static final PlanetRenderer rockyRenderer      = new PlanetRenderer(PlanetRenderer.Type.ROCKY);
    private static final PlanetRenderer sunRenderer        = new PlanetRenderer(PlanetRenderer.Type.SUN);
    private static boolean initialized = false;

    // 行星貼圖快取：planet id → GL texture id（-1=無貼圖）
    private static final Map<String, Integer> textureCache = new HashMap<>();

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

        Vector3f playerPos = new Vector3f(
            (float) mc.player.getX(),
            (float) mc.player.getY(),
            (float) mc.player.getZ()
        );
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

        record Body(float dist, Runnable draw) {}
        List<Body> bodies = new ArrayList<>();

        // 恆星
        for (StarDef star : system.stars()) {
            Vector3f sPos   = star.worldPositionAt(tick, starPos);
            Vector3f toStar = new Vector3f(sPos).sub(playerPos);
            float    sDist  = Math.max(toStar.length(), 0.1f);
            Vector3f sDir   = new Vector3f(toStar).normalize();

            float angDeg = (float) Math.toDegrees(Math.atan(star.radius() / sDist));
            if (angDeg < 0.01f) continue;
            if (sDir.dot(camFwd) < -(float) Math.sin(Math.toRadians(angDeg * 3.5f))) continue;

            float    cosAng = (float) Math.cos(Math.toRadians(angDeg));
            final float fSDist = sDist; final Vector3f fSDir = new Vector3f(sDir);
            final float fCos = cosAng; final Vector3f fColor = star.color();
            bodies.add(new Body(sDist, () ->
                sunRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
                    fSDir, fSDist, fCos, fSDir, fColor,
                    new Vector3f(1.0f, 0.6f, 0.2f), 1.5f, 0.0f, false, -1)
            ));
        }

        // 行星
        for (PlanetDef planet : system.planets()) {
            Vector3f planetPos = planet.worldPositionAt(tick, starPos);
            Vector3f toPlanet  = new Vector3f(planetPos).sub(playerPos);
            float    dist      = toPlanet.length();
            if (dist < 0.1f) continue;
            Vector3f dir = new Vector3f(toPlanet).normalize();

            float angDeg = (float) Math.toDegrees(Math.atan(planet.physicalRadius() / dist));
            if (angDeg < 0.02f) continue;

            float atmoFac = 1.0f + planet.atmoHeight();
            float cullSin = (float) Math.sin(Math.toRadians(angDeg * atmoFac * 1.5f));
            if (dir.dot(camFwd) < -cullSin) continue;

            float cosAng = (float) Math.cos(Math.toRadians(angDeg));
            final float fDist = dist; final Vector3f fDir = new Vector3f(dir);
            final float fCos = cosAng;
            final int texId = getTexture(planet.id());

            bodies.add(new Body(dist, () -> {
                if (planet.shaderType() == PlanetRenderer.Type.ATMOSPHERE) {
                    atmosphereRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCos, sunLight,
                        planet.colorA(), planet.colorB(),
                        planet.atmoDensity(), planet.atmoHeight(), false, texId);
                } else {
                    rockyRenderer.renderRocky(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCos, sunLight,
                        planet.colorA(), planet.colorB(),
                        planet.heatColor(), planet.heatAmount(), texId);
                }
            }));
        }

        bodies.sort((a, b) -> Float.compare(b.dist(), a.dist()));
        bodies.forEach(b -> b.draw().run());
    }

    // 懶載入行星貼圖：assets/koniava/textures/space/<id>.jpg 或 .png
    private static int getTexture(String planetId) {
        if (textureCache.containsKey(planetId)) return textureCache.get(planetId);

        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        int texId = -1;

        for (String ext : new String[]{"jpg", "png"}) {
            var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,
                "textures/space/" + planetId + "." + ext);
            var res = rm.getResource(loc);
            if (res.isEmpty()) continue;

            try (InputStream in = res.get().open();
                 NativeImage img = NativeImage.read(in)) {
                texId = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                img.upload(0, 0, 0, false);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                KoniavacraftMod.LOGGER.info("[SpacePlanet] Loaded texture: {}", loc);
                break;
            } catch (Exception e) {
                KoniavacraftMod.LOGGER.warn("[SpacePlanet] Failed to load {}: {}", loc, e.getMessage());
            }
        }

        textureCache.put(planetId, texId);
        return texId;
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
        for (int id : textureCache.values()) {
            if (id != -1) GL11.glDeleteTextures(id);
        }
        textureCache.clear();
        initialized = false;
    }
}
