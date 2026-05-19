package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Renders a dynamic Fourier epicycle visualization in world space.
 * N spinning circles whose tip traces out a curve over time.
 * Triggered by DevRenderTestItem2 on right-click.
 */
public class FourierCurveRenderer {

    // ── Fourier parameters ────────────────────────────────────────────────────

    private static final int   N          = 7;       // number of epicycles
    private static final float SCALE      = 1.8f;    // radius of first circle (blocks)
    private static final float PERIOD     = 80.0f;   // ticks per base revolution
    private static final int   TRAIL_MAX  = 340;     // max trail points (~4 revolutions)
    private static final int   SEGS       = 48;      // segments per circle outline

    // Odd harmonic coefficients for square-wave approximation
    private static final int[]   FREQ = new int[N];
    private static final float[] RAD  = new float[N];

    static {
        for (int i = 0; i < N; i++) {
            int k   = 2 * i + 1;
            // Alternate sign: even-index circles go CCW, odd-index go CW.
            // Result: x ≈ square wave, z ≈ triangle wave → traces a diamond shape.
            FREQ[i] = (i % 2 == 0) ? k : -k;
            RAD[i]  = SCALE * (float) (4.0 / Math.PI) / k;
        }
    }

    // ── Effect state ──────────────────────────────────────────────────────────

    private static class ActiveEffect {
        final Vec3           pos;
        final long           startTick;
        /** Tip offsets relative to pos, newest first. Each element: [dx, dz]. */
        final Deque<float[]> trail = new ArrayDeque<>();

        ActiveEffect(Vec3 pos, long startTick) {
            this.pos       = pos;
            this.startTick = startTick;
        }
    }

    private static final List<ActiveEffect> effects = new ArrayList<>();

    // ── GL resources ──────────────────────────────────────────────────────────

    private static int  prog = -1, vao = -1, vbo = -1;
    private static int  locProj, locMV;
    private static boolean initialized = false;
    private static FloatBuffer buf = null;

    // Pre-calculated vertex capacity
    private static final int VERT_CAPACITY =
            (N + 1)                   // arm joints
            + N * (SEGS + 1)          // circle outlines
            + TRAIL_MAX               // trail
            + 64;                     // padding
    private static final int FLOAT_CAPACITY = VERT_CAPACITY * 7; // 3 pos + 4 color

    // ── Public API ────────────────────────────────────────────────────────────

