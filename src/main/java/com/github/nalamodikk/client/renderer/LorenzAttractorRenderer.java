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
 * Lorenz chaotic attractor (sigma=10, rho=28, beta=8/3).
 * Pre-computed trajectory rendered as a slowly rotating 3D trail.
 */
public class LorenzAttractorRenderer {

    private static final int   STEPS     = 4000;
    private static final float DT        = 0.008f;
    private static final float SCALE     = 0.10f;
    private static final float ROT_SPEED = 0.005f;
    private static final int   FLOAT_CAP = STEPS * 2 * 7 + 64;

    private static class ActiveEffect {
        final Vec3   pos;
        final long   startTick;
        final float[] px, py, pz;   // pre-computed trajectory in local space

        ActiveEffect(Vec3 p, long t) {
            pos = p; startTick = t;
            float sigma = 10f, rho = 28f, beta = 8f / 3f;
            float x = -1f, y = 0f, z = 25f;
            px = new float[STEPS]; py = new float[STEPS]; pz = new float[STEPS];
            for (int i = 0; i < STEPS; i++) {
                // store scaled, centered: center of attractor is near (0,0,25)
                px[i] = x * SCALE;
                py[i] = (z - 25f) * SCALE;  // Y = Lorenz Z - 25, scaled
                pz[i] = y * SCALE;
                float dx = sigma * (y - x) * DT;
                float dy = (x * (rho - z) - y) * DT;
                float dz = (x * y - beta * z) * DT;
                x += dx; y += dy; z += dz;
            }
        }
    }

    private static final List<ActiveEffect> effects = new ArrayList<>();
    private static int  prog = -1, vao = -1, vbo = -1, locProj, locMV;
    private static boolean init = false;
    private static FloatBuffer buf;

    public static void spawnEffect(Vec3 pos, long tick) {
        effects.removeIf(e -> e.pos.distanceToSqr(pos) < 1.0);
        if (effects.size() >= 2) effects.remove(0);
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
            float ox  = (float)(eff.pos.x - cam.x);
            float oy  = (float)(eff.pos.y + 1.0 - cam.y);
            float oz  = (float)(eff.pos.z - cam.z);
            float rot = ROT_SPEED * (tick - eff.startTick);
            float cos = (float)Math.cos(rot), sin = (float)Math.sin(rot);

            buf.clear();
            int vertCount = 0;
            for (int i = 0; i < STEPS - 1; i++) {
                float frac = (float)i / STEPS;
                float alpha = 0.3f + 0.6f * frac;
                // Color: blue at start → red at end
                float r = frac, g = 0.2f + 0.3f * (float)Math.sin(frac * (float)Math.PI), b = 1f - frac;

                float rx0 = eff.px[i] * cos - eff.pz[i] * sin;
                float rz0 = eff.px[i] * sin + eff.pz[i] * cos;
                float rx1 = eff.px[i+1] * cos - eff.pz[i+1] * sin;
                float rz1 = eff.px[i+1] * sin + eff.pz[i+1] * cos;

                buf.put(ox+rx0).put(oy+eff.py[i]).put(oz+rz0).put(r).put(g).put(b).put(alpha);
                buf.put(ox+rx1).put(oy+eff.py[i+1]).put(oz+rz1).put(r).put(g).put(b).put(alpha);
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
        init = false; effects.clear();
    }

    private static void doInit() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            prog = buildProg(read(rm, "shaders/fourier_curve.vsh"), read(rm, "shaders/fourier_curve.fsh"));
            if (prog == -1) return;
            GL20.glUseProgram(prog); locProj = GL20.glGetUniformLocation(prog, "ProjMat"); locMV = GL20.glGetUniformLocation(prog, "ModelViewMat"); GL20.glUseProgram(0);
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[Lorenz] Shader load failed", e); return; }
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
