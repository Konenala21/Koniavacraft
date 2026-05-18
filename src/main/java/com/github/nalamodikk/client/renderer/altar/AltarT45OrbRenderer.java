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
public class AltarT45OrbRenderer {

    private static final String[] FRAG_PATHS = {
        "shaders/altar/common.glsl",
        "shaders/altar/fx_orbs_t45.glsl",
        "shaders/altar/fx_ground_t45.glsl",
        "shaders/altar/fx_pillars_t45.glsl",
        "shaders/altar/stage_altar_t45.fsh",
    };

    private static int  programId   = -1;
    private static int  vaoId       = -1;
    private static int  vboId       = -1;
    private static int  locDepth, locInvProj, locInvView, locCamPos, locBlockPos, locTime;
    private static boolean initialized = false;
    private static boolean initFailed  = false;

    private static final Matrix4f SCRATCH_PROJ = new Matrix4f();
    private static final Matrix4f SCRATCH_VIEW = new Matrix4f();
    private static final float[]  INV_PROJ_ARR = new float[16];
    private static final float[]  INV_VIEW_ARR = new float[16];

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        List<Map.Entry<BlockPos, AltarUpgradeAnimManager.AnimState>> entries =
                AltarUpgradeAnimManager.getActiveT45Entries();
        if (entries.isEmpty()) return;

        if (!initialized) {
            if (initFailed) return;
            init();
            if (!initialized) return;
        }

        var mc         = Minecraft.getInstance();
        var mainTarget = mc.getMainRenderTarget();
        var camPos     = event.getCamera().getPosition();

        SCRATCH_PROJ.set(event.getProjectionMatrix()).invert().get(INV_PROJ_ARR);
        SCRATCH_VIEW.set(event.getModelViewMatrix()).invert().get(INV_VIEW_ARR);

        int  prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDep = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBl  = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR); // screen blend: no blowout in daylight

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainTarget.getDepthTextureId());

        GL20.glUseProgram(programId);
        GL20.glUniform1i(locDepth, 0);
        GL20.glUniformMatrix4fv(locInvProj, false, INV_PROJ_ARR);
        GL20.glUniformMatrix4fv(locInvView, false, INV_VIEW_ARR);
        GL20.glUniform3f(locCamPos, (float) camPos.x, (float) camPos.y, (float) camPos.z);

        // Limit to 1 entry: the shader runs a 100-step raymarching per pixel;
        // multiple simultaneous altars would multiply the cost with no easy batching path.
        var entry = entries.get(0);
        BlockPos pos   = entry.getKey();
        AltarUpgradeAnimManager.AnimState state = entry.getValue();
        float tick = state.tick();
        if (state.tier() == 6) tick = Math.min(tick, AltarUpgradeAnimManager.T6_PHASE_OFFSET - 1f);

        GL20.glUniform3f(locBlockPos, pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f);
        GL20.glUniform1f(locTime, tick / 20f);
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (wasDep) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (!wasBl)  GL11.glDisable(GL11.GL_BLEND);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private static void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/altar_shockwave.vsh");
            StringBuilder frag = new StringBuilder();
            for (String path : FRAG_PATHS) frag.append(read(rm, path)).append('\n');

            int v = compile(GL20.GL_VERTEX_SHADER,   vert);
            int f = compile(GL20.GL_FRAGMENT_SHADER, frag.toString());
            if (initFailed) { GL20.glDeleteShader(v); GL20.glDeleteShader(f); return; }
            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, v);
            GL20.glAttachShader(programId, f);
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                KoniavacraftMod.LOGGER.error("[AltarT45Orb] Link: {}", GL20.glGetProgramInfoLog(programId));
                GL20.glDeleteProgram(programId);
                programId = -1;
                initFailed = true;
                return;
            }
            GL20.glUseProgram(programId);
            locDepth    = GL20.glGetUniformLocation(programId, "DepthSampler");
            locInvProj  = GL20.glGetUniformLocation(programId, "InvProjMat");
            locInvView  = GL20.glGetUniformLocation(programId, "InvViewMat");
            locCamPos   = GL20.glGetUniformLocation(programId, "CameraPosition");
            locBlockPos = GL20.glGetUniformLocation(programId, "BlockPosition");
            locTime     = GL20.glGetUniformLocation(programId, "iTime");
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
            KoniavacraftMod.LOGGER.error("[AltarT45Orb] Init failed", e);
            if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
            initFailed = true;
        }
    }

    public static void reload() { initFailed = false; release(); }

    public static void release() {
        if (programId != -1) { GL20.glDeleteProgram(programId);      programId   = -1; }
        if (vboId     != -1) { GL15.glDeleteBuffers(vboId);          vboId       = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId);     vaoId       = -1; }
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
            KoniavacraftMod.LOGGER.error("[AltarT45Orb] Compile: {}", GL20.glGetShaderInfoLog(id));
            initFailed = true;
        }
        return id;
    }
}
