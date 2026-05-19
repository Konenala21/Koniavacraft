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
 * Real-time 3-body gravitational simulation in the XZ plane.
 * Uses leapfrog integration. Draws fading trails for each body.
 */
public class NBodyRenderer {

    private static final int   TRAIL_MAX  = 180;
    private static final int   SUBSTEPS   = 4;
    private static final float SIM_DT     = 0.004f;
    private static final float SCALE      = 1.3f;
    private static final int   BODY_SEGS  = 12;
    // 3 bodies * TRAIL_MAX segs * 2 verts + body circles
    private static final int   FLOAT_CAP  = (3 * TRAIL_MAX + 3 * BODY_SEGS) * 2 * 7 + 128;

    private static final float[][] BODY_COLORS = {
        {1.0f, 0.3f, 0.2f},  // red
        {0.2f, 0.8f, 1.0f},  // cyan
        {0.4f, 1.0f, 0.3f},  // green
    };

    private static class NBodyEffect {
        final Vec3 origin;
        long  lastTick = -1;

        // State (positions, half-step velocities for leapfrog)
        float[] px, pz, vx, vz;
        final Deque<float[]>[] trails;

        @SuppressWarnings("unchecked")
        NBodyEffect(Vec3 origin) {
            this.origin = origin;
            // Figure-8 choreography initial conditions (Chenciner-Montgomery, scaled)
            px = new float[]{-0.97f * SCALE,  0f,            0.97f * SCALE};
            pz = new float[]{ 0.24f * SCALE,  0f,           -0.24f * SCALE};
            vx = new float[]{ 0.466f,         -0.932f,        0.466f};
            vz = new float[]{ 0.432f,         -0.865f,        0.432f};

            // Kick velocities back half-step for leapfrog
            for (int i = 0; i < 3; i++) {
                float[] acc = accel(i);
                vx[i] -= acc[0] * SIM_DT * 0.5f;
                vz[i] -= acc[1] * SIM_DT * 0.5f;
            }

            trails = new ArrayDeque[3];
            for (int i = 0; i < 3; i++) trails[i] = new ArrayDeque<>();
        }

        float[] accel(int i) {
            float ax = 0, az = 0;
            for (int j = 0; j < 3; j++) {
                if (j == i) continue;
                float dx = px[j] - px[i], dz = pz[j] - pz[i];
                float d3 = (float)Math.pow(dx*dx + dz*dz + 1e-4f, 1.5f);
                ax += dx / d3; az += dz / d3;
            }
            return new float[]{ax, az};
        }

        void step(long gameTick) {
            if (gameTick == lastTick) return;
            lastTick = gameTick;
            for (int s = 0; s < SUBSTEPS; s++) {
                // Leapfrog: kick position
                for (int i = 0; i < 3; i++) { px[i] += vx[i] * SIM_DT; pz[i] += vz[i] * SIM_DT; }
                // Recompute accelerations and kick velocities
                for (int i = 0; i < 3; i++) {
                    float[] a = accel(i);
                    vx[i] += a[0] * SIM_DT; vz[i] += a[1] * SIM_DT;
                }
            }
            for (int i = 0; i < 3; i++) {
                trails[i].addFirst(new float[]{px[i], pz[i]});
                if (trails[i].size() > TRAIL_MAX) trails[i].removeLast();
            }
        }
    }

    private static final List<NBodyEffect> effects = new ArrayList<>();
    private static int  prog = -1, vao = -1, vbo = -1, locProj, locMV;
    private static boolean init = false;
    private static FloatBuffer buf;

