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
import java.util.ArrayList;
import java.util.List;

/**
 * Möbius strip wireframe in 3D.
 * x=(1+v/2*cos(u/2))*cos(u), y=v/2*sin(u/2), z=(1+v/2*cos(u/2))*sin(u)
 * u in [0,2pi], v in [-1,1]
 */
public class MobiusStripRenderer {

    private static final int   U_DIVS    = 60;
    private static final int   V_DIVS    = 12;
    private static final float SCALE     = 1.8f;
    private static final float ROT_SPEED = 0.007f;
    private static final int   FLOAT_CAP = (U_DIVS * V_DIVS * 2 + U_DIVS * (V_DIVS + 1)) * 2 * 7 + 128;

    private static class ActiveEffect {
        final Vec3 pos; final long startTick;
        ActiveEffect(Vec3 p, long t) { pos = p; startTick = t; }
    }

    private static final List<ActiveEffect> effects = new ArrayList<>();
    private static int  prog = -1, vao = -1, vbo = -1, locProj, locMV;
    private static boolean init = false;
    private static boolean initFailed = false;
    private static FloatBuffer buf;

    public static void spawnEffect(Vec3 pos, long tick) {
        effects.removeIf(e -> e.pos.distanceToSqr(pos) < 1.0);
        if (effects.size() >= 3) effects.remove(0);
        effects.add(new ActiveEffect(pos, tick));
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (effects.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (initFailed) return;
        if (!init) { doInit(); if (!init) return; }

        Vec3  cam  = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float tick = mc.level.getGameTime() + partial;

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

        for (ActiveEffect eff : effects) {
            float ox  = (float)(eff.pos.x - cam.x);
            float oy  = (float)(eff.pos.y + 1.2 - cam.y);
            float oz  = (float)(eff.pos.z - cam.z);
            float rot = ROT_SPEED * (tick - eff.startTick);

            buf.clear();
            int vertCount = 0;

            // u-direction lines (constant v, vary u)
            for (int vi = 0; vi <= V_DIVS; vi++) {
                float v = -1.0f + 2.0f * vi / V_DIVS;
                float[] c = hue(0.5f + 0.5f * v);
                for (int ui = 0; ui < U_DIVS; ui++) {
                    float u0 = ui       * (float)(2 * Math.PI) / U_DIVS;
                    float u1 = (ui + 1) * (float)(2 * Math.PI) / U_DIVS;
                    float[] p0 = mobiusPt(u0, v, rot);
                    float[] p1 = mobiusPt(u1, v, rot);
                    buf.put(ox+p0[0]).put(oy+p0[1]).put(oz+p0[2]).put(c[0]).put(c[1]).put(c[2]).put(0.85f);
                    buf.put(ox+p1[0]).put(oy+p1[1]).put(oz+p1[2]).put(c[0]).put(c[1]).put(c[2]).put(0.85f);
                    vertCount += 2;
                }
            }

            // v-direction lines (constant u, vary v)
            for (int ui = 0; ui < U_DIVS; ui++) {
                float u = ui * (float)(2 * Math.PI) / U_DIVS;
                float[] c = hue((float)ui / U_DIVS);
                for (int vi = 0; vi < V_DIVS; vi++) {
                    float v0 = -1.0f + 2.0f * vi       / V_DIVS;
                    float v1 = -1.0f + 2.0f * (vi + 1) / V_DIVS;
                    float[] p0 = mobiusPt(u, v0, rot);
                    float[] p1 = mobiusPt(u, v1, rot);
                    buf.put(ox+p0[0]).put(oy+p0[1]).put(oz+p0[2]).put(c[0]).put(c[1]).put(c[2]).put(0.6f);
                    buf.put(ox+p1[0]).put(oy+p1[1]).put(oz+p1[2]).put(c[0]).put(c[1]).put(c[2]).put(0.6f);
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

    /** Möbius strip point, rotated around Y by yRot. */
    private static float[] mobiusPt(float u, float v, float yRot) {
        float half = v / 2.0f;
        float rho = SCALE * (1.0f + half * (float)Math.cos(u / 2.0f));
        float wx  = rho * (float)Math.cos(u);
        float wy  = SCALE * half * (float)Math.sin(u / 2.0f);
        float wz  = rho * (float)Math.sin(u);
        float cos = (float)Math.cos(yRot), sin = (float)Math.sin(yRot);
        return new float[]{ wx * cos - wz * sin, wy, wx * sin + wz * cos };
    }

    public static void release() {
        if (prog != -1) { GL20.glDeleteProgram(prog); prog = -1; }
        if (vbo  != -1) { GL15.glDeleteBuffers(vbo);  vbo  = -1; }
        if (vao  != -1) { GL30.glDeleteVertexArrays(vao); vao = -1; }
        if (buf  != null) { MemoryUtil.memFree(buf); buf = null; }
        init = false; initFailed = false; effects.clear();
    }

    private static float[] hue(float h) {
        h = h % 1.0f; if (h < 0) h += 1.0f;
        int i = (int)(h * 6); float f = h * 6 - i, q = 1 - f, t = f;
        return switch (i % 6) {
            case 0 -> new float[]{1, t, 0}; case 1 -> new float[]{q, 1, 0};
            case 2 -> new float[]{0, 1, t}; case 3 -> new float[]{0, q, 1};
            case 4 -> new float[]{t, 0, 1}; default -> new float[]{1, 0, q};
        };
    }

    private static void doInit() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            prog = buildProg(read(rm, "shaders/fourier_curve.vsh"), read(rm, "shaders/fourier_curve.fsh"));
            if (prog == -1) { initFailed = true; return; }
            GL20.glUseProgram(prog); locProj = GL20.glGetUniformLocation(prog, "ProjMat"); locMV = GL20.glGetUniformLocation(prog, "ModelViewMat"); GL20.glUseProgram(0);
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[Mobius] Shader load failed", e); initFailed = true; return; }
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
