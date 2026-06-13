package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SpaceSkyRenderer {

    private static int programId = -1;
    private static int vaoId     = -1;
    private static int vboId     = -1;

    private static int locTime, locInvProj, locInvView, locResolution, locMoonSky, locAtmosphere;

    private static boolean initialized = false;

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        boolean space = mc.level.dimension().equals(ModDimensions.SPACE);
        boolean moon  = mc.level.dimension().equals(ModDimensions.MOON);

        // atmo = 1 全太空（不透明）；主世界爬升 = 漸變值（星空淡入疊在原版天空上）。
        // 在主世界就跑這個 shader 也順便做了「背景預載」：切換前 init() 已完成 → 進 SPACE 不再冷啟動閃黑。
        float atmo;
        if (space || moon) {
            // 月球地底/中空不畫星空
            if (moon && mc.player != null
                    && mc.player.getY() < com.github.nalamodikk.dimension.MoonChunkGenerator.CRUST_BOTTOM) return;
            atmo = 1.0f;
        } else {
            atmo = AtmosphereTransition.skyFade(mc); // 比地球漸變陡,切換前就全黑蓋掉 vanilla 太陽月亮
            if (atmo <= 0f) return;
        }

        if (!initialized) {
            init();
            if (programId == -1) return;
        }

        var target = mc.getMainRenderTarget();

        int  prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        // 全太空(atmo=1)：不透明直接蓋。主世界爬升(atmo<1)：alpha blend 把星空淡入疊在原版天空上。
        if (atmo < 1.0f) {
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }

        GL20.glUseProgram(programId);

        float partial  = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float gameTime = (mc.level.getGameTime() + partial) / 20.0f;
        GL20.glUniform1f(locTime, gameTime);
        GL20.glUniform2f(locResolution, target.width, target.height);
        if (locMoonSky != -1) GL20.glUniform1i(locMoonSky, moon ? 1 : 0);
        if (locAtmosphere != -1) GL20.glUniform1f(locAtmosphere, atmo);

        float[] invProj = new float[16];
        float[] invView = new float[16];
        new Matrix4f(event.getProjectionMatrix()).invert().get(invProj);
        new Matrix4f(event.getModelViewMatrix()).invert().get(invView);
        GL20.glUniformMatrix4fv(locInvProj, false, invProj);
        GL20.glUniformMatrix4fv(locInvView, false, invView);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProg);
        if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (blend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
    }

    public static void reload() { release(); }

    private static void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            String vert = read(rm, "shaders/space/space_sky.vsh");
            String frag = read(rm, "shaders/space/space_sky.fsh");
            programId = buildProgram(vert, frag);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[SpaceSky] Shader load failed", e);
            programId = -1;
            return;
        }

        GL20.glUseProgram(programId);
        locTime       = GL20.glGetUniformLocation(programId, "iTime");
        locInvProj    = GL20.glGetUniformLocation(programId, "InvProjMat");
        locInvView    = GL20.glGetUniformLocation(programId, "InvViewMat");
        locResolution = GL20.glGetUniformLocation(programId, "iResolution");
        locMoonSky    = GL20.glGetUniformLocation(programId, "uMoonSky");
        locAtmosphere = GL20.glGetUniformLocation(programId, "uAtmosphere");
        GL20.glUseProgram(0);

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        float[] quad = { -1f,-1f, 1f,-1f, 1f,1f, -1f,1f };
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
        initialized = false;
    }

    private static String read(ResourceManager rm, String path) throws IOException {
        var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int buildProgram(String vert, String frag) {
        int v = compile(GL20.GL_VERTEX_SHADER,   vert);
        int f = compile(GL20.GL_FRAGMENT_SHADER, frag);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[SpaceSky] Link: {}", GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p);
            return -1;
        }
        return p;
    }

    private static int compile(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[SpaceSky] Compile: {}", GL20.glGetShaderInfoLog(id));
        return id;
    }
}
