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
 * Lissajous curve tracer.
 * x = A * cos(a*t + delta(time))
 * z = B * sin(b*t)
 * delta slowly changes, morphing the figure-8 into star patterns.
 */
public class LissajousRenderer {

    private static final float A      = 2.5f;    // x amplitude
    private static final float B      = 2.5f;    // z amplitude
    private static final int   FREQ_A = 3;       // x frequency
    private static final int   FREQ_B = 2;       // z frequency
    private static final int   TRAIL  = 500;
    private static final float T_STEP = 0.04f;   // t increment per tick
    private static final float DELTA_SPEED = 0.008f; // delta changes speed
    private static final int   FLOAT_CAP = TRAIL * 2 * 7 + 64;

    private static class ActiveEffect {
        final Vec3 pos; final long startTick;
        final Deque<float[]> trail = new ArrayDeque<>();
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
            float ox    = (float)(eff.pos.x - cam.x);
            float oy    = (float)(eff.pos.y + 0.07 - cam.y);
            float oz    = (float)(eff.pos.z - cam.z);
            float dt    = tick - eff.startTick;
            float t     = dt * T_STEP;
            float delta = dt * DELTA_SPEED;

            float dx = A * (float)Math.cos(FREQ_A * t + delta);
            float dz = B * (float)Math.sin(FREQ_B * t);

            eff.trail.addFirst(new float[]{dx, dz});
            if (eff.trail.size() > TRAIL) eff.trail.removeLast();

            if (eff.trail.size() < 2) continue;

            buf.clear();
            int vertCount = 0;
            int total = eff.trail.size();
            float[] pts = new float[total * 2];
            int idx = 0;
            for (float[] pt : eff.trail) { pts[idx++] = pt[0]; pts[idx++] = pt[1]; }

            for (int i = 0; i < total - 1; i++) {
                float frac = 1.0f - (float) i / total;
                float alpha = frac * frac * 0.9f;
                // Colour: magenta → cyan gradient along trail
                float r = 0.8f * (1.0f - frac) + 0.1f;
                float g = frac * 0.5f;
                float b = 0.6f + frac * 0.4f;

                buf.put(ox + pts[i*2]).put(oy).put(oz + pts[i*2+1]).put(r).put(g).put(b).put(alpha);
                buf.put(ox + pts[(i+1)*2]).put(oy).put(oz + pts[(i+1)*2+1]).put(r).put(g).put(b).put(alpha);
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
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                                 GL11.GL_ONE,        GL11.GL_ONE_MINUS_SRC_ALPHA);
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
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[Lissajous] Shader load failed", e); return; }
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
