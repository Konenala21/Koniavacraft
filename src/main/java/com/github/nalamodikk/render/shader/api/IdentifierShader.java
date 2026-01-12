package com.github.nalamodikk.render.shader.api;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.render.shader.utils.GlslUtil;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL20;

/**
 * 基於 ResourceLocation 的 Shader 封裝
 */
public class IdentifierShader {
    private final ResourceLocation vertexId;
    private final ResourceLocation fragmentId;
    private int programId;

    public IdentifierShader(ResourceLocation vertexId, ResourceLocation fragmentId) {
        this.vertexId = vertexId;
        this.fragmentId = fragmentId;
    }

    public void init() {
        String vert = GlslUtil.readShader(vertexId);
        String frag = GlslUtil.readShader(fragmentId);

        int vShader = compile(vert, GL20.GL_VERTEX_SHADER);
        int fShader = compile(frag, GL20.GL_FRAGMENT_SHADER);

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vShader);
        GL20.glAttachShader(programId, fShader);
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            KoniavacraftMod.LOGGER.error("Shader 連結失敗: " + GL20.glGetProgramInfoLog(programId));
        }

        GL20.glDeleteShader(vShader);
        GL20.glDeleteShader(fShader);
    }

    private int compile(String source, int type) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
            KoniavacraftMod.LOGGER.error("Shader 編譯失敗: " + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public void release() {
        GL20.glDeleteProgram(programId);
    }

    public int getProgramId() {
        return programId;
    }
}
