package com.github.nalamodikk.particle.render.shader;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.glsl.GlShader;

import static org.lwjgl.opengl.GL33.*;

public class SimpleShaderProgram implements CooShaderProgram {
    private int program = 0;
    private int prevProgram = 0;
    private final GlShader vertexShader;
    private final GlShader fragmentShader;

    public SimpleShaderProgram(GlShader vertexShader, GlShader fragmentShader) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
    }

    @Override
    public int getProgram() {
        return program;
    }

    @Override
    public GlShader getVertexShader() {
        return vertexShader;
    }

    @Override
    public GlShader getFragmentShader() {
        return fragmentShader;
    }

    @Override
    public void init() {
        program = glCreateProgram();
        vertexShader.compile();
        fragmentShader.compile();
        
        glAttachShader(program, vertexShader.getShaderID());
        glAttachShader(program, fragmentShader.getShaderID());
        
        glLinkProgram(program);
        assertProgram();
        
        // ??畸? Shader ??麾???蝞???Link ??Program ??        vertexShader.deleteShader();
        fragmentShader.deleteShader();
    }

    @Override
    public void use() {
        prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
        glUseProgram(program);
    }

    @Override
    public void reset() {
        glUseProgram(prevProgram);
    }

    @Override
    public void release() {
        if (program > 0) {
            glDeleteProgram(program);
        }
    }

    private void assertProgram() {
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            throw new RuntimeException("Program " + program + " link error: " + log);
        }
    }
}
