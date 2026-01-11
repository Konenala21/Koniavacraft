package com.github.nalamodikk.particle.render.shader.pipe;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.api.pipe.ShaderPipe;

public class SimpleShaderPipe implements ShaderPipe {
    private final CooShaderProgram program;

    public SimpleShaderPipe(CooShaderProgram program) {
        this.program = program;
    }

    @Override
    public void init() {
        program.init();
    }

    @Override
    public void release() {
        program.release();
    }

    @Override
    public void resize(int width, int height) {
        // Simple pipe doesn't handle FBO resizing usually
    }

    @Override
    public CooShaderProgram getProgram() {
        return program;
    }
}
