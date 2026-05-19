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
 * Animated Archimedean spiral in the XZ plane.
 * Two counter-rotating spirals with rainbow colour gradient.
 */
public class SpiralCurveRenderer {

    private static final float MAX_TURNS  = 4.0f;         // total spiral turns
    private static final int   SEGS       = 300;           // line segments per spiral
    private static final float SCALE      = 3.0f / (MAX_TURNS * (float)(2 * Math.PI)); // r = SCALE * theta
    private static final float ROT_SPEED  = 0.015f;       // radians per tick
    private static final int   FLOAT_CAP  = SEGS * 2 * 2 * 7 + 64; // 2 spirals, 2 verts per seg

    private static class ActiveEffect {
        final Vec3 pos; final long startTick;
        ActiveEffect(Vec3 p, long t) { pos = p; startTick = t; }
    }

    private static final List<ActiveEffect> effects = new ArrayList<>();
    private static int  prog = -1, vao = -1, vbo = -1, locProj, locMV;
    private static boolean init = false;
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
            float ox   = (float)(eff.pos.x - cam.x);
            float oy   = (float)(eff.pos.y + 0.08 - cam.y);
            float oz   = (float)(eff.pos.z - cam.z);
            float rot  = ROT_SPEED * (tick - eff.startTick);

            buf.clear();
            int vertCount = 0;

            // Draw two counter-rotating spirals
            for (int spiral = 0; spiral < 2; spiral++) {
                float dir = (spiral == 0) ? 1.0f : -1.0f;
                float phaseOff = spiral * (float) Math.PI;

                for (int i = 0; i < SEGS; i++) {
                    float t0 = i       * MAX_TURNS * (float)(2 * Math.PI) / SEGS;
                    float t1 = (i + 1) * MAX_TURNS * (float)(2 * Math.PI) / SEGS;
                    float r0 = SCALE * t0, r1 = SCALE * t1;
                    float a0 = dir * t0 + rot + phaseOff;
                    float a1 = dir * t1 + rot + phaseOff;

                    float frac0 = (float) i / SEGS, frac1 = (float)(i+1) / SEGS;
                    // Rainbow: red → yellow → green → cyan → blue → purple
                    float[] c0 = hue(frac0 + spiral * 0.5f);
                    float[] c1 = hue(frac1 + spiral * 0.5f);
                    float alpha = 0.5f + 0.4f * frac0;

                    buf.put(ox + r0*(float)Math.cos(a0)).put(oy).put(oz + r0*(float)Math.sin(a0))
                       .put(c0[0]).put(c0[1]).put(c0[2]).put(alpha);
                    buf.put(ox + r1*(float)Math.cos(a1)).put(oy).put(oz + r1*(float)Math.sin(a1))
                       .put(c1[0]).put(c1[1]).put(c1[2]).put(alpha);
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
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                                 GL11.GL_ONE,        GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** HSV to RGB, hue in [0,1]. */
    private static float[] hue(float h) {
        h = h % 1.0f; if (h < 0) h += 1.0f;
        int i = (int)(h * 6); float f = h * 6 - i;
        float q = 1 - f, t = f;
        return switch (i % 6) {
            case 0 -> new float[]{1, t, 0};
            case 1 -> new float[]{q, 1, 0};
            case 2 -> new float[]{0, 1, t};
            case 3 -> new float[]{0, q, 1};
            case 4 -> new float[]{t, 0, 1};
            default -> new float[]{1, 0, q};
        };
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
            prog = buildProg(read(rm,"shaders/fourier_curve.vsh"), read(rm,"shaders/fourier_curve.fsh"));
            if (prog == -1) return;
            GL20.glUseProgram(prog);
            locProj = GL20.glGetUniformLocation(prog, "ProjMat");
            locMV   = GL20.glGetUniformLocation(prog, "ModelViewMat");
            GL20.glUseProgram(0);
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[Spiral] Shader load failed", e); return; }
        buf = MemoryUtil.memAllocFloat(FLOAT_CAP);
        vao = GL30.glGenVertexArrays(); vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long)FLOAT_CAP * Float.BYTES, GL15.GL_STREAM_DRAW);
        GL20.glVertexAttribPointer(0,3,GL11.GL_FLOAT,false,28,0);  GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1,4,GL11.GL_FLOAT,false,28,12); GL20.glEnableVertexAttribArray(1);
        GL30.glBindVertexArray(0);
        init = true;
    }
    private static String read(ResourceManager rm, String p) throws IOException {
        try (InputStream in = rm.getResourceOrThrow(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, p)).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static int buildProg(String v, String f) {
        int vS = cs(GL20.GL_VERTEX_SHADER,v), fS = cs(GL20.GL_FRAGMENT_SHADER,f);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p,vS); GL20.glAttachShader(p,fS);
        GL20.glBindAttribLocation(p,0,"Position"); GL20.glBindAttribLocation(p,1,"Color");
        GL20.glLinkProgram(p); GL20.glDeleteShader(vS); GL20.glDeleteShader(fS);
        if (GL20.glGetProgrami(p,GL20.GL_LINK_STATUS)==GL11.GL_FALSE){GL20.glDeleteProgram(p);return -1;}
        return p;
    }
    private static int cs(int t, String s) {
        int id=GL20.glCreateShader(t); GL20.glShaderSource(id,s); GL20.glCompileShader(id); return id;
    }
}