    public static void spawnEffect(Vec3 pos, long gameTick) {
        // Remove any effect too close (re-trigger at same spot replaces it)
        effects.removeIf(e -> e.pos.distanceToSqr(pos) < 1.0);
        // Cap at 3 simultaneous effects
        if (effects.size() >= 3) effects.remove(0);
        effects.add(new ActiveEffect(pos, gameTick));
        KoniavacraftMod.LOGGER.info("[FourierCurve] spawned at {}", pos);
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (effects.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (!initialized) {
            doInit();
            if (!initialized) return;
        }

        Vec3  cam     = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float tick    = mc.level.getGameTime() + partial;

        // ── Save GL state ────────────────────────────────────────────────────
        int     prevProg  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasBlend  = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDepth  = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        // Additive blending: bright lines glow against dark backgrounds
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // ── Upload matrices ──────────────────────────────────────────────────
        float[] projArr = new float[16];
        float[] mvArr   = new float[16];
        event.getProjectionMatrix().get(projArr);
        event.getModelViewMatrix().get(mvArr);

        GL20.glUseProgram(prog);
        GL20.glUniformMatrix4fv(locProj, false, projArr);
        GL20.glUniformMatrix4fv(locMV,   false, mvArr);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        for (ActiveEffect eff : effects) {
            float t = (tick - eff.startTick) / PERIOD * (float) (2 * Math.PI);

            // Camera-relative origin
            float ox = (float) (eff.pos.x - cam.x);
            float oy = (float) (eff.pos.y + 0.05 - cam.y);
            float oz = (float) (eff.pos.z - cam.z);

            // Compute arm joint offsets from origin
            float[] ax = new float[N + 1];
            float[] az = new float[N + 1];
            for (int i = 0; i < N; i++) {
                ax[i + 1] = ax[i] + RAD[i] * (float) Math.cos(FREQ[i] * t);
                az[i + 1] = az[i] + RAD[i] * (float) Math.sin(FREQ[i] * t);
            }

            // Add tip offset to trail
            eff.trail.addFirst(new float[]{ax[N], az[N]});
            if (eff.trail.size() > TRAIL_MAX) eff.trail.removeLast();

            buf.clear();

            // ── Section 1: Arms (GL_LINE_STRIP, N+1 verts) ──────────────────
            for (int i = 0; i <= N; i++) {
                putVertex(ox + ax[i], oy, oz + az[i], 1f, 1f, 1f, 0.85f);
            }
            int armCount = N + 1;

            // ── Section 2: Circle outlines (GL_LINE_STRIP per circle) ────────
            int[] circleStart = new int[N];
            for (int i = 0; i < N; i++) {
                circleStart[i] = buf.position() / 7;
                float cx = ox + ax[i], cz = oz + az[i];
                for (int s = 0; s <= SEGS; s++) {
                    float a = s * (float) (2 * Math.PI) / SEGS;
                    putVertex(
                            cx + RAD[i] * (float) Math.cos(a),
                            oy,
                            cz + RAD[i] * (float) Math.sin(a),
                            0.2f, 0.7f, 1.0f, 0.50f
                    );
                }
            }

            // ── Section 3: Trail (GL_LINE_STRIP) ────────────────────────────
            int trailStart = buf.position() / 7;
            int trailSize  = eff.trail.size();
            int idx        = 0;
            for (float[] pt : eff.trail) {
                float frac  = 1.0f - (float) idx / trailSize;
                float alpha = frac * frac * 0.95f;          // quadratic fade
                float r     = 0.1f + 0.4f * (1.0f - frac); // fades toward purple
                float g     = frac * 0.9f;
                float b     = 1.0f;
                putVertex(ox + pt[0], oy, oz + pt[1], r, g, b, alpha);
                idx++;
            }

            buf.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STREAM_DRAW);

            // Draw arms
            GL11.glDrawArrays(GL11.GL_LINE_STRIP, 0, armCount);

            // Draw circle outlines
            for (int i = 0; i < N; i++) {
                GL11.glDrawArrays(GL11.GL_LINE_STRIP, circleStart[i], SEGS + 1);
            }

            // Draw trail
            if (trailSize >= 2) {
                GL11.glDrawArrays(GL11.GL_LINE_STRIP, trailStart, trailSize);
            }
        }

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // ── Restore GL state ─────────────────────────────────────────────────
        GL20.glUseProgram(prevProg);
        if (wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend)  GL11.glEnable(GL11.GL_BLEND);     else GL11.glDisable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                                 GL11.GL_ONE,        GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void release() {
        if (prog != -1) { GL20.glDeleteProgram(prog);      prog = -1; }
        if (vbo  != -1) { GL15.glDeleteBuffers(vbo);       vbo  = -1; }
        if (vao  != -1) { GL30.glDeleteVertexArrays(vao);  vao  = -1; }
        if (buf  != null) { MemoryUtil.memFree(buf);        buf  = null; }
        initialized = false;
        effects.clear();
    }

    // ── Init / helpers ────────────────────────────────────────────────────────

    private static void doInit() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = readShader(rm, "shaders/fourier_curve.vsh");
            String frag = readShader(rm, "shaders/fourier_curve.fsh");
            prog = buildProgram(vert, frag);
            if (prog == -1) return;
            GL20.glUseProgram(prog);
            locProj = GL20.glGetUniformLocation(prog, "ProjMat");
            locMV   = GL20.glGetUniformLocation(prog, "ModelViewMat");
            GL20.glUseProgram(0);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[FourierCurve] Shader load failed", e);
            return;
        }

        buf = MemoryUtil.memAllocFloat(FLOAT_CAPACITY);

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) FLOAT_CAPACITY * Float.BYTES, GL15.GL_STREAM_DRAW);
        // attrib 0: Position (xyz, stride 28 bytes, offset 0)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 28, 0);
        GL20.glEnableVertexAttribArray(0);
        // attrib 1: Color (rgba, stride 28 bytes, offset 12)
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 28, 12);
        GL20.glEnableVertexAttribArray(1);
        GL30.glBindVertexArray(0);

        initialized = true;
        KoniavacraftMod.LOGGER.info("[FourierCurve] Initialized ({} max verts)", VERT_CAPACITY);
    }

    private static void putVertex(float x, float y, float z, float r, float g, float b, float a) {
        buf.put(x).put(y).put(z).put(r).put(g).put(b).put(a);
    }

    private static String readShader(ResourceManager rm, String path) throws IOException {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int buildProgram(String vertSrc, String fragSrc) {
        int v = compileShader(GL20.GL_VERTEX_SHADER,   vertSrc);
        int f = compileShader(GL20.GL_FRAGMENT_SHADER, fragSrc);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        GL20.glBindAttribLocation(p, 0, "Position");
        GL20.glBindAttribLocation(p, 1, "Color");
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[FourierCurve] Link error: {}", GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p);
            return -1;
        }
        return p;
    }

    private static int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[FourierCurve] Compile error: {}", GL20.glGetShaderInfoLog(id));
        return id;
    }
}
