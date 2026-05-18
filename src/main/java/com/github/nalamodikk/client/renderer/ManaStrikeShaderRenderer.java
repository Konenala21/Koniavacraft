package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.MIRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ManaStrikeShaderRenderer {

    private record ActiveEffect(Vec3 pos, long startTick) {}

    // ── Shader effect ─────────────────────────────────────────────────────────
    private static final List<ActiveEffect> activeEffects = new ArrayList<>();

    private static final float TOTAL_SECS  = 24.0f;
    private static final int   TOTAL_TICKS = (int)(TOTAL_SECS * 20);

    // ── Ring effect ───────────────────────────────────────────────────────────
    private static final List<ActiveEffect> ringEffects = new ArrayList<>();

    private static final int   RING_COUNT      = 7;
    private static final float RING_DELAY      = 5f;
    private static final float RING_SPEED      = 0.32f;
    private static final float RING_MAX_R      = 15f;
    private static final float TOTAL_RING_LIFE = RING_DELAY * (RING_COUNT - 1) + RING_MAX_R / RING_SPEED;
    private static final int   SEGS            = 48;

    private static final float[] COS_TABLE = new float[SEGS];
    private static final float[] SIN_TABLE = new float[SEGS];
    static {
        for (int i = 0; i < SEGS; i++) {
            double a = i * 2 * Math.PI / SEGS;
            COS_TABLE[i] = (float) Math.cos(a);
            SIN_TABLE[i] = (float) Math.sin(a);
        }
    }

    // ── GL objects ────────────────────────────────────────────────────────────
    private static int programId  = -1;
    private static int vaoId      = -1;
    private static int vboId      = -1;
    private static int copyTexId  = -1;
    private static int copyWidth  = -1;
    private static int copyHeight = -1;

    private static int locDiffuse, locDepth;
    private static int locBlockPos, locCamPos;
    private static int locInvProj, locInvView;
    private static int locTime,    locAlpha;

    private static boolean initialized = false;
    private static boolean initFailed  = false;

    // ── Public API ────────────────────────────────────────────────────────────

    public static void spawnEffect(Vec3 worldPos, long gameTick) {
        activeEffects.add(new ActiveEffect(worldPos, gameTick));
        ringEffects.add(new ActiveEffect(worldPos, gameTick));
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            renderRings(event);
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Expire finished effects
        long now = mc.level.getGameTime();
        activeEffects.removeIf(e -> now - e.startTick() > TOTAL_TICKS + 40);
        if (activeEffects.isEmpty()) return;

        if (!initialized) {
            if (initFailed) { activeEffects.clear(); return; }
            init();
            if (programId == -1) { initFailed = true; activeEffects.clear(); return; }
        }

        var mainTarget = mc.getMainRenderTarget();
        var camPos     = event.getCamera().getPosition();
        int fbW        = mainTarget.width;
        int fbH        = mainTarget.height;

        ensureCopyTex(fbW, fbH);
        int prevReadFBO = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTexId);
        GL30.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fbW, fbH);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFBO);

        int     prevProg  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDepth  = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBlend  = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        GL20.glUseProgram(programId);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTexId);
        GL20.glUniform1i(locDiffuse, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainTarget.getDepthTextureId());
        GL20.glUniform1i(locDepth, 1);

        GL20.glUniform3f(locCamPos, (float)camPos.x, (float)camPos.y, (float)camPos.z);
        float[] invProj = new float[16];
        float[] invView = new float[16];
        new Matrix4f(event.getProjectionMatrix()).invert().get(invProj);
        new Matrix4f(event.getModelViewMatrix()).invert().get(invView);
        GL20.glUniformMatrix4fv(locInvProj, false, invProj);
        GL20.glUniformMatrix4fv(locInvView, false, invView);

        float partial  = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float gameTick = now + partial;

        GL30.glBindVertexArray(vaoId);
        Iterator<ActiveEffect> it = activeEffects.iterator();
        while (it.hasNext()) {
            ActiveEffect eff = it.next();
            float iTime = (gameTick - eff.startTick()) / 20.0f;
            if (iTime > TOTAL_SECS + 1.0f) { it.remove(); continue; }

            float iAlpha = calcAlpha(iTime);
            GL20.glUniform3f(locBlockPos,
                    (float)eff.pos().x, (float)eff.pos().y, (float)eff.pos().z);
            GL20.glUniform1f(locTime,  iTime);
            GL20.glUniform1f(locAlpha, iAlpha);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        }
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend) GL11.glEnable(GL11.GL_BLEND);      else GL11.glDisable(GL11.GL_BLEND);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    private static void renderRings(RenderLevelStageEvent event) {
        if (ringEffects.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float partial  = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float gameTick = mc.level.getGameTime() + partial;

        Vec3 cam     = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        var buf      = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(MIRenderTypes.solarGlow());

        Iterator<ActiveEffect> it = ringEffects.iterator();
        while (it.hasNext()) {
            ActiveEffect eff = it.next();
            float elapsed = gameTick - eff.startTick();
            if (elapsed > TOTAL_RING_LIFE) { it.remove(); continue; }

            ps.pushPose();
            ps.translate(eff.pos().x - cam.x, eff.pos().y - cam.y, eff.pos().z - cam.z);
            Matrix4f mat = ps.last().pose();

            for (int i = 0; i < RING_COUNT; i++) {
                float ringElapsed = elapsed - i * RING_DELAY;
                if (ringElapsed <= 0) continue;
                float radius = ringElapsed * RING_SPEED;
                if (radius > RING_MAX_R) continue;
                float progress  = radius / RING_MAX_R;
                float fadeOut   = 1f - progress;
                float thickness = 0.12f + fadeOut * 0.55f;
                drawRing(mat, vc, radius + thickness * 1.6f, thickness * 1.6f, 60,  140, (int)(30  * fadeOut));
                drawRing(mat, vc, radius,                    thickness,          100, 190, (int)(140 * fadeOut));
                drawRing(mat, vc, radius - thickness * 0.15f, thickness * 0.25f, 210, 235, (int)(200 * fadeOut));
            }
            ps.popPose();
        }
        buf.endBatch(MIRenderTypes.solarGlow());
    }

    private static void drawRing(Matrix4f mat, VertexConsumer vc,
                                  float radius, float halfWidth,
                                  int r, int g, int a) {
        if (a <= 0 || radius <= 0) return;
        float inner = Math.max(0, radius - halfWidth);
        float outer = radius + halfWidth;
        for (int i = 0; i < SEGS; i++) {
            int j = (i + 1 == SEGS) ? 0 : i + 1;
            vc.addVertex(mat, inner * COS_TABLE[i], 0f, inner * SIN_TABLE[i]).setColor(r, g, 255, 0);
            vc.addVertex(mat, outer * COS_TABLE[i], 0f, outer * SIN_TABLE[i]).setColor(r, g, 255, a);
            vc.addVertex(mat, outer * COS_TABLE[j], 0f, outer * SIN_TABLE[j]).setColor(r, g, 255, a);
            vc.addVertex(mat, inner * COS_TABLE[j], 0f, inner * SIN_TABLE[j]).setColor(r, g, 255, 0);
        }
    }

    /** Discard compiled shaders; next render call will re-compile from the new resource pack. */
    public static void reload() {
        initFailed = false;
        release();
    }

    // ── Init / Release ────────────────────────────────────────────────────────

    private static void init() {
        if (initialized) release();

        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = readResource(rm, "shaders/mana_strike.vsh");
            String frag = readResource(rm, "shaders/mana_strike.fsh");
            programId = buildProgram(vert, frag);
            GL20.glUseProgram(programId);
            locDiffuse  = GL20.glGetUniformLocation(programId, "DiffuseSampler");
            locDepth    = GL20.glGetUniformLocation(programId, "DepthSampler");
            locBlockPos = GL20.glGetUniformLocation(programId, "BlockPosition");
            locCamPos   = GL20.glGetUniformLocation(programId, "CameraPosition");
            locInvProj  = GL20.glGetUniformLocation(programId, "InvProjMat");
            locInvView  = GL20.glGetUniformLocation(programId, "InvViewMat");
            locTime     = GL20.glGetUniformLocation(programId, "iTime");
            locAlpha    = GL20.glGetUniformLocation(programId, "iAlpha");
            GL20.glUseProgram(0);
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            float[] quad = {-1f,-1f, 1f,-1f, 1f,1f, -1f,1f};
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
            GL20.glEnableVertexAttribArray(0);
            GL30.glBindVertexArray(0);
            initialized = true;
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[ManaStrike] Shader load failed", e);
            if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
            initFailed = true;
        }
    }

    public static void release() {
        if (programId  != -1) { GL20.glDeleteProgram(programId);    programId  = -1; }
        if (vboId      != -1) { GL15.glDeleteBuffers(vboId);        vboId      = -1; }
        if (vaoId      != -1) { GL30.glDeleteVertexArrays(vaoId);   vaoId      = -1; }
        if (copyTexId  != -1) { GL11.glDeleteTextures(copyTexId);   copyTexId  = -1; }
        copyWidth = copyHeight = -1;
        initialized = false;
        activeEffects.clear();
        ringEffects.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static float calcAlpha(float t) {
        if (t < 0.6f)               return t / 0.6f;
        if (t < TOTAL_SECS - 2.0f) return 1.0f;
        return 1.0f - (t - (TOTAL_SECS - 2.0f)) / 2.0f;
    }

    private static void ensureCopyTex(int w, int h) {
        if (copyTexId == -1) {
            copyTexId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTexId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            copyWidth = copyHeight = -1;
        }
        if (w != copyWidth || h != copyHeight) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTexId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            copyWidth = w; copyHeight = h;
        }
    }

    private static String readResource(ResourceManager rm, String path) throws IOException {
        var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int buildProgram(String vertSrc, String fragSrc) {
        int vert = compileShader(GL20.GL_VERTEX_SHADER,   vertSrc);
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, fragSrc);
        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vert);
        GL20.glAttachShader(prog, frag);
        GL20.glLinkProgram(prog);
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[ManaStrike] Link: {}", GL20.glGetProgramInfoLog(prog));
            GL20.glDeleteProgram(prog);
            return -1;
        }
        return prog;
    }

    private static int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[ManaStrike] Compile ({}): {}",
                    type, GL20.glGetShaderInfoLog(id));
        return id;
    }
}
