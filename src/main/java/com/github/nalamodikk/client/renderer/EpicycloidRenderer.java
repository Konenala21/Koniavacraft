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
 * Epicycloid/Hypocycloid: rolling circle parametric curves.
 * k cycles 1..5 every 100 ticks. k=1 cardioid, k=2 nephroid, k=3..5 multi-cusp.
 * Also draws the fixed base circle and the rolling circle at current phase.
 */
public class EpicycloidRenderer {

    private static final int   SEGS        = 600;
    private static final float BASE_R      = 1.8f;
    private static final int   CYCLE       = 100;
    private static final float ROT_SPEED   = 0.014f;
    private static final int   FLOAT_CAP   = (SEGS + 120 + 120) * 2 * 7 + 128;

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
            float oy  = (float)(eff.pos.y + 0.05 - cam.y);
            float oz  = (float)(eff.pos.z - cam.z);
            float dt  = tick - eff.startTick;
            int   k   = 1 + ((int)(dt / CYCLE) % 5);
            float smallR = BASE_R / k;
            float phase  = ROT_SPEED * dt;  // current rolling angle

            buf.clear();
            int vertCount = 0;

            // Fixed base circle (dim white)
            for (int i = 0; i < 72; i++) {
                float a0 = i       * (float)(2 * Math.PI) / 72;
                float a1 = (i + 1) * (float)(2 * Math.PI) / 72;
                buf.put(ox + BASE_R * (float)Math.cos(a0)).put(oy).put(oz + BASE_R * (float)Math.sin(a0))
                   .put(0.5f).put(0.5f).put(0.5f).put(0.35f);
                buf.put(ox + BASE_R * (float)Math.cos(a1)).put(oy).put(oz + BASE_R * (float)Math.sin(a1))
                   .put(0.5f).put(0.5f).put(0.5f).put(0.35f);
                vertCount += 2;
            }

            // Rolling circle at current phase (dim cyan)
            float rollCX = (BASE_R + smallR) * (float)Math.cos(phase);
            float rollCZ = (BASE_R + smallR) * (float)Math.sin(phase);
            for (int i = 0; i < 36; i++) {
                float a0 = i       * (float)(2 * Math.PI) / 36;
                float a1 = (i + 1) * (float)(2 * Math.PI) / 36;
                buf.put(ox + rollCX + smallR * (float)Math.cos(a0)).put(oy).put(oz + rollCZ + smallR * (float)Math.sin(a0))
                   .put(0.2f).put(0.8f).put(1.0f).put(0.4f);
                buf.put(ox + rollCX + smallR * (float)Math.cos(a1)).put(oy).put(oz + rollCZ + smallR * (float)Math.sin(a1))
                   .put(0.2f).put(0.8f).put(1.0f).put(0.4f);
                vertCount += 2;
            }

            // Epicycloid trace (bright rainbow)
            for (int i = 0; i < SEGS; i++) {
                float t0 = i       * (float)(2 * Math.PI) * k / SEGS;
                float t1 = (i + 1) * (float)(2 * Math.PI) * k / SEGS;
                float x0 = (BASE_R + smallR) * (float)Math.cos(t0) - smallR * (float)Math.cos((BASE_R / smallR + 1) * t0);
                float z0 = (BASE_R + smallR) * (float)Math.sin(t0) - smallR * (float)Math.sin((BASE_R / smallR + 1) * t0);
                float x1 = (BASE_R + smallR) * (float)Math.cos(t1) - smallR * (float)Math.cos((BASE_R / smallR + 1) * t1);
                float z1 = (BASE_R + smallR) * (float)Math.sin(t1) - smallR * (float)Math.sin((BASE_R / smallR + 1) * t1);
                float[] c = hue((float)i / SEGS);
                buf.put(ox + x0).put(oy).put(oz + z0).put(c[0]).put(c[1]).put(c[2]).put(0.9f);
                buf.put(ox + x1).put(oy).put(oz + z1).put(c[0]).put(c[1]).put(c[2]).put(0.9f);
                vertCount += 2;
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
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[Epicycloid] Shader load failed", e); initFailed = true; return; }
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
