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
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen-space Fresnel sphere.
 * A glowing-edge sphere rendered via raymarching using SDF.
 * Additive blend — transparent interior, bright rim.
 */
public class FresnelSphereRenderer {

    private record ActiveEffect(Vec3 pos, long startTick) {}

    private static final List<ActiveEffect> effects = new ArrayList<>();

    private static int  prog = -1, vao = -1, vbo = -1;
    private static int  locDiffuse, locBlockPos, locCamPos, locInvProj, locInvView, locTime;
    private static int  copyTex = -1, copyW = -1, copyH = -1;
    private static boolean init = false;

    public static void spawnEffect(Vec3 pos, long tick) {
        effects.removeIf(e -> e.pos().distanceToSqr(pos) < 1.0);
        if (effects.size() >= 3) effects.remove(0);
        effects.add(new ActiveEffect(pos, tick));
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (effects.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!init) { doInit(); if (!init) return; }

        var main   = mc.getMainRenderTarget();
        var camPos = event.getCamera().getPosition();
        int fw = main.width, fh = main.height;

        ensureCopyTex(fw, fh);
        int prevReadFBO = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
        GL30.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fw, fh);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFBO);

        int     prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);

        float[] invProj = new float[16], invView = new float[16];
        new Matrix4f(event.getProjectionMatrix()).invert().get(invProj);
        new Matrix4f(event.getModelViewMatrix()).invert().get(invView);

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now     = mc.level.getGameTime() + partial;

        GL20.glUseProgram(prog);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
        GL20.glUniform1i(locDiffuse, 0);
        GL20.glUniform3f(locCamPos, (float)camPos.x, (float)camPos.y, (float)camPos.z);
        GL20.glUniformMatrix4fv(locInvProj, false, invProj);
        GL20.glUniformMatrix4fv(locInvView, false, invView);

        GL30.glBindVertexArray(vao);
        for (ActiveEffect eff : effects) {
            float iTime = (now - eff.startTick()) / 20.0f;
            GL20.glUniform3f(locBlockPos, (float)eff.pos().x, (float)eff.pos().y + 1.5f, (float)eff.pos().z);
            GL20.glUniform1f(locTime, iTime);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        }
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend)  GL11.glEnable(GL11.GL_BLEND);     else GL11.glDisable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                                 GL11.GL_ONE,        GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public static void release() {
        if (prog   != -1) { GL20.glDeleteProgram(prog);     prog    = -1; }
        if (vbo    != -1) { GL15.glDeleteBuffers(vbo);      vbo     = -1; }
        if (vao    != -1) { GL30.glDeleteVertexArrays(vao); vao     = -1; }
        if (copyTex!= -1) { GL11.glDeleteTextures(copyTex); copyTex = -1; }
        copyW = copyH = -1;
        init = false; effects.clear();
    }

    private static void ensureCopyTex(int w, int h) {
        if (copyTex == -1) {
            copyTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            copyW = copyH = -1;
        }
        if (w != copyW || h != copyH) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            copyW = w; copyH = h;
        }
    }

    private static void doInit() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/orbital_test.vsh");
            String frag = read(rm, "shaders/fresnel_sphere.fsh");
            prog = buildProg(vert, frag);
            if (prog == -1) return;
            GL20.glUseProgram(prog);
            locDiffuse  = GL20.glGetUniformLocation(prog, "DiffuseSampler");
            locBlockPos = GL20.glGetUniformLocation(prog, "BlockPosition");
            locCamPos   = GL20.glGetUniformLocation(prog, "CameraPosition");
            locInvProj  = GL20.glGetUniformLocation(prog, "InvProjMat");
            locInvView  = GL20.glGetUniformLocation(prog, "InvViewMat");
            locTime     = GL20.glGetUniformLocation(prog, "iTime");
            GL20.glUseProgram(0);
        } catch (Exception e) { KoniavacraftMod.LOGGER.error("[FresnelSphere] Shader load failed", e); return; }

        vao = GL30.glGenVertexArrays(); vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[]{-1f,-1f, 1f,-1f, 1f,1f, -1f,1f}, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        init = true;
        KoniavacraftMod.LOGGER.info("[FresnelSphere] Initialized");
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
        GL20.glLinkProgram(p); GL20.glDeleteShader(vS); GL20.glDeleteShader(fS);
        if (GL20.glGetProgrami(p,GL20.GL_LINK_STATUS)==GL11.GL_FALSE){
            KoniavacraftMod.LOGGER.error("[FresnelSphere] Link: {}", GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p); return -1;
        }
        return p;
    }
    private static int cs(int t, String s) {
        int id=GL20.glCreateShader(t); GL20.glShaderSource(id,s); GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[FresnelSphere] Compile: {}", GL20.glGetShaderInfoLog(id));
        return id;
    }
}
