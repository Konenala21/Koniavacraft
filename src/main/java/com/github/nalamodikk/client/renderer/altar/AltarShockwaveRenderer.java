package com.github.nalamodikk.client.renderer.altar;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class AltarShockwaveRenderer {

    private static int programId  = -1;
    private static int vaoId      = -1;
    private static int vboId      = -1;
    private static int locDepth, locInvProj, locInvView, locCamPos, locAltarPos, locWaves;
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        List<Map.Entry<BlockPos, AltarUpgradeAnimManager.AnimState>> entries =
                AltarUpgradeAnimManager.getActiveLowTierEntries();
        if (entries.isEmpty()) return;

        if (!initialized) {
            init();
            if (!initialized) return;
        }

        var mc         = Minecraft.getInstance();
        var mainTarget = mc.getMainRenderTarget();
        var camPos     = event.getCamera().getPosition();

        float[] invProj = new float[16];
        float[] invView = new float[16];
        new Matrix4f(event.getProjectionMatrix()).invert().get(invProj);
        new Matrix4f(event.getModelViewMatrix()).invert().get(invView);

        int prevProg  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDep = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBl  = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE); // additive glow

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainTarget.getDepthTextureId());

        GL20.glUseProgram(programId);
        GL20.glUniform1i(locDepth, 0);
        GL20.glUniformMatrix4fv(locInvProj, false, invProj);
        GL20.glUniformMatrix4fv(locInvView, false, invView);
        GL20.glUniform3f(locCamPos, (float) camPos.x, (float) camPos.y, (float) camPos.z);

        GL30.glBindVertexArray(vaoId);
        for (var entry : entries) {
            BlockPos pos   = entry.getKey();
            float    tick  = entry.getValue().tick();
            float[]  waves = computeWaves(tick);

            GL20.glUniform3f(locAltarPos, pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f);
            GL20.glUniform3fv(locWaves, waves);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        }
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (wasDep) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (!wasBl) GL11.glDisable(GL11.GL_BLEND);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    // Returns a float[9] for Waves[3] uniform: (radius, alpha, thickness) x 3
    private static float[] computeWaves(float tick) {
        float[] data = new float[9]; // 3 waves * 3 floats
        for (int w = 0; w < 3; w++) {
            float wAge = tick - w * 26f;
            if (wAge <= 0f || wAge > 26f) continue;
            float progress  = wAge / 26f;
            float radius    = progress * 14f;
            float alpha     = (float) Math.sin(progress * Math.PI) * 0.75f;
            float thickness = 0.20f + (1f - progress) * 0.30f;
            int base = w * 3;
            data[base]     = radius;
            data[base + 1] = alpha;
            data[base + 2] = thickness;
        }
        return data;
    }

    private static void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/altar_shockwave.vsh");
            String frag = read(rm, "shaders/altar_shockwave.fsh");
            int v = compile(GL20.GL_VERTEX_SHADER,   vert);
            int f = compile(GL20.GL_FRAGMENT_SHADER, frag);
            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, v);
            GL20.glAttachShader(programId, f);
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                KoniavacraftMod.LOGGER.error("[Shockwave] Link: {}", GL20.glGetProgramInfoLog(programId));
                GL20.glDeleteProgram(programId);
                programId = -1;
                return;
            }
            GL20.glUseProgram(programId);
            locDepth    = GL20.glGetUniformLocation(programId, "DepthSampler");
            locInvProj  = GL20.glGetUniformLocation(programId, "InvProjMat");
            locInvView  = GL20.glGetUniformLocation(programId, "InvViewMat");
            locCamPos   = GL20.glGetUniformLocation(programId, "CameraPosition");
            locAltarPos = GL20.glGetUniformLocation(programId, "AltarPos");
            locWaves    = GL20.glGetUniformLocation(programId, "Waves");
            GL20.glUseProgram(0);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[Shockwave] Init failed", e);
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
        if (programId != -1) { GL20.glDeleteProgram(programId);   programId = -1; }
        if (vboId     != -1) { GL15.glDeleteBuffers(vboId);       vboId     = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId);  vaoId     = -1; }
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
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[Shockwave] Compile: {}", GL20.glGetShaderInfoLog(id));
        return id;
    }
}
