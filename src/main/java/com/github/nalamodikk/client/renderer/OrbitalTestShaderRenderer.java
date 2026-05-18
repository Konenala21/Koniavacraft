package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
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

public class OrbitalTestShaderRenderer {

    private static final String COMMON = "shaders/orbital/common.glsl";

    public enum Mode {
        // 拆分後的 orbital 組合
        ORBITAL_CHARGE   (new String[]{COMMON, "shaders/orbital/stage_charge.fsh"},                                         2f),
        ORBITAL_SPHERE   (new String[]{COMMON, "shaders/orbital/sdf_sphere.glsl",    "shaders/orbital/stage_body.fsh"},    16f),
        ORBITAL_SATS     (new String[]{COMMON, "shaders/orbital/sdf_satellites.glsl","shaders/orbital/stage_body.fsh"},    16f),
        ORBITAL_FULL     (new String[]{COMMON, "shaders/orbital/sdf_full.glsl",      "shaders/orbital/stage_body.fsh"},    16f),
        ORBITAL_COLLAPSE (new String[]{COMMON, "shaders/orbital/sdf_collapse.glsl",  "shaders/orbital/stage_collapse.fsh"}, 8f),
        // 完整原版 orbital（含 charge + full + collapse）
        ORBITAL          (new String[]{"shaders/orbital_test.fsh"},                                                         36f),
        // 其他測試 shader
        SHIELD           (new String[]{"shaders/test_shield.fsh"},                    8f),
        TORUS            (new String[]{"shaders/test_torus.fsh"},                     8f),
        CYLINDER         (new String[]{"shaders/test_cylinder.fsh"},                 10f),
        CRYSTAL          (new String[]{"shaders/test_crystal.fsh"},                   8f);

        public final String[] fragPaths;
        public final float    totalSecs;
        Mode(String[] paths, float s) { fragPaths = paths; totalSecs = s; }
    }

    public static Mode currentMode = Mode.ORBITAL;

    public static void cycleMode(int delta) {
        Mode[] vals = Mode.values();
        currentMode = vals[Math.floorMod(currentMode.ordinal() + delta, vals.length)];
    }

    private record ActiveEffect(Vec3 pos, long startTick, float totalSecs) {}
    private static final List<ActiveEffect> activeEffects = new ArrayList<>();

    private static int programId  = -1;
    private static int vaoId      = -1;
    private static int vboId      = -1;
    private static int copyTexId  = -1;
    private static int copyWidth  = -1;
    private static int copyHeight = -1;

    private static int locDiffuse, locDepth, locBlockPos, locCamPos;
    private static int locInvProj, locInvView, locTime;

    private static boolean initialized = false;
    private static Mode    loadedMode  = null;

    public static void spawnEffect(Vec3 worldPos, long gameTick) {
        activeEffects.clear();
        if (loadedMode != currentMode) release();
        activeEffects.add(new ActiveEffect(worldPos, gameTick, currentMode.totalSecs));
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        activeEffects.removeIf(e -> (now - e.startTick()) / 20.0f > e.totalSecs() + 1.0f);
        if (activeEffects.isEmpty()) return;

        if (!initialized) {
            init();
            if (programId == -1) { activeEffects.clear(); return; }
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

        int     prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
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
            if (iTime > eff.totalSecs() + 1.0f) { it.remove(); continue; }

            GL20.glUniform3f(locBlockPos, (float)eff.pos().x, (float)eff.pos().y, (float)eff.pos().z);
            GL20.glUniform1f(locTime, iTime);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        }
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend) GL11.glEnable(GL11.GL_BLEND);      else GL11.glDisable(GL11.GL_BLEND);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public static void reload() { release(); }

    private static void init() {
        if (initialized) release();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = readResource(rm, "shaders/orbital_test.vsh");
            StringBuilder fragBuilder = new StringBuilder();
            for (String path : currentMode.fragPaths) {
                fragBuilder.append(readResource(rm, path)).append('\n');
            }
            programId = buildProgram(vert, fragBuilder.toString());
            GL20.glUseProgram(programId);
            locDiffuse  = GL20.glGetUniformLocation(programId, "DiffuseSampler");
            locDepth    = GL20.glGetUniformLocation(programId, "DepthSampler");
            locBlockPos = GL20.glGetUniformLocation(programId, "BlockPosition");
            locCamPos   = GL20.glGetUniformLocation(programId, "CameraPosition");
            locInvProj  = GL20.glGetUniformLocation(programId, "InvProjMat");
            locInvView  = GL20.glGetUniformLocation(programId, "InvViewMat");
            locTime     = GL20.glGetUniformLocation(programId, "iTime");
            GL20.glUseProgram(0);
            loadedMode = currentMode;
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[OrbitalTest] Shader load failed ({})", currentMode, e);
            programId = -1;
            return;
        }

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
    }

    public static void release() {
        if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
        if (vboId     != -1) { GL15.glDeleteBuffers(vboId);     vboId     = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId); vaoId    = -1; }
        if (copyTexId != -1) { GL11.glDeleteTextures(copyTexId); copyTexId = -1; }
        copyWidth = copyHeight = -1;
        initialized = false;
        loadedMode  = null;
        activeEffects.clear();
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
            KoniavacraftMod.LOGGER.error("[OrbitalTest] Link: {}", GL20.glGetProgramInfoLog(prog));
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
            KoniavacraftMod.LOGGER.error("[OrbitalTest] Compile ({}): {}", type, GL20.glGetShaderInfoLog(id));
        return id;
    }
}
