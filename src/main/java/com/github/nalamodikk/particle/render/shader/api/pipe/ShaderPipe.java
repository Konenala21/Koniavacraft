package com.github.nalamodikk.particle.render.shader.api.pipe;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;

public interface ShaderPipe {
    void init();
    void release();
    void resize(int width, int height);
    
    // 簡單版：只獲取 ShaderProgram
    CooShaderProgram getProgram();
}
