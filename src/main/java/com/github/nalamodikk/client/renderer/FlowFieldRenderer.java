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
 * Animated 2D flow field in the XZ plane.
 * Each cell shows an arrow aligned with the vector field v = (sin(z+t), cos(x+t)).
 */
public class FlowFieldRenderer {

    private static final int   GRID       = 11;
    private static final float SPACING    = 0.45f;
    private static final float ARROW_LEN  = 0.18f;
    private static final float ANIM_SPEED = 0.025f;
    private static final int   FLOAT_CAP  = GRID * GRID * 6 * 7 + 64;

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

        float t = ANIM_SPEED * (tick - 0);

        for (ActiveEffect eff : effects) {
            float ox = (float)(eff.pos.x - cam.x);
            float oy = (float)(eff.pos.y + 0.03 - cam.y);
            float oz = (float)(eff.pos.z - cam.z);
            float lt = ANIM_SPEED * (tick - eff.startTick);

            buf.clear();
            int vertCount = 0;
            float half = (GRID - 1) * SPACING / 2.0f;

            for (int xi = 0; xi < GRID; xi++) {
                for (int zi = 0; zi < GRID; zi++) {
                    float gx = -half + xi * SPACING;
                    float gz = -half + zi * SPACING;

                    // Field direction
                    float dx = (float)Math.sin(gz + lt);
                    float dz = (float)Math.cos(gx + lt);
                    float len = (float)Math.sqrt(dx*dx + dz*dz);
                    if (len < 1e-5f) continue;
                    dx /= len; dz /= len;

                    // Color by direction angle
                    float angle = (float)Math.atan2(dz, dx);
                    float[] c = hue((angle + (float)Math.PI) / (float)(2 * Math.PI));

                    // Arrow stem
                    float hLen = ARROW_LEN * 0.6f;
                    float sx = gx - dx * hLen, sz = gz - dz * hLen;
                    float ex = gx + dx * hLen, ez = gz + dz * hLen;
                    buf.put(ox+sx).put(oy).put(oz+sz).put(c[0]).put(c[1]).put(c[2]).put(0.7f);
                    buf.put(ox+ex).put(oy).put(oz+ez).put(c[0]).put(c[1]).put(c[2]).put(0.9f);
                    vertCount += 2;

                    // Arrowhead (two short lines at tip)
                    float perpX = -dz * ARROW_LEN * 0.25f;
                    float perpZ =  dx * ARROW_LEN * 0.25f;
                    float backX = ex - dx * ARROW_LEN * 0.3f;
                    float backZ = ez - dz * ARROW_LEN * 0.3f;
                    buf.put(ox+ex).put(oy).put(oz+ez).put(c[0]).put(c[1]).put(c[2]).put(0.9f);
                    buf.put(ox+backX+perpX).put(oy).put(oz+backZ+perpZ).put(c[0]).put(c[1]).put(c[2]).put(0.7f);
                    buf.put(ox+ex).put(oy).put(oz+ez).put(c[0]).put(c[1]).put(c[2]).put(0.9f);
                    buf.put(ox+backX-perpX).put(oy).put(oz+backZ-perpZ).put(c[0]).put(c[1]).put(c[2]).put(0.7f);
                    vertCount += 4;
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
            if (prog == -1) return;
            GL20.glUseProgram(prog); locProj = GL20.glGetUniformLocation(prog, "ProjMat"); locMV = GL20.glGetUniformLocation(prog, "ModelViewMat"); GL20.glUseProgram(0);
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[FlowField] Shader load failed", e); return; }
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
