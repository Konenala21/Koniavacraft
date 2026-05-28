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
 * Renders a layered magic circle in the XZ plane.
 *
 * Structure (outer → inner):
 *   Double outer ring
 *   Outer rune band (20 glyphs)
 *   {9/4} nonagram (9-pointed star)
 *   Middle separating ring
 *   Inner rune band (12 glyphs)
 *   {5/2} pentagram + regular pentagon
 *   Three tight inner rings
 *   Bright center glow
 */
public class MagicCircleRenderer {

    // ── Radii ─────────────────────────────────────────────────────────────────
    private static final float R_OUT1    = 3.50f;
    private static final float R_OUT2    = 3.30f;
    private static final float R_RUNE1   = 3.05f;  // outer glyph ring center
    private static final float R_NONA    = 2.72f;  // {9/4} nonagram vertices
    private static final float R_MID     = 2.05f;  // middle ring
    private static final float R_RUNE2   = 1.78f;  // inner glyph ring center
    private static final float R_PENTA   = 1.48f;  // pentagram + pentagon vertices
    private static final float R_IN1     = 0.90f;
    private static final float R_IN2     = 0.66f;
    private static final float R_IN3     = 0.44f;
    private static final float R_CORE    = 0.18f;

    // ── Rotation speeds (rad/tick) ────────────────────────────────────────────
    private static final float SPD_OUTER =  0.0040f;   // outer rings + runes1: slow CW
    private static final float SPD_NONA  = -0.0080f;   // nonagram: medium CCW
    private static final float SPD_MID   =  0.0000f;   // mid ring: static
    private static final float SPD_RUNE2 =  0.0055f;   // inner runes: slow CW
    private static final float SPD_PENTA = -0.0160f;   // pentagram: fast CCW
    private static final float SPD_INNER =  0.0110f;   // inner rings: medium CW
    private static final float SPD_CORE  = -0.0030f;   // core: very slow CCW

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final float[] GOLD    = {1.00f, 0.90f, 0.48f};
    private static final float[] BRIGHT  = {1.00f, 0.97f, 0.82f};
    private static final float[] DIM     = {0.85f, 0.72f, 0.32f};
    private static final float[] GLOW    = {1.00f, 1.00f, 0.94f};

    // ── Glyph templates ───────────────────────────────────────────────────────
    // Each entry: array of {x0, z0, x1, z1} segments.
    // Coordinates in glyph-local space:  z+ = radially outward ("top"),
    //   x+ = tangential ("right").  Scale 1.0 → glyph is 1 block tall.
    private static final float[][][] GLYPHS = {
        // 0: vertical staff + two right branches
        {{0,-0.50f, 0,0.50f}, {0, 0.28f, 0.38f, 0.06f}, {0,-0.08f, 0.38f,-0.30f}},
        // 1: Y-fork (like Algiz)
        {{0,-0.50f, 0, 0.05f}, {0, 0.05f,-0.32f, 0.50f}, {0, 0.05f, 0.32f, 0.50f}},
        // 2: up-arrow on staff
        {{0,-0.50f, 0, 0.50f}, {0, 0.50f,-0.30f, 0.18f}, {0, 0.50f, 0.30f, 0.18f}},
        // 3: double horizontal cross (like Nauthiz)
        {{0,-0.50f, 0, 0.50f}, {-0.30f, 0.18f, 0.30f, 0.18f}, {-0.22f,-0.18f, 0.22f,-0.18f}},
        // 4: Z-stroke
        {{-0.28f, 0.50f, 0.28f, 0.50f}, {0.28f, 0.50f,-0.28f,-0.50f}, {-0.28f,-0.50f, 0.28f,-0.50f}},
        // 5: P-like (staff + right loop approx)
        {{0,-0.50f, 0, 0.50f}, {0, 0.50f, 0.36f, 0.22f}, {0.36f, 0.22f, 0.36f,-0.02f}, {0.36f,-0.02f, 0, 0.10f}},
        // 6: diagonal cross (like Gebo)
        {{-0.30f,-0.50f, 0.30f, 0.50f}, {0.30f,-0.50f,-0.30f, 0.50f}},
        // 7: T with drop
        {{0,-0.50f, 0, 0.50f}, {-0.34f, 0.50f, 0.34f, 0.50f}, {0, 0.00f, 0.26f,-0.22f}},
    };

