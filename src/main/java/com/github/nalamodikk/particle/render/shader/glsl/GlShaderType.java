package com.github.nalamodikk.particle.render.shader.glsl;

import org.lwjgl.opengl.GL33;

public enum GlShaderType {
    VERTEX(GL33.GL_VERTEX_SHADER),
    FRAGMENT(GL33.GL_FRAGMENT_SHADER);

    private final int glType;

    GlShaderType(int glType) {
        this.glType = glType;
    }

    public int getGlType() {
        return glType;
    }
}