    public static void spawnEffect(Vec3 pos, long tick) {
        effects.removeIf(e -> e.origin.distanceToSqr(pos) < 1.0);
        if (effects.size() >= 2) effects.remove(0);
        effects.add(new NBodyEffect(pos));
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (effects.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!init) { doInit(); if (!init) return; }

        long   gameTick = mc.level.getGameTime();
        Vec3   cam      = event.getCamera().getPosition();
        float  partial  = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        for (NBodyEffect eff : effects) eff.step(gameTick);

        int prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        float[] proj = new float[16], mv = new float[16];
        event.getProjectionMatrix().get(proj); event.getModelViewMatrix().get(mv);
        GL20.glUseProgram(prog);
        GL20.glUniformMatrix4fv(locProj, false, proj);
        GL20.glUniformMatrix4fv(locMV,   false, mv);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        for (NBodyEffect eff : effects) {
            float ox = (float)(eff.origin.x - cam.x);
            float oy = (float)(eff.origin.y + 0.05 - cam.y);
            float oz = (float)(eff.origin.z - cam.z);

            buf.clear();
            int vertCount = 0;

            for (int b = 0; b < 3; b++) {
                float[] col = BODY_COLORS[b];
                // Trail
                int idx = 0;
                float[] prev = null;
                for (float[] pt : eff.trails[b]) {
                    if (prev != null) {
                        float alpha = (1.0f - (float)idx / TRAIL_MAX) * 0.85f;
                        buf.put(ox+prev[0]).put(oy).put(oz+prev[1]).put(col[0]).put(col[1]).put(col[2]).put(alpha);
                        buf.put(ox+pt[0]).put(oy).put(oz+pt[1]).put(col[0]).put(col[1]).put(col[2]).put(alpha * 0.7f);
                        vertCount += 2;
                    }
                    prev = pt; idx++;
                }
                // Current body circle
                float bx = eff.px[b], bz = eff.pz[b];
                for (int s = 0; s < BODY_SEGS; s++) {
                    float a0 = s       * (float)(2*Math.PI) / BODY_SEGS;
                    float a1 = (s + 1) * (float)(2*Math.PI) / BODY_SEGS;
                    float r = 0.12f;
                    buf.put(ox+bx+r*(float)Math.cos(a0)).put(oy).put(oz+bz+r*(float)Math.sin(a0)).put(col[0]).put(col[1]).put(col[2]).put(1.0f);
                    buf.put(ox+bx+r*(float)Math.cos(a1)).put(oy).put(oz+bz+r*(float)Math.sin(a1)).put(col[0]).put(col[1]).put(col[2]).put(1.0f);
                    vertCount += 2;
                }
            }
            buf.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STREAM_DRAW);
            GL11.glDrawArrays(GL11.GL_LINES, 0, vertCount);
        }

        GL30.glBindVertexArray(0); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL20.glUseProgram(prevProg);
        if (wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend)  GL11.glEnable(GL11.GL_BLEND);     else GL11.glDisable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void release() {
        if (prog != -1) { GL20.glDeleteProgram(prog); prog = -1; }
        if (vbo  != -1) { GL15.glDeleteBuffers(vbo);  vbo  = -1; }
        if (vao  != -1) { GL30.glDeleteVertexArrays(vao); vao = -1; }
        if (buf  != null) { MemoryUtil.memFree(buf); buf = null; }
        init = false; effects.clear();
    }

    private static void doInit() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            prog = buildProg(read(rm, "shaders/fourier_curve.vsh"), read(rm, "shaders/fourier_curve.fsh"));
            if (prog == -1) return;
            GL20.glUseProgram(prog); locProj = GL20.glGetUniformLocation(prog, "ProjMat"); locMV = GL20.glGetUniformLocation(prog, "ModelViewMat"); GL20.glUseProgram(0);
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[NBody] Shader load failed", e); return; }
        buf = MemoryUtil.memAllocFloat(FLOAT_CAP);
        vao = GL30.glGenVertexArrays(); vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long)FLOAT_CAP * Float.BYTES, GL15.GL_STREAM_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 28, 0);  GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 28, 12); GL20.glEnableVertexAttribArray(1);
        GL30.glBindVertexArray(0); init = true;
    }
    private static String read(ResourceManager rm, String p) throws IOException {
        try (InputStream in = rm.getResourceOrThrow(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, p)).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static int buildProg(String v, String f) {
        int vS = cs(GL20.GL_VERTEX_SHADER, v), fS = cs(GL20.GL_FRAGMENT_SHADER, f);
        int p = GL20.glCreateProgram(); GL20.glAttachShader(p, vS); GL20.glAttachShader(p, fS);
        GL20.glBindAttribLocation(p, 0, "Position"); GL20.glBindAttribLocation(p, 1, "Color");
        GL20.glLinkProgram(p); GL20.glDeleteShader(vS); GL20.glDeleteShader(fS);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) { GL20.glDeleteProgram(p); return -1; }
        return p;
    }
    private static int cs(int t, String s) { int id = GL20.glCreateShader(t); GL20.glShaderSource(id, s); GL20.glCompileShader(id); return id; }
}