    private static final int OUTER_GLYPH_COUNT = 20;
    private static final float OUTER_GLYPH_SCALE = 0.17f;
    private static final int INNER_GLYPH_COUNT = 12;
    private static final float INNER_GLYPH_SCALE = 0.13f;

    // ── Effect state ──────────────────────────────────────────────────────────
    private static class ActiveEffect {
        final Vec3 pos;
        final long startTick;
        ActiveEffect(Vec3 pos, long startTick) { this.pos = pos; this.startTick = startTick; }
    }

    private static final List<ActiveEffect> effects = new ArrayList<>();

    // ── GL ────────────────────────────────────────────────────────────────────
    private static int  prog = -1, vao = -1, vbo = -1;
    private static int  locProj, locMV;
    private static boolean initialized = false;
    private static FloatBuffer buf = null;
    private static final int FLOAT_CAP = 30000;

    // ── Public API ────────────────────────────────────────────────────────────

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

        if (!initialized) { doInit(); if (!initialized) return; }

        Vec3  cam     = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float tick    = mc.level.getGameTime() + partial;

        int     prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        float[] proj = new float[16], mv = new float[16];
        event.getProjectionMatrix().get(proj);
        event.getModelViewMatrix().get(mv);

        GL20.glUseProgram(prog);
        GL20.glUniformMatrix4fv(locProj, false, proj);
        GL20.glUniformMatrix4fv(locMV,   false, mv);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        for (ActiveEffect eff : effects) {
            float ox = (float)(eff.pos.x - cam.x);
            float oy = (float)(eff.pos.y + 0.05 - cam.y);
            float oz = (float)(eff.pos.z - cam.z);
            float dt = tick - eff.startTick;

            float rOuter = SPD_OUTER * dt;
            float rNona  = SPD_NONA  * dt;
            float rRune2 = SPD_RUNE2 * dt;
            float rPenta = SPD_PENTA * dt;
            float rInner = SPD_INNER * dt;
            float rCore  = SPD_CORE  * dt;

            buf.clear();

            // ── 1. Double outer ring ──────────────────────────────────────────
            addRing(ox, oy, oz, R_OUT1, 80, BRIGHT, 0.85f);
            addRing(ox, oy, oz, R_OUT2, 80, BRIGHT, 0.70f);

            // ── 2. Outer rune band (20 glyphs, rotating with outer ring) ──────
            for (int i = 0; i < OUTER_GLYPH_COUNT; i++) {
                float angle = i * (float)(2 * Math.PI) / OUTER_GLYPH_COUNT + rOuter;
                int   type  = i % GLYPHS.length;
                addGlyph(ox, oy, oz, R_RUNE1, angle, OUTER_GLYPH_SCALE, type, BRIGHT, 0.80f);
            }

            // ── 3. {9/4} nonagram ─────────────────────────────────────────────
            addStarPolygon(ox, oy, oz, R_NONA, 9, 4, rNona, BRIGHT, 0.88f);

            // Small node circles at each nonagram vertex
            for (int i = 0; i < 9; i++) {
                float a  = i * (float)(2 * Math.PI) / 9 + rNona - (float)(Math.PI / 2);
                float vx = ox + R_NONA * (float)Math.cos(a);
                float vz = oz + R_NONA * (float)Math.sin(a);
                addRing(vx, oy, vz, 0.10f, 12, GOLD, 0.65f);
            }

            // ── 4. Middle separating ring ─────────────────────────────────────
            addRing(ox, oy, oz, R_MID, 72, GOLD, 0.72f);
            // Tick marks on mid ring (aligned with nonagram vertices)
            for (int i = 0; i < 9; i++) {
                float a   = i * (float)(2 * Math.PI) / 9 + rNona;
                float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
                addSegment(
                        ox + (R_MID - 0.12f) * cos, oy, oz + (R_MID - 0.12f) * sin,
                        ox + (R_MID + 0.12f) * cos, oy, oz + (R_MID + 0.12f) * sin,
                        GOLD, 0.60f
                );
            }

            // ── 5. Inner rune band (12 glyphs) ────────────────────────────────
            for (int i = 0; i < INNER_GLYPH_COUNT; i++) {
                float angle = i * (float)(2 * Math.PI) / INNER_GLYPH_COUNT + rRune2;
                int   type  = (i * 3 + 2) % GLYPHS.length;
                addGlyph(ox, oy, oz, R_RUNE2, angle, INNER_GLYPH_SCALE, type, BRIGHT, 0.78f);
            }

            // ── 6. {5/2} pentagram ────────────────────────────────────────────
            addStarPolygon(ox, oy, oz, R_PENTA, 5, 2, rPenta, BRIGHT, 0.90f);

            // Regular pentagon (connecting same 5 vertices consecutively)
            addPolygon(ox, oy, oz, R_PENTA, 5, rPenta, DIM, 0.55f);

            // Small node circles at pentagon/pentagram vertices
            for (int i = 0; i < 5; i++) {
                float a  = i * (float)(2 * Math.PI) / 5 + rPenta - (float)(Math.PI / 2);
                float vx = ox + R_PENTA * (float)Math.cos(a);
                float vz = oz + R_PENTA * (float)Math.sin(a);
                addRing(vx, oy, vz, 0.09f, 12, BRIGHT, 0.72f);
            }

            // ── 7. Three tight inner rings ────────────────────────────────────
            addRing(ox, oy, oz, R_IN1, 60, GOLD, 0.78f);

            // Small radial ticks at pentagram vertex angles on inner ring
            for (int i = 0; i < 5; i++) {
                float a   = i * (float)(2 * Math.PI) / 5 + rPenta;
                float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
                addSegment(
                        ox + (R_IN1 - 0.10f) * cos, oy, oz + (R_IN1 - 0.10f) * sin,
                        ox + (R_IN1 + 0.10f) * cos, oy, oz + (R_IN1 + 0.10f) * sin,
                        BRIGHT, 0.65f
                );
            }

            addRing(ox, oy, oz, R_IN2, 48, GOLD,  0.72f);
            addRing(ox, oy, oz, R_IN3, 36, GOLD,  0.68f);
            addRing(ox, oy, oz, R_IN3 * 0.72f, 24, DIM, 0.45f);

            // ── 8. Center glow core ───────────────────────────────────────────
            // Multiple bright rings growing outward from center (bloom simulation)
            addRing(ox, oy, oz, R_CORE,        16, GLOW, 1.00f);
            addRing(ox, oy, oz, R_CORE * 1.6f, 16, GLOW, 0.85f);
            addRing(ox, oy, oz, R_CORE * 2.4f, 20, GLOW, 0.65f);

            // Cross at center
            for (int i = 0; i < 4; i++) {
                float a   = i * (float)(Math.PI / 4) + rCore;
                float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
                addSegment(
                        ox, oy, oz,
                        ox + R_CORE * 2.5f * cos, oy, oz + R_CORE * 2.5f * sin,
                        GLOW, 0.80f
                );
            }

            buf.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STREAM_DRAW);
            GL11.glDrawArrays(GL11.GL_LINES, 0, buf.limit() / 7);
        }

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL20.glUseProgram(prevProg);
        if (wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend)  GL11.glEnable(GL11.GL_BLEND);     else GL11.glDisable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                                 GL11.GL_ONE,        GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void release() {
        if (prog != -1) { GL20.glDeleteProgram(prog);     prog = -1; }
        if (vbo  != -1) { GL15.glDeleteBuffers(vbo);      vbo  = -1; }
        if (vao  != -1) { GL30.glDeleteVertexArrays(vao); vao  = -1; }
        if (buf  != null) { MemoryUtil.memFree(buf);       buf  = null; }
        initialized = false;
        effects.clear();
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    // Perf: 預算單位圓 cos/sin 表（key=segs，value=[c0,s0,c1,s1,...]）
    // 之前每幀對每個 ring 跑 segs×2 sin/cos call（總共 ~1240 calls / effect / frame），
    // 改成查表 0 sin/cos call，省 ~60µs / effect / frame。
    private static final java.util.Map<Integer, float[]> RING_TABLES = new java.util.HashMap<>();

    private static float[] ringTable(int segs) {
        float[] t = RING_TABLES.get(segs);
        if (t == null) {
            t = new float[segs * 2];
            for (int i = 0; i < segs; i++) {
                float a = i * (float)(2 * Math.PI) / segs;
                t[i * 2]     = (float) Math.cos(a);
                t[i * 2 + 1] = (float) Math.sin(a);
            }
            RING_TABLES.put(segs, t);
        }
        return t;
    }

    /** Circle as GL_LINES pairs. */
    private static void addRing(float cx, float y, float cz, float r,
                                int segs, float[] col, float alpha) {
        float[] tab = ringTable(segs);
        for (int i = 0; i < segs; i++) {
            int j = (i + 1) % segs;
            putV(cx + r * tab[i * 2], y, cz + r * tab[i * 2 + 1], col, alpha);
            putV(cx + r * tab[j * 2], y, cz + r * tab[j * 2 + 1], col, alpha);
        }
    }

    /** Single line segment (2 vertices). */
    private static void addSegment(float x0, float y0, float z0,
                                   float x1, float y1, float z1,
                                   float[] col, float alpha) {
        putV(x0, y0, z0, col, alpha);
        putV(x1, y1, z1, col, alpha);
    }

    /** Regular {n/k} star polygon. */
    private static void addStarPolygon(float cx, float y, float cz, float r,
                                       int n, int k, float rot,
                                       float[] col, float alpha) {
        // Perf: 改用單位圓表 + 旋轉矩陣（cos/sin per polygon 而非 per vertex）
        float[] tab = ringTable(n);
        float rOff = rot - (float)(Math.PI / 2);
        float cosR = (float) Math.cos(rOff), sinR = (float) Math.sin(rOff);
        float[] vx = new float[n], vz = new float[n];
        for (int i = 0; i < n; i++) {
            float ct = tab[i * 2], st = tab[i * 2 + 1];
            vx[i] = cx + r * (ct * cosR - st * sinR);
            vz[i] = cz + r * (st * cosR + ct * sinR);
        }
        int comps = gcd(n, k);
        for (int s = 0; s < comps; s++) {
            int cur = s;
            do {
                int nxt = (cur + k) % n;
                putV(vx[cur], y, vz[cur], col, alpha);
                putV(vx[nxt], y, vz[nxt], col, alpha);
                cur = nxt;
            } while (cur != s);
        }
    }

    /** Regular n-gon (polygon, not star). */
    private static void addPolygon(float cx, float y, float cz, float r,
                                   int n, float rot, float[] col, float alpha) {
        float[] tab = ringTable(n);
        float rOff = rot - (float)(Math.PI / 2);
        float cosR = (float) Math.cos(rOff), sinR = (float) Math.sin(rOff);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ct0 = tab[i * 2], st0 = tab[i * 2 + 1];
            float ct1 = tab[j * 2], st1 = tab[j * 2 + 1];
            putV(cx + r * (ct0 * cosR - st0 * sinR), y, cz + r * (st0 * cosR + ct0 * sinR), col, alpha);
            putV(cx + r * (ct1 * cosR - st1 * sinR), y, cz + r * (st1 * cosR + ct1 * sinR), col, alpha);
        }
    }

    /**
     * Place and orient a rune glyph on a ring.
     * The glyph's +z axis points radially outward from the center; +x is tangential.
     *
     * @param ringR  ring radius where glyph is centered
     * @param angle  polar angle of glyph center on the ring
     * @param scale  glyph height in blocks
     * @param type   index into GLYPHS array
     */
    private static void addGlyph(float cx, float y, float cz,
                                 float ringR, float angle, float scale,
                                 int type, float[] col, float alpha) {
        float gx = cx + ringR * (float)Math.cos(angle);
        float gz = cz + ringR * (float)Math.sin(angle);

        // Local basis vectors (in XZ plane)
        // radial (outward, glyph "up"):    (cos a, sin a)
        // tangential (glyph "right"):      (-sin a, cos a)
        float radX =  (float)Math.cos(angle), radZ =  (float)Math.sin(angle);
        float tanX = -(float)Math.sin(angle), tanZ =  (float)Math.cos(angle);

        float[][] tpl = GLYPHS[type % GLYPHS.length];
        for (float[] seg : tpl) {
            // seg = {lx0, lz0, lx1, lz1} in local space
            float lx0 = seg[0], lz0 = seg[1], lx1 = seg[2], lz1 = seg[3];
            // Transform to world XZ
            float wx0 = gx + scale * (lx0 * tanX + lz0 * radX);
            float wz0 = gz + scale * (lx0 * tanZ + lz0 * radZ);
            float wx1 = gx + scale * (lx1 * tanX + lz1 * radX);
            float wz1 = gz + scale * (lx1 * tanZ + lz1 * radZ);
            putV(wx0, y, wz0, col, alpha);
            putV(wx1, y, wz1, col, alpha);
        }
    }

    private static void putV(float x, float y, float z, float[] col, float a) {
        buf.put(x).put(y).put(z).put(col[0]).put(col[1]).put(col[2]).put(a);
    }

    private static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }

    // ── GL init ───────────────────────────────────────────────────────────────

    private static void doInit() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = readSrc(rm, "shaders/fourier_curve.vsh");
            String frag = readSrc(rm, "shaders/fourier_curve.fsh");
            prog = buildProg(vert, frag);
            if (prog == -1) return;
            GL20.glUseProgram(prog);
            locProj = GL20.glGetUniformLocation(prog, "ProjMat");
            locMV   = GL20.glGetUniformLocation(prog, "ModelViewMat");
            GL20.glUseProgram(0);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[MagicCircle] Shader load failed", e);
            return;
        }

        buf = MemoryUtil.memAllocFloat(FLOAT_CAP);
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) FLOAT_CAP * Float.BYTES, GL15.GL_STREAM_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 28, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 28, 12);
        GL20.glEnableVertexAttribArray(1);
        GL30.glBindVertexArray(0);
        initialized = true;
        KoniavacraftMod.LOGGER.info("[MagicCircle] Initialized");
    }

    private static String readSrc(ResourceManager rm, String path) throws IOException {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int buildProg(String vert, String frag) {
        int v = compileShader(GL20.GL_VERTEX_SHADER, vert);
        int f = compileShader(GL20.GL_FRAGMENT_SHADER, frag);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v); GL20.glAttachShader(p, f);
        GL20.glBindAttribLocation(p, 0, "Position");
        GL20.glBindAttribLocation(p, 1, "Color");
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(v); GL20.glDeleteShader(f);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[MagicCircle] Link error: {}", GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p); return -1;
        }
        return p;
    }

    private static int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src); GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[MagicCircle] Compile: {}", GL20.glGetShaderInfoLog(id));
        return id;
    }
}
