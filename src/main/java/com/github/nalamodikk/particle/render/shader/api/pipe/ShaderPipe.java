package com.github.nalamodikk.particle.render.shader.api.pipe;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.api.glsl.GlFrameBuffer;
import com.github.nalamodikk.particle.render.shader.api.pipe.handler.ShaderProgramUploader;
import java.util.function.Consumer;

public interface ShaderPipe {
    void init();

    ShaderPipe addRenderHandler(ShaderProgramUploader handler);

    GlFrameBuffer fbo();

    ShaderPipe useMipmap();

    void write(Consumer<ShaderPipe> invoker);

    ShaderPipe writeFromChannel(PipeChannels channel);

    void drawPipeFrame();

    PipeChannels getFrameOutput();

    void resize(int width, int height);

    void release();
    
    // Optional helper
    CooShaderProgram getProgram();
}
