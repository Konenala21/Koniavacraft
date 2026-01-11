package com.github.nalamodikk.particle.render.shader.pipe;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.api.pipe.ShaderPipe;

/**
 * 乒乓緩衝管線
 * 用於多重後處理（如高斯模糊）
 */
public class PingPongShaderPipe implements ShaderPipe {
    private final CooShaderProgram program;
    // FBOs would be here

    public PingPongShaderPipe(CooShaderProgram program) {
        this.program = program;
    }

    @Override
    public void init() {
        program.init();
        // Init FBOs
    }

    @Override
    public void release() {
        program.release();
        // Release FBOs
    }

    @Override
    public void resize(int width, int height) {
        // Resize FBOs
    }

    @Override
    public CooShaderProgram getProgram() {
        return program;
    }
    
    public void swap() {
        // Swap buffers
    }
}
