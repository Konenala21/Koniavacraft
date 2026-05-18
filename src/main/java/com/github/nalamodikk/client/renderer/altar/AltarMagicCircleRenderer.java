package com.github.nalamodikk.client.renderer.altar;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class AltarMagicCircleRenderer {

    private static final ResourceLocation MAGIC_CIRCLE_TEX =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/altar_magic_circle.png");

    private static int programId   = -1;
    private static int vaoId       = -1;
    private static int vboId       = -1;
    private static int locTex, locScreenSize, locProgress, locRotation, locAlpha, locCircleSize;
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        AltarUpgradeAnimManager.AnimState state = AltarUpgradeAnimManager.getActiveT6State();
        if (state == null) return;

        float tick    = state.tick();
        float alpha   = getMagicCircleAlpha(tick);
        float scale   = getMagicCircleScale(tick);
        if (alpha <= 0.001f || scale <= 0.001f) return;

        if (!initialized) {
            init();
            if (!initialized) return;
        }

        Minecraft mc   = Minecraft.getInstance();
        int width      = mc.getWindow().getWidth();
        int height     = mc.getWindow().getHeight();
        float progress = getMagicCircleProgress(tick);
        float rotation = getMagicCircleRotation(tick);

        int prevProg  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDep = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBl  = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,
                mc.getTextureManager().getTexture(MAGIC_CIRCLE_TEX).getId());

        GL20.glUseProgram(programId);
        GL20.glUniform1i(locTex, 0);
        GL20.glUniform2f(locScreenSize, width, height);
        GL20.glUniform1f(locProgress,   progress);
        GL20.glUniform1f(locRotation,   rotation);
        GL20.glUniform1f(locAlpha,      alpha);
        GL20.glUniform1f(locCircleSize, scale);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (wasDep) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (!wasBl) GL11.glDisable(GL11.GL_BLEND);
    }

    private static void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/altar_magic_circle.vsh");
            String frag = read(rm, "shaders/altar_magic_circle.fsh");
            int v = compile(GL20.GL_VERTEX_SHADER,   vert);
            int f = compile(GL20.GL_FRAGMENT_SHADER, frag);
            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, v);
            GL20.glAttachShader(programId, f);
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                KoniavacraftMod.LOGGER.error("[MagicCircle] Link: {}", GL20.glGetProgramInfoLog(programId));
                GL20.glDeleteProgram(programId);
                programId = -1;
                return;
            }
            GL20.glUseProgram(programId);
            locTex        = GL20.glGetUniformLocation(programId, "MagicCircleTex");
            locScreenSize = GL20.glGetUniformLocation(programId, "ScreenSize");
            locProgress   = GL20.glGetUniformLocation(programId, "Progress");
            locRotation   = GL20.glGetUniformLocation(programId, "Rotation");
            locAlpha      = GL20.glGetUniformLocation(programId, "Alpha");
            locCircleSize = GL20.glGetUniformLocation(programId, "CircleSize");
            GL20.glUseProgram(0);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[MagicCircle] Init failed", e);
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
        if (programId != -1) { GL20.glDeleteProgram(programId);      programId = -1; }
        if (vboId     != -1) { GL15.glDeleteBuffers(vboId);          vboId     = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId);     vaoId     = -1; }
        initialized = false;
    }

    // ── Animation curve helpers ───────────────────────────────────────────────

    private static float getMagicCircleAlpha(float tick) {
        if (tick < 380f)  return 0f;
        if (tick < 440f)  return (tick - 380f) / 60f;
        if (tick < 900f)  return 1f;
        if (tick < 1000f) return 1f - (tick - 900f) / 100f;
        return 0f;
    }

    private static float getMagicCircleProgress(float tick) {
        if (tick < 440f) return 0f;
        if (tick < 700f) return Math.min(1f, (tick - 440f) / 260f);
        return 1f;
    }

    private static float getMagicCircleRotation(float tick) {
        if (tick < 700f) return 0f;
        float t = Math.min(1f, (tick - 700f) / 260f);
        return t * t * (float)(Math.PI * 4.0); // ease-in, 2 full rotations
    }

    private static float getMagicCircleScale(float tick) {
        if (tick < 380f || tick > 1000f) return 0f;
        if (tick < 700f)  return 0.68f;
        if (tick < 850f)  return lerp(0.68f, 0.80f, (tick - 700f) / 150f);
        if (tick < 960f)  return lerp(0.80f, 0.00f, smoothstep((tick - 850f) / 110f));
        return 0f;
    }

    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    // ── Shader utilities ─────────────────────────────────────────────────────

    private static String read(ResourceManager rm, String path) throws IOException {
        var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int compile(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[MagicCircle] Compile: {}", GL20.glGetShaderInfoLog(id));
        return id;
    }
}
