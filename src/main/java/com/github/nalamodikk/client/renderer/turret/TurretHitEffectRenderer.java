package com.github.nalamodikk.client.renderer.turret;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class TurretHitEffectRenderer {

    private static final int SEGMENTS = 32;
    // GL_TRIANGLES: each quad = 2 triangles = 6 verts
    // Per effect: ring (SEGMENTS * 6) + trail (SEGMENTS * 6) + flash (6) = SEGMENTS*12 + 6
    private static final int VERTS_PER_EFFECT = SEGMENTS * 12 + 6;
    // 7 floats per vertex: xyz + rgba
    private static final int FLOATS_PER_VERT = 7;
    private static final int MAX_EFFECTS      = 16;
    private static final FloatBuffer BUF =
            BufferUtils.createFloatBuffer(MAX_EFFECTS * VERTS_PER_EFFECT * FLOATS_PER_VERT);

    private static int programId   = -1;
    private static int vaoId       = -1;
    private static int vboId       = -1;
    private static int locProj, locModelView;
    private static boolean initialized = false;
    private static boolean initFailed  = false;

    private static final float[] PROJ_ARR     = new float[16];
    private static final float[] MODELVIEW_ARR = new float[16];

    // ── Event ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long  gameTime = mc.level.getGameTime();
        float pt       = event.getPartialTick().getGameTimeDeltaTicks();

        TurretHitEffectManager.prune(gameTime);
        List<TurretHitEffectManager.HitEffect> effects = TurretHitEffectManager.getActive();
        if (effects.isEmpty()) return;

        if (!initialized) {
            if (initFailed) return;
            init();
            if (!initialized) return;
        }

        event.getProjectionMatrix().get(PROJ_ARR);
        event.getModelViewMatrix().get(MODELVIEW_ARR);

        Vec3 cam = event.getCamera().getPosition();

        // Build geometry
        BUF.clear();
        for (var effect : effects) {
            float age      = (gameTime - effect.spawnTick()) + pt;
            float progress = age / TurretHitEffectManager.DURATION_TICKS;
            if (progress < 0 || progress >= 1.0f) continue;
            buildRing(BUF, effect, progress, cam);
        }
        BUF.flip();

        if (BUF.limit() == 0) return;

        // Save GL state
        int  prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDep = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBl  = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDW  = GL11.glGetInteger(GL11.GL_DEPTH_WRITEMASK) != 0;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST); // keep depth test — ring blends naturally with scene
        GL11.glDepthMask(false); // don't write to depth buffer (ring is translucent)

        GL20.glUseProgram(programId);
        GL20.glUniformMatrix4fv(locProj,      false, PROJ_ARR);
        GL20.glUniformMatrix4fv(locModelView, false, MODELVIEW_ARR);

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, BUF, GL15.GL_STREAM_DRAW);

        int vertexCount = BUF.limit() / FLOATS_PER_VERT;
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);

        GL30.glBindVertexArray(0);
        GL20.glUseProgram(prevProg);

        // Restore GL state
        if (wasDep) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (!wasBl) GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(wasDW);
    }

    // ── Geometry builder ────────────────────────────────────────────────────

    private static void buildRing(FloatBuffer buf,
                                   TurretHitEffectManager.HitEffect effect,
                                   float progress, Vec3 cam) {
        float chargeRatio = effect.chargeRatio();
        float maxRadius   = 1.0f; // compact, focused hit feedback
        float expandedR   = maxRadius * (0.30f + progress * 0.70f); // start at 30%, immediately visible
        float ringWidth   = maxRadius * 0.20f * (1.0f - progress * 0.50f); // 20% width, thins as it expands
        float outerR      = expandedR;
        float innerR      = Math.max(0.02f, expandedR - ringWidth);

        float fadeAlpha = (float) Math.pow(1.0 - progress, 1.3 - chargeRatio * 0.35);

        float r  = (50  + chargeRatio * 160) / 255f;
        float g  = (140 + chargeRatio * 90)  / 255f;
        float b  = 1.0f;
        float ai = fadeAlpha * 0.90f;
        float ao = fadeAlpha * 0.30f;

        // Camera-relative center of effect
        float cx = (float)(effect.pos().x - cam.x);
        float cy = (float)(effect.pos().y - cam.y);
        float cz = (float)(effect.pos().z - cam.z);

        // Main ring: SEGMENTS × 2 triangles each
        for (int i = 0; i < SEGMENTS; i++) {
            float a1 = (float)(i       * 2 * Math.PI / SEGMENTS);
            float a2 = (float)((i + 1) * 2 * Math.PI / SEGMENTS);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);

            // Triangle 1: inner1, outer1, outer2
            putVert(buf, cx + innerR * c1, cy, cz + innerR * s1, r, g, b, ai);
            putVert(buf, cx + outerR * c1, cy, cz + outerR * s1, r, g, b, ao);
            putVert(buf, cx + outerR * c2, cy, cz + outerR * s2, r, g, b, ao);
            // Triangle 2: inner1, outer2, inner2
            putVert(buf, cx + innerR * c1, cy, cz + innerR * s1, r, g, b, ai);
            putVert(buf, cx + outerR * c2, cy, cz + outerR * s2, r, g, b, ao);
            putVert(buf, cx + innerR * c2, cy, cz + innerR * s2, r, g, b, ai);
        }

        // Trail ring: slightly smaller, dimmer
        float tOuter = innerR;
        float tInner = Math.max(0.01f, innerR * 0.82f);
        float tai    = progress > 0.08f ? fadeAlpha * 0.40f : 0f;
        for (int i = 0; i < SEGMENTS; i++) {
            float a1 = (float)(i       * 2 * Math.PI / SEGMENTS);
            float a2 = (float)((i + 1) * 2 * Math.PI / SEGMENTS);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);

            putVert(buf, cx + tInner * c1, cy, cz + tInner * s1, r, g, b, tai);
            putVert(buf, cx + tOuter * c1, cy, cz + tOuter * s1, r, g, b, 0f);
            putVert(buf, cx + tOuter * c2, cy, cz + tOuter * s2, r, g, b, 0f);
            putVert(buf, cx + tInner * c1, cy, cz + tInner * s1, r, g, b, tai);
            putVert(buf, cx + tOuter * c2, cy, cz + tOuter * s2, r, g, b, 0f);
            putVert(buf, cx + tInner * c2, cy, cz + tInner * s2, r, g, b, tai);
        }

        // Centre flash (first 15%): 2 triangles
        if (progress < 0.15f) {
            float ft = 1.0f - progress / 0.15f;
            float fa = ft * ft * 0.70f;
            float fr = maxRadius * 0.35f * (1.0f - ft * 0.5f);
            putVert(buf, cx - fr, cy, cz - fr, 0.82f, 0.92f, 1f, fa);
            putVert(buf, cx + fr, cy, cz - fr, 0.82f, 0.92f, 1f, fa);
            putVert(buf, cx + fr, cy, cz + fr, 0.82f, 0.92f, 1f, fa);
            putVert(buf, cx - fr, cy, cz - fr, 0.82f, 0.92f, 1f, fa);
            putVert(buf, cx + fr, cy, cz + fr, 0.82f, 0.92f, 1f, fa);
            putVert(buf, cx - fr, cy, cz + fr, 0.82f, 0.92f, 1f, fa);
        } else {
            // Degenerate triangles to keep count predictable
            for (int v = 0; v < 6; v++) putVert(buf, cx, cy, cz, 0, 0, 0, 0);
        }
    }

    private static void putVert(FloatBuffer buf,
                                 float x, float y, float z,
                                 float r, float g, float b, float a) {
        buf.put(x).put(y).put(z).put(r).put(g).put(b).put(a);
    }

    // ── GL setup ─────────────────────────────────────────────────────────────

    private static void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/turret/hit_ring.vsh");
            String frag = read(rm, "shaders/turret/hit_ring.fsh");

            int v = compile(GL20.GL_VERTEX_SHADER,   vert);
            int f = compile(GL20.GL_FRAGMENT_SHADER, frag);
            if (initFailed) { GL20.glDeleteShader(v); GL20.glDeleteShader(f); return; }

            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, v);
            GL20.glAttachShader(programId, f);
            // Bind attribute locations BEFORE linking (layout qualifiers require GLSL 330)
            GL20.glBindAttribLocation(programId, 0, "Position");
            GL20.glBindAttribLocation(programId, 1, "Color");
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);

            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                KoniavacraftMod.LOGGER.error("[HitRing] Link: {}", GL20.glGetProgramInfoLog(programId));
                GL20.glDeleteProgram(programId);
                programId = -1;
                initFailed = true;
                return;
            }

            GL20.glUseProgram(programId);
            locProj      = GL20.glGetUniformLocation(programId, "ProjMat");
            locModelView = GL20.glGetUniformLocation(programId, "ModelViewMat");
            GL20.glUseProgram(0);

            // VAO + VBO
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

            int stride = FLOATS_PER_VERT * Float.BYTES;
            // Position (location 0)
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
            GL20.glEnableVertexAttribArray(0);
            // Color (location 1)
            GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);

            GL30.glBindVertexArray(0);
            initialized = true;
            KoniavacraftMod.LOGGER.debug("[HitRing] Shader initialized.");
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[HitRing] Init failed", e);
            if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
            initFailed = true;
        }
    }

    public static void reload() { initFailed = false; release(); }

    public static void release() {
        if (programId != -1) { GL20.glDeleteProgram(programId);  programId = -1; }
        if (vboId     != -1) { GL15.glDeleteBuffers(vboId);      vboId     = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId); vaoId     = -1; }
        initialized = false;
    }

    private static String read(ResourceManager rm, String path) throws IOException {
        var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int compile(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[HitRing] Compile: {}", GL20.glGetShaderInfoLog(id));
            initFailed = true;
        }
        return id;
    }
}
