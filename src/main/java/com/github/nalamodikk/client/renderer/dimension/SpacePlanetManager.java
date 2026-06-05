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

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpacePlanetManager {

    private static final PlanetRenderer atmosphereRenderer = new PlanetRenderer(PlanetRenderer.Type.ATMOSPHERE);
    private static final PlanetRenderer rockyRenderer      = new PlanetRenderer(PlanetRenderer.Type.ROCKY);
    private static final PlanetRenderer sunRenderer        = new PlanetRenderer(PlanetRenderer.Type.SUN);
    private static final PlanetRenderer ringRenderer       = new PlanetRenderer(PlanetRenderer.Type.RING);
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
            if (sDir.dot(camFwd) < -0.5f) continue;

            float    cosAng = (float) Math.cos(Math.toRadians(angDeg));
            final float fSDist = sDist; final Vector3f fSDir = new Vector3f(sDir);
            final float fCos = cosAng; final Vector3f fColor = star.color();
            final int starTexId = getTexture(star.id());
            bodies.add(new Body(sDist, () ->
                sunRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
                    fSDir, fSDist, fCos, fSDir, fColor,
                    new Vector3f(1.0f, 0.6f, 0.2f), 1.5f, 0.0f, false,
                    starTexId, -1, -1, 0.002f, 0.002f, 1.0f, null, 1.001f)
            ));
        }

        // 第一趟：計算所有主行星位置（parentId 為空）
        java.util.Map<String, Vector3f> planetPositions = new java.util.HashMap<>();
        for (PlanetDef planet : system.planets()) {
            if (planet.parentId().isEmpty()) {
                planetPositions.put(planet.id(), planet.worldPositionAt(tick, starPos));
            }
        }
        // 第二趟：計算衛星位置（parentId 不為空，繞父行星）
        for (PlanetDef planet : system.planets()) {
            if (!planet.parentId().isEmpty()) {
                Vector3f parentPos = planetPositions.getOrDefault(planet.parentId(), starPos);
                planetPositions.put(planet.id(), planet.worldPositionAt(tick, parentPos));
            }
        }

        // 行星（含衛星）
        for (PlanetDef planet : system.planets()) {
            Vector3f planetPos = planetPositions.getOrDefault(planet.id(), starPos);
            Vector3f toPlanet  = new Vector3f(planetPos).sub(playerPos);
            float    dist      = toPlanet.length();
            if (dist < 0.1f) continue;
            Vector3f dir = new Vector3f(toPlanet).normalize();

            float angDeg = (float) Math.toDegrees(Math.atan(planet.physicalRadius() / dist));
            if (angDeg < 0.005f) continue;
            // 淡入：0.005°→0.08° 平滑出現，消除突然跳出感
            final float fadeAlpha = Math.min(1.0f, (angDeg - 0.005f) / 0.075f);

            // 剔除：靠近時視角超過 90°（angDeg > 90 = 玩家在星球內），不剔除
            // 遠距離只剔除完全背面（dot < -0.5）
            if (angDeg < 85f && dir.dot(camFwd) < -0.5f) continue;

            float cosAng = (float) Math.cos(Math.toRadians(angDeg));
            final float fDist = dist; final Vector3f fDir = new Vector3f(dir);
            final float fCos = cosAng;
            final int texId   = getTexture(planet.id());
            final int texId2  = getTexture(planet.id() + "_atmo");
            final int nightId = getTexture(planet.id() + "_night");
            final float rotSpeed   = planet.rotSpeedRadPerSec();
            final float cloudSpeed = rotSpeed * 1.08f; // 雲層比地表快 8%

            final int ringTexId = planet.hasRings() ? getTexture(planet.id() + "_ring") : -1;

            // 衛星遮擋：計算父行星是否在衛星前方（若是，傳遮擋參數消除跳躍）
            final Vector3f occDir = new Vector3f();
            final float occCos;
            if (!planet.parentId().isEmpty()) {
                Vector3f parentPos = planetPositions.getOrDefault(planet.parentId(), starPos);
                Vector3f toParent = new Vector3f(parentPos).sub(playerPos);
                float parentDist = Math.max(toParent.length(), 0.1f);
                if (parentDist < dist) {
                    // 父行星在衛星前面 → 計算遮擋角
                    occDir.set(toParent).normalize();
                    PlanetDef parentPlanet = system.planets().stream()
                        .filter(p -> p.id().equals(planet.parentId())).findFirst().orElse(null);
                    float parentRadius = parentPlanet != null ? parentPlanet.physicalRadius() : 0f;
                    occCos = (float) Math.cos(Math.atan(parentRadius / parentDist));
                } else { occCos = 1.001f; }
            } else { occCos = 1.001f; }

            final Vector3f fOccDir = occCos < 1.0f ? new Vector3f(occDir) : null;
            final float fOccCos = occCos;

            bodies.add(new Body(dist, () -> {
                if (planet.shaderType() == PlanetRenderer.Type.ATMOSPHERE) {
                    atmosphereRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCos, sunLight,
                        planet.colorA(), planet.colorB(),
                        planet.atmoDensity(), planet.atmoHeight(), false,
                        texId, texId2, nightId, rotSpeed, cloudSpeed, fadeAlpha,
                        fOccDir, fOccCos);
                } else {
                    rockyRenderer.renderRocky(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCos, sunLight,
                        planet.colorA(), planet.colorB(),
                        planet.heatColor(), planet.heatAmount(), texId, fadeAlpha,
                        fOccDir, fOccCos);
                }
                // 土星環（行星本體之後畫，前方環蓋在星球上）
                if (planet.hasRings() && ringTexId != -1) {
                    ringRenderer.renderRing(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCos, sunLight,
                        planet.ringInner(), planet.ringOuter(), planet.ringTiltDeg(),
                        ringTexId, fadeAlpha);
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
                 MemoryStack stack = MemoryStack.stackPush()) {
                byte[] bytes = in.readAllBytes();
                var buf = BufferUtils.createByteBuffer(bytes.length);
                buf.put(bytes).flip();

                var w = stack.mallocInt(1);
                var h = stack.mallocInt(1);
                var ch = stack.mallocInt(1);
                var pixels = STBImage.stbi_load_from_memory(buf, w, h, ch, 4);
                if (pixels == null) {
                    KoniavacraftMod.LOGGER.warn("[SpacePlanet] STB failed {}: {}", loc, STBImage.stbi_failure_reason());
                    continue;
                }
                texId = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    w.get(0), h.get(0), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                STBImage.stbi_image_free(pixels);
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
        ringRenderer.init();
        initialized = true;
    }

    public static void reload() {
        atmosphereRenderer.release();
        rockyRenderer.release();
        sunRenderer.release();
        ringRenderer.release();
        for (int id : textureCache.values()) {
            if (id != -1) GL11.glDeleteTextures(id);
        }
        textureCache.clear();
        initialized = false;
    }
}
