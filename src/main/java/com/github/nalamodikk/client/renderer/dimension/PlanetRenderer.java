package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PlanetRenderer {

    public enum Type { ATMOSPHERE, ROCKY, SUN }

    private final Type type;
    private int programId = -1;
    private int vaoId     = -1;
    private int vboId     = -1;

    private int locTime, locRes, locInvProj, locInvView;
    private int locPlanetDir, locPlanetDist, locDirToStar;
    private int locAngularRadius, locPlanetColor;
    private int locAtmoColor, locAtmoDensity, locAtmoHeight, locPassGlow;
    private int locColorLight, locColorDark, locHeatColor, locHeatAmount;
    private int locSurface, locHasTexture;

    private boolean ready = false;

    public PlanetRenderer(Type type) { this.type = type; }

    public void init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        String base = "shaders/space/planet/";
        try {
            String vert = read(rm, base + "atmosphere.vsh");
            String frag = read(rm, base + switch (type) {
                case ROCKY -> "rocky.fsh";
                case SUN   -> "sun.fsh";
                default    -> "atmosphere.fsh";
            });
            programId = buildProgram(vert, frag);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("[PlanetRenderer:{}] Load failed", type, e);
            return;
        }
        if (programId == -1) return;

        GL20.glUseProgram(programId);
        locTime          = GL20.glGetUniformLocation(programId, "iTime");
        locRes           = GL20.glGetUniformLocation(programId, "iResolution");
        locInvProj       = GL20.glGetUniformLocation(programId, "InvProjMat");
        locInvView       = GL20.glGetUniformLocation(programId, "InvViewMat");
        locPlanetDir     = GL20.glGetUniformLocation(programId, "uPlanetDir");
        locPlanetDist    = GL20.glGetUniformLocation(programId, "uPlanetDist");
        locAngularRadius = GL20.glGetUniformLocation(programId, "uAngularRadius");
        locDirToStar     = GL20.glGetUniformLocation(programId, "uDirToStar");
        locSurface       = GL20.glGetUniformLocation(programId, "uSurface");
        locHasTexture    = GL20.glGetUniformLocation(programId, "uHasTexture");

        if (type == Type.ATMOSPHERE || type == Type.SUN) {
            locPlanetColor = GL20.glGetUniformLocation(programId, "uPlanetColor");
            locAtmoColor   = GL20.glGetUniformLocation(programId, "uAtmoColor");
            locAtmoDensity = GL20.glGetUniformLocation(programId, "uAtmoDensity");
            locAtmoHeight  = GL20.glGetUniformLocation(programId, "uAtmoHeight");
            locPassGlow    = GL20.glGetUniformLocation(programId, "uPassGlow");
        } else {
            locColorLight = GL20.glGetUniformLocation(programId, "uColorLight");
            locColorDark  = GL20.glGetUniformLocation(programId, "uColorDark");
            locHeatColor  = GL20.glGetUniformLocation(programId, "uHeatColor");
            locHeatAmount = GL20.glGetUniformLocation(programId, "uHeatAmount");
        }

        // 設定貼圖 sampler 到 unit 0
        if (locSurface != -1) GL20.glUniform1i(locSurface, 0);
        GL20.glUseProgram(0);

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
            new float[]{-1,-1, 1,-1, 1,1, -1,1}, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        ready = true;
    }

    public void renderAtmosphere(float[] invProj, float[] invView, float gameTime,
                                 float resW, float resH,
                                 Vector3f planetDir, float planetDist, float angularRadius,
                                 Vector3f dirToStar, Vector3f planetColor,
                                 Vector3f atmoColor, float atmoDensity, float atmoHeight,
                                 boolean passGlow, int textureId) {
        if (!ready) return;
        bindTexture(textureId);
        setCommon(invProj, invView, gameTime, resW, resH, planetDir, planetDist, angularRadius);
        GL20.glUniform3f(locDirToStar,   dirToStar.x,   dirToStar.y,   dirToStar.z);
        GL20.glUniform3f(locPlanetColor, planetColor.x, planetColor.y, planetColor.z);
        GL20.glUniform3f(locAtmoColor,   atmoColor.x,   atmoColor.y,   atmoColor.z);
        GL20.glUniform1f(locAtmoDensity, atmoDensity);
        GL20.glUniform1f(locAtmoHeight,  atmoHeight);
        GL20.glUniform1i(locPassGlow,    passGlow ? 1 : 0);
        draw();
    }

    public void renderRocky(float[] invProj, float[] invView, float gameTime,
                            float resW, float resH,
                            Vector3f planetDir, float planetDist, float angularRadius,
                            Vector3f dirToStar,
                            Vector3f colorLight, Vector3f colorDark,
                            Vector3f heatColor, float heatAmount, int textureId) {
        if (!ready) return;
        bindTexture(textureId);
        setCommon(invProj, invView, gameTime, resW, resH, planetDir, planetDist, angularRadius);
        GL20.glUniform3f(locDirToStar,  dirToStar.x,  dirToStar.y,  dirToStar.z);
        GL20.glUniform3f(locColorLight, colorLight.x, colorLight.y, colorLight.z);
        GL20.glUniform3f(locColorDark,  colorDark.x,  colorDark.y,  colorDark.z);
        GL20.glUniform3f(locHeatColor,  heatColor.x,  heatColor.y,  heatColor.z);
        GL20.glUniform1f(locHeatAmount, heatAmount);
        draw();
    }

    private void bindTexture(int textureId) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        if (textureId != -1) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL20.glUniform1i(locHasTexture, 1);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL20.glUniform1i(locHasTexture, 0);
        }
    }

    private void setCommon(float[] invProj, float[] invView, float gameTime,
                           float resW, float resH,
                           Vector3f planetDir, float planetDist, float angularRadius) {
        _prevProg  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        _wasDepth  = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        _wasBlend  = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glUseProgram(programId);
        GL20.glUniform1f(locTime,   gameTime);
        GL20.glUniform2f(locRes,    resW, resH);
        GL20.glUniformMatrix4fv(locInvProj, false, invProj);
        GL20.glUniformMatrix4fv(locInvView, false, invView);
        GL20.glUniform3f(locPlanetDir,    planetDir.x,    planetDir.y,    planetDir.z);
        GL20.glUniform1f(locPlanetDist,   planetDist);
        GL20.glUniform1f(locAngularRadius, angularRadius);
    }

    private void draw() {
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(_prevProg);
        if (_wasDepth) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (!_wasBlend) GL11.glDisable(GL11.GL_BLEND);
    }

    private int _prevProg; private boolean _wasDepth, _wasBlend;

    public void release() {
        if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
        if (vboId     != -1) { GL15.glDeleteBuffers(vboId);     vboId     = -1; }
        if (vaoId     != -1) { GL30.glDeleteVertexArrays(vaoId); vaoId    = -1; }
        ready = false;
    }

    private static String read(ResourceManager rm, String path) throws IOException {
        var loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
        try (InputStream in = rm.getResourceOrThrow(loc).open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int buildProgram(String vert, String frag) {
        int v = compile(GL20.GL_VERTEX_SHADER, vert);
        int f = compile(GL20.GL_FRAGMENT_SHADER, frag);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v); GL20.glAttachShader(p, f);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(v); GL20.glDeleteShader(f);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            KoniavacraftMod.LOGGER.error("[PlanetRenderer] Link: {}", GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p); return -1;
        }
        return p;
    }

    private static int compile(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src); GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            KoniavacraftMod.LOGGER.error("[PlanetRenderer] Compile: {}", GL20.glGetShaderInfoLog(id));
        return id;
    }
}
