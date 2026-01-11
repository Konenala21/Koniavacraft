package com.github.nalamodikk.particle.render.shader.glsl;

import com.github.nalamodikk.particle.render.shader.ShaderUtil;
import org.lwjgl.opengl.GL33;

import static org.lwjgl.opengl.GL33.*;

public class FileShader implements GlShader {
    private final String path;
    private final GlShaderType type;
    private int shaderID = 0;

    public FileShader(String path, GlShaderType type) {
        this.path = path;
        this.type = type;
    }

    @Override
    public GlShaderType getType() {
        return type;
    }

    @Override
    public int getShaderID() {
        return shaderID;
    }

    @Override
    public void compile() {
        shaderID = glCreateShader(type.getGlType());
        glShaderSource(shaderID, readFromJar());
        glCompileShader(shaderID);
        assertCompiled();
    }

    @Override
    public void assertCompiled() {
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderID);
            throw new RuntimeException("Shader compilation failed: " + path + "\n" + log);
        }
    }

    @Override
    public void deleteShader() {
        glDeleteShader(shaderID);
    }

    private String readFromJar() {
        return ShaderUtil.readShaderSource(path);
    }
}
