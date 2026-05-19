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
 * Animated cubic Bezier curve in the XZ plane.
 * P0 and P3 are fixed endpoints; P1 and P2 float with sin/cos.
 * Shows control polygon (dim) and curve (bright).
 */
public class BezierCurveRenderer {

    private static final int   CURVE_SEGS = 200;
    private static final float ANIM_SPEED = 0.022f;
    // curve verts + 3 polygon lines + 4 small control-point circles (16 segs each)
    private static final int   FLOAT_CAP  = (CURVE_SEGS + 3 + 4 * 16) * 2 * 7 + 128;

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
            float ox = (float)(eff.pos.x - cam.x);
            float oy = (float)(eff.pos.y + 0.05 - cam.y);
            float oz = (float)(eff.pos.z - cam.z);
            float a  = ANIM_SPEED * (tick - eff.startTick);

            // Control points in XZ plane
            float p0x = -2.2f, p0z = 0f;
            float p1x =  (float)Math.sin(a * 0.7f) * 2.0f, p1z = -1.8f + (float)Math.cos(a * 0.5f);
            float p2x = -(float)Math.sin(a * 0.9f + 1) * 2.0f, p2z = 1.8f + (float)Math.sin(a * 0.6f);
            float p3x =  2.2f, p3z = 0f;

            buf.clear();
            int vertCount = 0;

            // Control polygon (dim gray)
            float[][] poly = {{p0x,p0z},{p1x,p1z},{p2x,p2z},{p3x,p3z}};
            for (int i = 0; i < 3; i++) {
                buf.put(ox+poly[i][0]).put(oy).put(oz+poly[i][1]).put(0.4f).put(0.4f).put(0.4f).put(0.5f);
                buf.put(ox+poly[i+1][0]).put(oy).put(oz+poly[i+1][1]).put(0.4f).put(0.4f).put(0.4f).put(0.5f);
                vertCount += 2;
            }

            // Control point markers (small circles)
            float[][] cpColors = {{1,.4f,.1f},{.2f,1,.4f},{.2f,.4f,1},{1,.9f,.2f}};
            for (int ci = 0; ci < 4; ci++) {
                float cpx = poly[ci][0], cpz = poly[ci][1];
                float[] cc = cpColors[ci];
                for (int s = 0; s < 16; s++) {
                    float sa0 = s       * (float)(2*Math.PI)/16, sa1 = (s+1)*(float)(2*Math.PI)/16;
                    float r = 0.10f;
                    buf.put(ox+cpx+r*(float)Math.cos(sa0)).put(oy).put(oz+cpz+r*(float)Math.sin(sa0)).put(cc[0]).put(cc[1]).put(cc[2]).put(0.9f);
                    buf.put(ox+cpx+r*(float)Math.cos(sa1)).put(oy).put(oz+cpz+r*(float)Math.sin(sa1)).put(cc[0]).put(cc[1]).put(cc[2]).put(0.9f);
                    vertCount += 2;
                }
            }

            // Cubic Bezier curve (rainbow)
            for (int i = 0; i < CURVE_SEGS; i++) {
                float t0 = (float)i       / CURVE_SEGS;
                float t1 = (float)(i + 1) / CURVE_SEGS;
                float[] c0 = hue(t0), c1 = hue(t1);
                float[] b0 = cubic(p0x,p0z, p1x,p1z, p2x,p2z, p3x,p3z, t0);
                float[] b1 = cubic(p0x,p0z, p1x,p1z, p2x,p2z, p3x,p3z, t1);
                buf.put(ox+b0[0]).put(oy).put(oz+b0[1]).put(c0[0]).put(c0[1]).put(c0[2]).put(0.95f);
                buf.put(ox+b1[0]).put(oy).put(oz+b1[1]).put(c1[0]).put(c1[1]).put(c1[2]).put(0.95f);
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

    private static float[] cubic(float ax, float az, float bx, float bz,
                                  float cx, float cz, float dx, float dz, float t) {
        float u = 1 - t;
        float x = u*u*u*ax + 3*u*u*t*bx + 3*u*t*t*cx + t*t*t*dx;
        float z = u*u*u*az + 3*u*u*t*bz + 3*u*t*t*cz + t*t*t*dz;
        return new float[]{x, z};
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
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[Bezier] Shader load failed", e); initFailed = true; return; }
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
