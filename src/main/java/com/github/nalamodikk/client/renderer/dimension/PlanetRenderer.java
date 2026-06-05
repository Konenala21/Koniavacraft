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

    public enum Type { ATMOSPHERE, ROCKY, SUN, RING }

    private final Type type;
    private int programId = -1;
    private int vaoId     = -1;
    private int vboId     = -1;

    private int locTime, locRes, locInvProj, locInvView;
    private int locPlanetDir, locPlanetDist, locDirToStar;
    private int locAngularRadius, locPlanetColor;
    private int locAtmoColor, locAtmoDensity, locAtmoHeight, locPassGlow;
    private int locColorLight, locColorDark, locHeatColor, locHeatAmount;
    private int locSurface, locHasTexture, locSurface2, locHasTexture2, locNightTex, locHasNight;
    private int locRotSpeed, locCloudSpeed, locAlpha;
    private int locRingInner, locRingOuter, locRingTilt, locRingTex;
    private int locOccluderDir, locOccluderCos;

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
                case RING  -> "ring.fsh";
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
        locSurface2      = GL20.glGetUniformLocation(programId, "uSurface2");
        locHasTexture2   = GL20.glGetUniformLocation(programId, "uHasTexture2");

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
        if (locSurface   != -1) GL20.glUniform1i(locSurface,   0);
        if (locSurface2  != -1) GL20.glUniform1i(locSurface2,  1);
        locNightTex  = GL20.glGetUniformLocation(programId, "uNightTex");
        locHasNight  = GL20.glGetUniformLocation(programId, "uHasNight");
        if (locNightTex  != -1) GL20.glUniform1i(locNightTex,  2);
        locRotSpeed   = GL20.glGetUniformLocation(programId, "uRotSpeed");
        locCloudSpeed = GL20.glGetUniformLocation(programId, "uCloudSpeed");
        locAlpha       = GL20.glGetUniformLocation(programId, "uAlpha");
        locOccluderDir = GL20.glGetUniformLocation(programId, "uOccluderDir");
        locOccluderCos = GL20.glGetUniformLocation(programId, "uOccluderCos");
        if (type == Type.RING) {
            locRingInner = GL20.glGetUniformLocation(programId, "uRingInner");
            locRingOuter = GL20.glGetUniformLocation(programId, "uRingOuter");
            locRingTilt  = GL20.glGetUniformLocation(programId, "uRingTilt");
            locRingTex   = GL20.glGetUniformLocation(programId, "uRingTex");
            if (locRingTex != -1) GL20.glUniform1i(locRingTex, 0);
        }
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
                                 boolean passGlow, int textureId, int textureId2, int nightTexId,
                                 float rotSpeed, float cloudSpeed, float alpha,
                                 Vector3f occluderDir, float occluderCos) {
        if (!ready) return;
        setCommon(invProj, invView, gameTime, resW, resH, planetDir, planetDist, angularRadius);
        bindTexture(textureId, textureId2, nightTexId);
        if (locRotSpeed   != -1) GL20.glUniform1f(locRotSpeed,   rotSpeed);
        if (locCloudSpeed != -1) GL20.glUniform1f(locCloudSpeed, cloudSpeed);
        if (locAlpha      != -1) GL20.glUniform1f(locAlpha,      alpha);
        if (locOccluderCos!= -1) GL20.glUniform1f(locOccluderCos, occluderCos);
        if (locOccluderDir!= -1 && occluderDir != null)
            GL20.glUniform3f(locOccluderDir, occluderDir.x, occluderDir.y, occluderDir.z);
        GL20.glUniform3f(locDirToStar,   dirToStar.x,   dirToStar.y,   dirToStar.z);
        GL20.glUniform3f(locPlanetColor, planetColor.x, planetColor.y, planetColor.z);
        GL20.glUniform3f(locAtmoColor,   atmoColor.x,   atmoColor.y,   atmoColor.z);
        GL20.glUniform1f(locAtmoDensity, atmoDensity);
        GL20.glUniform1f(locAtmoHeight,  atmoHeight);
        GL20.glUniform1i(locPassGlow,    passGlow ? 1 : 0);
        draw();
    }

    public void renderRing(float[] invProj, float[] invView, float gameTime,
                           float resW, float resH,
                           Vector3f planetDir, float planetDist, float angularRadius,
                           Vector3f dirToStar,
                           float ringInner, float ringOuter, float ringTiltDeg,
                           int ringTexId, float alpha) {
        if (!ready) return;
        setCommon(invProj, invView, gameTime, resW, resH, planetDir, planetDist, angularRadius);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, ringTexId != -1 ? ringTexId : 0);
        GL20.glUniform3f(locDirToStar, dirToStar.x, dirToStar.y, dirToStar.z);
        if (locRingInner != -1) GL20.glUniform1f(locRingInner, ringInner);
        if (locRingOuter != -1) GL20.glUniform1f(locRingOuter, ringOuter);
        if (locRingTilt  != -1) GL20.glUniform1f(locRingTilt,  (float)Math.toRadians(ringTiltDeg));
        if (locAlpha     != -1) GL20.glUniform1f(locAlpha,     alpha);
        draw();
    }

    public void renderRocky(float[] invProj, float[] invView, float gameTime,
                            float resW, float resH,
                            Vector3f planetDir, float planetDist, float angularRadius,
                            Vector3f dirToStar,
                            Vector3f colorLight, Vector3f colorDark,
                            Vector3f heatColor, float heatAmount, int textureId, float alpha,
                            Vector3f occluderDir, float occluderCos) {
        if (!ready) return;
        setCommon(invProj, invView, gameTime, resW, resH, planetDir, planetDist, angularRadius);
        bindTexture(textureId);
        if (locAlpha      != -1) GL20.glUniform1f(locAlpha,      alpha);
        if (locOccluderCos!= -1) GL20.glUniform1f(locOccluderCos, occluderCos);
        if (locOccluderDir!= -1 && occluderDir != null)
            GL20.glUniform3f(locOccluderDir, occluderDir.x, occluderDir.y, occluderDir.z);
        GL20.glUniform3f(locDirToStar,  dirToStar.x,  dirToStar.y,  dirToStar.z);
        GL20.glUniform3f(locColorLight, colorLight.x, colorLight.y, colorLight.z);
        GL20.glUniform3f(locColorDark,  colorDark.x,  colorDark.y,  colorDark.z);
        GL20.glUniform3f(locHeatColor,  heatColor.x,  heatColor.y,  heatColor.z);
        GL20.glUniform1f(locHeatAmount, heatAmount);
        draw();
    }

    private void bindTexture(int t0) { bindTexture(t0, -1, -1); }
    private void bindTexture(int t0, int t1) { bindTexture(t0, t1, -1); }

    private void bindTexture(int t0, int t1, int t2) {
        bind(GL13.GL_TEXTURE0, locHasTexture,  t0);
        bind(GL13.GL_TEXTURE0 + 1, locHasTexture2, t1);
        if (locHasNight != -1) bind(GL13.GL_TEXTURE0 + 2, locHasNight, t2);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    private void bind(int unit, int locFlag, int texId) {
        GL13.glActiveTexture(unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId != -1 ? texId : 0);
        if (locFlag != -1) GL20.glUniform1i(locFlag, texId != -1 ? 1 : 0);
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

    public void setOccluder(Vector3f dir, float cosCutoff) {
        if (locOccluderDir != -1) GL20.glUniform3f(locOccluderDir, dir.x, dir.y, dir.z);
        if (locOccluderCos != -1) GL20.glUniform1f(locOccluderCos, cosCutoff);
    }

    public void clearOccluder() {
        if (locOccluderCos != -1) GL20.glUniform1f(locOccluderCos, 1.001f);
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
