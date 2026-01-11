package com.github.nalamodikk.particle.render.shader.glsl;

public interface GlShader {
    GlShaderType getType();
    int getShaderID();
    void compile();
    void assertCompiled();
    void deleteShader();
}
