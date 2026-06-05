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
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
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
    private static final PlanetRenderer beltRenderer       = new PlanetRenderer(PlanetRenderer.Type.BELT);
    private static boolean initialized = false;
    private static int     initCooldown = 0; // AMD：init 後跳過 N 幀再開始渲染
    // 公轉/自轉時間倍率（由 /koniava timescale 設定，SpaceTimeScalePacket 同步）
    public static volatile float timeScale = 1.0f;
    // 累積的縮放時間：每幀只累加 delta*timeScale，改倍率時不會瞬移（只改速率）
    private static double accumulatedTime = 0.0;
    private static double lastRawTime = Double.NaN;

    // 行星貼圖快取：planet id → GL texture id（-1=無貼圖）
    private static final Map<String, Integer> textureCache = new HashMap<>();
    // 每幀最多上傳 N 張貼圖，避免首次進入維度時一幀全部上傳造成延遲
    private static final int MAX_TEXTURE_UPLOADS_PER_FRAME = 2;
    private static int textureUploadsThisFrame = 0;

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean inSpace = mc.level.dimension().equals(ModDimensions.SPACE);
        boolean onMoon  = mc.level.dimension().equals(ModDimensions.MOON);
        if (!inSpace && !onMoon) return;

        textureUploadsThisFrame = 0; // 每幀重置上傳計數
        if (!initialized) { init(); initCooldown = 3; return; }
        if (initCooldown > 0) { initCooldown--; return; }

        float partial  = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        // 累積縮放時間：每幀累加 delta*timeScale，改 timescale 不會瞬移（只變速率）
        double rawNow = mc.level.getGameTime() + partial;
        if (Double.isNaN(lastRawTime)) lastRawTime = rawNow;
        double delta = rawNow - lastRawTime;
        if (delta < 0) delta = 0; // 換世界/回繞保護
        accumulatedTime += delta * timeScale;
        lastRawTime = rawNow;

        float  gameTime  = (float)(accumulatedTime / 20.0);
        double tick      = accumulatedTime;
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

        if (onMoon) {
            // 月球天空：從地表往上看，太陽走天空弧（日夜）、地球固定掛天上有相位
            float partialT = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            renderMoonSky(mc, partialT, gameTime, invProj, invView, target.width, target.height);
            return;
        }

        for (StarSystem system : StarSystemRegistry.getActive()) {
            renderSystem(system, playerPos, camFwd, tick, gameTime,
                         invProj, invView, target.width, target.height);
        }
    }

    /** 月球天空：簡單明亮太陽（驅動日夜）+ 精緻地球（固定掛天上，相位由太陽方向自動算）。 */
    private static void renderMoonSky(Minecraft mc, float partial, float gameTime,
                                      float[] invProj, float[] invView, int w, int h) {
        // 太陽方向：綁 MC 自然日夜時間，跟地表明暗一致
        // dayTime: 0=日出(東), 6000=正午(頂), 12000=日落(西), 18000=午夜(地平線下)
        double t = (mc.level.getDayTime() % 24000L) / 24000.0;
        double ang = (t - 0.0) * 2.0 * Math.PI;  // 0..2π
        // 弧線：東(+X)升起 → 頂(+Y) → 西(-X)落下；午夜在地平線下
        Vector3f sunDir = new Vector3f(
            (float) Math.cos(ang),
            (float) Math.sin(ang),
            0.15f
        ).normalize();

        // 地球：潮汐鎖定固定掛在天空（南方高處），不隨日夜移動
        Vector3f earthDir = new Vector3f(0.0f, 0.55f, -0.84f).normalize();
        // 地球→太陽方向 = 太陽方向（太陽極遠，方向近似相同）→ 給地球 shader 算相位
        Vector3f earthToSun = new Vector3f(sunDir);

        // ── 太陽（簡單亮盤）：用 sun shader 但小尺寸，從地表看就是顆亮球 ──
        float sunCos = (float) Math.cos(Math.toRadians(2.2)); // 視角半徑 2.2°
        sunRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
            sunDir, 1500f, sunCos, sunDir,
            new Vector3f(1.0f, 0.95f, 0.80f),
            new Vector3f(1.0f, 0.7f, 0.3f), 1.2f, 0.0f, false,
            -1, -1, -1, 0.002f, 0.002f, 1.0f, null, 1.001f);

        // ── 地球（精緻，有相位/雲/夜景）視角半徑 6° ──
        float earthCos = (float) Math.cos(Math.toRadians(6.0));
        int earthTex   = getTexture("earth");
        int earthAtmo  = getTexture("earth_atmo");
        int earthNight = getTexture("earth_night");
        atmosphereRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
            earthDir, 1500f, earthCos, earthToSun,
            new Vector3f(0.18f, 0.42f, 0.68f), new Vector3f(0.35f, 0.62f, 1.0f),
            1.2f, 0.10f, false,
            earthTex, earthAtmo, earthNight, 0.002f, 0.00216f, 1.0f, null, 1.001f);
    }

    private static void renderSystem(StarSystem system, Vector3f playerPos, Vector3f camFwd,
                                     double tick, float gameTime,
                                     float[] invProj, float[] invView, int w, int h) {
        Vector3f starPos  = system.worldPos();
        Vector3f sunLight = system.combinedLightDir(tick, playerPos);

        // 帶最先畫（最遠的背景，岩石點疊在星空上、行星之下）
        Vector3f toStarB = new Vector3f(starPos).sub(playerPos);
        float    starDistB = Math.max(toStarB.length(), 0.1f);
        Vector3f starDirB  = new Vector3f(toStarB).normalize();
        for (var belt : system.belts()) {
            beltRenderer.renderBelt(invProj, invView, gameTime, w, h,
                starDirB, starDistB, sunLight,
                belt.innerRadius(), belt.outerRadius(), belt.thickness(),
                belt.density(), belt.color(), 1.0f);
        }

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
            if (sDir.dot(camFwd) < -0.95f) continue;

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

            // 完全背面才剔除（shader 早退更精確，Java 端不要過度剔除）
            if (dir.dot(camFwd) < -0.95f) continue;

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
        // 每幀上傳限制：超過上限本幀回傳 -1（下幀再試），避免首次進入卡頓
        if (textureUploadsThisFrame >= MAX_TEXTURE_UPLOADS_PER_FRAME) {
            return -1;
        }

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
                // AMD driver bug（atio6axx 24+/26.x）：
                // glTexImage2D 在 shader reload 後崩潰。
                // 修法：先用 null data 分配空間，再用 glTexSubImage2D 上傳像素
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    w.get(0), h.get(0), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                    (java.nio.ByteBuffer) null);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                    w.get(0), h.get(0), GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
                STBImage.stbi_image_free(pixels);
                textureUploadsThisFrame++;
                KoniavacraftMod.LOGGER.info("[SpacePlanet] Loaded {}x{} {}", w.get(0), h.get(0), loc);
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
        beltRenderer.init();
        initialized = true;
    }

    public static void reload() {
        // AMD driver 26.x bug：glDeleteTextures + 重新 glTexImage2D 會 EXCEPTION_ACCESS_VIOLATION
        // 所以 F3+T 完全不動：貼圖保留、shader 不重編
        // 代價：shader/貼圖變更需要完整重啟遊戲才生效（AMD 驅動限制，無法繞過）
    }
}
