package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.space.orbit.PlanetDef;
import com.github.nalamodikk.space.orbit.StarDef;
import com.github.nalamodikk.space.orbit.StarSystem;
import com.github.nalamodikk.space.orbit.StarSystemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpacePlanetManager {

    private static final PlanetRenderer atmosphereRenderer = new PlanetRenderer(PlanetRenderer.Type.ATMOSPHERE);
    private static final PlanetRenderer rockyRenderer      = new PlanetRenderer(PlanetRenderer.Type.ROCKY);
    private static final PlanetRenderer sunRenderer        = new PlanetRenderer(PlanetRenderer.Type.SUN);
    private static final PlanetRenderer ringRenderer       = new PlanetRenderer(PlanetRenderer.Type.RING);
    private static final PlanetRenderer beltRenderer       = new PlanetRenderer(PlanetRenderer.Type.BELT);
    private static boolean initialized = false;
    private static int     initCooldown = 0; // AMD：init 後跳過 N 幀再開始渲染

    // 行星貼圖快取：planet id → GL texture id（-1=無貼圖/解碼失敗,不再試）。只在 render(主)執行緒讀寫。
    private static final Map<String, Integer> textureCache = new HashMap<>();

    // 背景解碼:大圖(earth 8K,134MB)STB 解碼要數秒,放 worker thread 跑 → render thread 不卡。
    private record Decoded(int width, int height, ByteBuffer pixels) {} // pixels==null => 解碼失敗哨兵
    private static final Map<String, Decoded> decoded = new ConcurrentHashMap<>();      // worker 放,主執行緒取走上傳
    private static final Set<String> decoding = ConcurrentHashMap.newKeySet();          // 正在排隊/解碼+上傳,防重複提交

    // 分幀上傳:解碼好的大貼圖一次 glTexSubImage2D 整張(134MB)會卡一下,改成每幀只上傳幾條 row,化整為零、不降畫質。
    // 上傳完成前該行星先用程序化(texId 還沒進 textureCache),完成才換貼圖 → 不會露出半填滿的亂色。
    private static final class Upload {
        final int texId, width, height; final ByteBuffer pixels; int rowsDone;
        Upload(int texId, int width, int height, ByteBuffer pixels) {
            this.texId = texId; this.width = width; this.height = height; this.pixels = pixels;
        }
    }
    private static final Map<String, Upload> uploading = new HashMap<>(); // 主執行緒:分幀上傳中
    private static final int UPLOAD_BYTES_PER_FRAME = 4 * 1024 * 1024;    // 每幀上傳預算 ~4MB(134MB 約 33 幀填滿,~0.5s)

    private static final ExecutorService DECODE_POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Koniava-PlanetTexDecode"); t.setDaemon(true); return t;
    });

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean inSpace = mc.level.dimension().equals(ModDimensions.SPACE);
        boolean onMoon  = mc.level.dimension().equals(ModDimensions.MOON);
        // 主世界爬升：在玩家正下方畫一顆會後退的地球，賣「離開地球」並對齊太空那邊的視覺
        float ascentAtmo = 0f;
        boolean overworldAscent = false;
        if (!inSpace && !onMoon) {
            ascentAtmo = AtmosphereTransition.blend(mc);
            if (ascentAtmo <= 0f) return;
            overworldAscent = true;
        }
        // 月球地底/中空（地殼底以下）不畫天空，否則在星球內部看到星空很怪
        if (onMoon && mc.player.getY() < com.github.nalamodikk.dimension.MoonChunkGenerator.CRUST_BOTTOM) return;

        processUploads(); // 每幀推進分幀貼圖上傳(限 byte 預算)
        if (!initialized) { init(); initCooldown = 3; return; }
        if (initCooldown > 0) { initCooldown--; return; }

        float partial  = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        // 單純用遊戲時間（含 partial tick），平滑不頓
        double accumulatedTime = mc.level.getGameTime() + partial;

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

        if (overworldAscent) {
            renderAscentEarth(mc, ascentAtmo, gameTime, invProj, invView, target.width, target.height);
            return;
        }

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

    /**
     * 主世界爬升時在玩家正下方畫一顆大地球（剛飛上來的感覺）。隨高度後退（dist 近→遠 → 地球越爬越小）+ 淡入（alpha=atmo）。
     * 半徑放大成 500（>太空地球的 76）→ 比切換瞬間的太空地球大很多，切換那刻會「縮一下」，這是大球換來的代價。
     * dist 起終點是獨立調的（不再綁引力區，那個已因大球放大失去對齊意義）。
     */
    private static void renderAscentEarth(Minecraft mc, float atmo, float gameTime,
                                          float[] invProj, float[] invView, int w, int h) {
        float dist   = Mth.lerp(atmo, 350f, 900f);          // 低空近(大) → 高空遠(小):縮速回上一版(範圍小=縮得慢)
        float radius = 2500f;                               // 超大爬升地球(塞滿下方;跟太空地球 76 差很多 → 切換會縮一下)
        float earthAlpha = Math.min(1f, atmo * 4f);         // 淡入跟大小解綁:早早全顯示,才看得到低空那顆最大的
        float angDeg = (float) Math.toDegrees(Math.atan(radius / dist));
        float cosAng = (float) Math.cos(Math.toRadians(angDeg));

        Vector3f earthDir = new Vector3f(0f, -1f, 0f); // 正下方

        // 光源：用主世界當下太陽方向，地球亮面跟主世界日照大致一致
        float sunAng = mc.level.getSunAngle(1.0f);
        Vector3f sunDir = new Vector3f((float) Math.cos(sunAng), (float) Math.sin(sunAng), 0.2f).normalize();

        int earthTex    = getTexture("earth");
        int earthAtmo   = getTexture("earth_atmo");
        int earthNight  = getTexture("earth_night");
        int earthNormal = getTexture("earth_normal");
        int earthSpec   = getTexture("earth_specular");

        atmosphereRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
            earthDir, dist, cosAng, sunDir,
            new Vector3f(0.18f, 0.42f, 0.68f), new Vector3f(0.35f, 0.62f, 1.0f),
            1.2f, 0.10f, false,
            earthTex, earthAtmo, earthNight, 0.002f, 0.00216f, earthAlpha, null, 1.001f, earthNormal, earthSpec);
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
        int earthNormal = getTexture("earth_normal");
        int earthSpec = getTexture("earth_specular");
        atmosphereRenderer.renderAtmosphere(invProj, invView, gameTime, w, h,
            earthDir, 1500f, earthCos, earthToSun,
            new Vector3f(0.18f, 0.42f, 0.68f), new Vector3f(0.35f, 0.62f, 1.0f),
            1.2f, 0.10f, false,
            earthTex, earthAtmo, earthNight, 0.002f, 0.00216f, 1.0f, null, 1.001f, earthNormal, earthSpec);
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
            // 日夜方向用「行星→太陽」算，不是玩家→太陽：玩家跟行星在太陽兩側時，玩家相對的太陽方向
            // 會跟行星相對的相反 → 亮面整個反掉。用行星位置算才物理正確(不管玩家站哪)。
            final Vector3f fPlanetLight = system.combinedLightDir(tick, planetPos);

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
            final int normalId = getTexture(planet.id() + "_normal"); // 只地球有，其他回 -1
            final int specId   = getTexture(planet.id() + "_specular"); // 海洋反光遮罩，只地球有
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
                        fDir, fDist, fCos, fPlanetLight,
                        planet.colorA(), planet.colorB(),
                        planet.atmoDensity(), planet.atmoHeight(), false,
                        texId, texId2, nightId, rotSpeed, cloudSpeed, fadeAlpha,
                        fOccDir, fOccCos, normalId, specId);
                } else {
                    rockyRenderer.renderRocky(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCos, fPlanetLight,
                        planet.colorA(), planet.colorB(),
                        planet.heatColor(), planet.heatAmount(), texId, fadeAlpha,
                        fOccDir, fOccCos);
                }
                if (planet.hasRings() && ringTexId != -1) {
                    ringRenderer.renderRing(invProj, invView, gameTime, w, h,
                        fDir, fDist, fCos, fPlanetLight,
                        planet.ringInner(), planet.ringOuter(), planet.ringTiltDeg(),
                        ringTexId, fadeAlpha);
                }
            }));
        }

        bodies.sort((a, b) -> Float.compare(b.dist(), a.dist()));
        bodies.forEach(b -> b.draw().run());
    }

    /**
     * 懶載入行星貼圖：assets/koniava/textures/space/&lt;id&gt;.jpg 或 .png。
     * 解碼(STB,大圖數秒)在背景執行緒;主執行緒只查 Resource + GL 上傳(每幀限流)。回 -1 = 還沒好/無貼圖。
     */
    private static int getTexture(String planetId) {
        Integer cached = textureCache.get(planetId);
        if (cached != null) return cached;          // 含 -1(無檔/解碼失敗,不再試)
        if (uploading.containsKey(planetId)) return -1; // 分幀上傳中:先用程序化,別重複解碼/分配

        // 背景解好了 → 分配空 texture + 排進分幀上傳(每幀幾條 row,不一次塞 134MB)
        Decoded ready = decoded.remove(planetId);
        if (ready != null) {
            if (ready.pixels() == null) { // 失敗哨兵
                textureCache.put(planetId, -1);
                decoding.remove(planetId);
                return -1;
            }
            int texId = allocTexture(ready.width(), ready.height());
            uploading.put(planetId, new Upload(texId, ready.width(), ready.height(), ready.pixels()));
            return -1; // 開始分幀上傳,填滿前先程序化
        }

        // 還沒解碼 → 主執行緒只查 Resource(快),讀 bytes + STB 解碼丟背景
        if (decoding.add(planetId)) {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            Resource res = null;
            for (String ext : new String[]{"jpg", "png"}) {
                var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,
                    "textures/space/" + planetId + "." + ext);
                var r = rm.getResource(loc);
                if (r.isPresent()) { res = r.get(); break; }
            }
            if (res == null) {
                textureCache.put(planetId, -1); // 沒這檔
                decoding.remove(planetId);
                return -1;
            }
            final Resource fres = res;
            DECODE_POOL.submit(() -> decoded.put(planetId, decodeOffThread(planetId, fres)));
        }
        return -1; // 解碼中
    }

    /** 背景執行緒：讀 bytes + STB 解碼成 RGBA。失敗回 pixels=null 哨兵(native pixels 由主執行緒上傳後釋放)。 */
    private static Decoded decodeOffThread(String planetId, Resource res) {
        try (InputStream in = res.open(); MemoryStack stack = MemoryStack.stackPush()) {
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
            buf.put(bytes).flip();
            var w = stack.mallocInt(1);
            var h = stack.mallocInt(1);
            var ch = stack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(buf, w, h, ch, 4);
            if (pixels == null) {
                KoniavacraftMod.LOGGER.warn("[SpacePlanet] STB failed {}: {}", planetId, STBImage.stbi_failure_reason());
                return new Decoded(0, 0, null);
            }
            return new Decoded(w.get(0), h.get(0), pixels);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.warn("[SpacePlanet] decode failed {}: {}", planetId, e.getMessage());
            return new Decoded(0, 0, null);
        }
    }

    /** 主執行緒：GL 生成 texture + 配置空間(AMD bug:先 null 配置,像素之後分幀 glTexSubImage2D 填)。 */
    private static int allocTexture(int width, int height) {
        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        return texId;
    }

    /**
     * 每幀推進分幀上傳:在 ~{@link #UPLOAD_BYTES_PER_FRAME} byte 預算內,對進行中的貼圖各上傳幾條 row。
     * 填滿才把 texId 放進 textureCache(行星才換成貼圖)、釋放 native pixels。化整為零 → 不卡、不降畫質。
     */
    private static void processUploads() {
        if (uploading.isEmpty()) return;
        int budget = UPLOAD_BYTES_PER_FRAME;
        var it = uploading.entrySet().iterator();
        while (it.hasNext() && budget > 0) {
            var e = it.next();
            Upload u = e.getValue();
            int rowBytes = u.width * 4;
            int rows = Math.min(Math.max(1, budget / rowBytes), u.height - u.rowsDone);
            ByteBuffer slice = u.pixels.duplicate();
            slice.position(u.rowsDone * rowBytes);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, u.texId);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, u.rowsDone, u.width, rows,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, slice);
            u.rowsDone += rows;
            budget -= rows * rowBytes;
            if (u.rowsDone >= u.height) { // 填滿 → 換貼圖、釋放
                STBImage.stbi_image_free(u.pixels);
                textureCache.put(e.getKey(), u.texId);
                decoding.remove(e.getKey());
                it.remove();
                KoniavacraftMod.LOGGER.info("[SpacePlanet] Uploaded {}x{} {}", u.width, u.height, e.getKey());
            }
        }
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
        // 但要釋放「還沒上傳完/還沒上傳」的 native pixels(STB 配置,不會被 GC),否則登出在首次載入中途會洩漏。
        for (Upload u : uploading.values()) STBImage.stbi_image_free(u.pixels);
        uploading.clear();
        for (Decoded d : decoded.values()) if (d.pixels() != null) STBImage.stbi_image_free(d.pixels());
        decoded.clear();
        decoding.clear();
    }
}
