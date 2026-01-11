package com.github.nalamodikk.particle.render.shader;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.glsl.FileShader;
import com.github.nalamodikk.particle.render.shader.glsl.GlShader;
import com.github.nalamodikk.particle.render.shader.glsl.GlShaderType;

public class ShaderProgramBuilder {
    private GlShader vertex;
    private GlShader fragment;

    public ShaderProgramBuilder vertex(String path) {
        this.vertex = new FileShader(path, GlShaderType.VERTEX);
        return this;
    }

    public ShaderProgramBuilder fragment(String path) {
        this.fragment = new FileShader(path, GlShaderType.FRAGMENT);
        return this;
    }

    public ShaderProgramBuilder vertex(GlShader shader) {
        this.vertex = shader;
        return this;
    }

    public ShaderProgramBuilder fragment(GlShader shader) {
        this.fragment = shader;
        return this;
    }

    public CooShaderProgram build() {
        if (vertex == null || fragment == null) {
            throw new IllegalStateException("Vertex and fragment shaders cannot be null");
        }
        return new SimpleShaderProgram(vertex, fragment);
    }
}
