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
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class AltarFadeRenderer {

    private static int programId = -1;
    private static int vaoId     = -1;
    private static int locColor;
    private static boolean initialized = false;
    private static boolean initFailed  = false;

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float alpha = AltarUpgradeAnimManager.getScreenFadeAlpha();
        if (alpha <= 0.001f) return;

        if (!initialized) {
            if (initFailed) return;
            init();
            if (!initialized) return;
        }

        int prevProg  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL20.glUseProgram(programId);
        GL20.glUniform4f(locColor, 0f, 0f, 0f, alpha);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (!wasBlend) GL11.glDisable(GL11.GL_BLEND);
    }

    private static void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/altar_fade.vsh");
            String frag = read(rm, "shaders/altar_fade.fsh");
            int v = compile(GL20.GL_VERTEX_SHADER, vert);
            int f = compile(GL20.GL_FRAGMENT_SHADER, frag);
            if (initFailed) { GL20.glDeleteShader(v); GL20.glDeleteShader(f); return; }
            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, v);
            GL20.glAttachShader(programId, f);
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                KoniavacraftMod.LOGGER.error("[AltarFade] Link: {}", GL20.glGetProgramInfoLog(programId));
                GL20.glDeleteProgram(programId);
                programId = -1;
                initFailed = true;
                return;
            }
            GL20.glUseProgram(programId);
            locColor = GL20.glGetUniformLocation(programId, "FadeColor");
            GL20.glUseProgram(0);

            // Empty VAO — vertex positions generated in shader via gl_VertexID
            vaoId = GL30.glGenVertexArrays();
            initialized = true;
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[AltarFade] Init failed", e);
            if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
            initFailed = true;
        }
    }

    public static void reload() { initFailed = false; release(); }

    public static void release() {
        if (programId != -1) { GL20.glDeleteProgram(programId);  programId = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId); vaoId     = -1; }
        initialized = false;
    }

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
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[AltarFade] Compile: {}", GL20.glGetShaderInfoLog(id));
            initFailed = true;
        }
        return id;
    }
}
