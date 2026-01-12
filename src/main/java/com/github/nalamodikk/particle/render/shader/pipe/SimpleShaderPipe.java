package com.github.nalamodikk.particle.render.shader.pipe;

import com.github.nalamodikk.particle.render.shader.ShaderProgramBuilder;
import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.api.glsl.GlFrameBuffer;
import com.github.nalamodikk.particle.render.shader.glsl.GlShader;
import com.github.nalamodikk.particle.render.shader.glsl.GlShaderType;
import com.github.nalamodikk.particle.render.shader.api.pipe.PipeChannels;
import com.github.nalamodikk.particle.render.shader.api.pipe.ShaderPipe;
import com.github.nalamodikk.particle.render.shader.api.pipe.handler.ShaderProgramUploader;
import com.github.nalamodikk.particle.render.shader.glsl.FileShader;
import com.github.nalamodikk.particle.render.shader.glsl.SimpleFrameBuffer;
import com.github.nalamodikk.particle.render.shader.vertex.SimpleVertexBuffer;
import com.github.nalamodikk.particle.render.shader.vertex.VertexBuffers;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL33;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleShaderPipe implements ShaderPipe {
    private final GlShader fragment;
    private final int textureFilterMod;
    private final GlShader screenVertex = new FileShader("pipe/vertexes/screen.vsh", GlShaderType.VERTEX);
    private final SimpleVertexBuffer shaderVertexes = VertexBuffers.getScreenBuffer();
    private final List<ShaderProgramUploader> handles = new ArrayList<>();
    private final CooShaderProgram screenProgram;
    private final SimpleFrameBuffer fbo;

    public SimpleShaderPipe(GlShader fragment, Supplier<Integer> depthSupplier, int colorChannelCount, int textureFilterMod) {
        this.fragment = fragment;
        this.textureFilterMod = textureFilterMod;
        this.screenProgram = new ShaderProgramBuilder().vertex(screenVertex).fragment(fragment).build();
        this.fbo = new SimpleFrameBuffer(colorChannelCount, depthSupplier);
    }

    public SimpleShaderPipe(GlShader fragment, Supplier<Integer> depthSupplier) {
        this(fragment, depthSupplier, 1, GL33.GL_LINEAR);
    }

    @Override
    public void init() {
        if (fragment.getType() != GlShaderType.FRAGMENT) throw new IllegalArgumentException("Fragment shader required");
        fbo.setTextureFilterMod(textureFilterMod);
        screenProgram.init();
        fbo.init();
        shaderVertexes.init();
    }

    @Override public ShaderPipe addRenderHandler(ShaderProgramUploader h) { handles.add(h); return this; }
    @Override public GlFrameBuffer fbo() { return fbo; }
    @Override public ShaderPipe useMipmap() { fbo.useMipmap(); return this; }

    @Override
    public void write(Consumer<ShaderPipe> invoker) {
        screenProgram.useOnContext(() -> {
            for (ShaderProgramUploader h : handles) h.uploadShaderData(screenProgram);
            fbo.writeFrameBufferWith(buffer -> {
                RenderSystem.disableBlend();
                invoker.accept(this);
            });
        });
    }

    @Override
    public ShaderPipe writeFromChannel(PipeChannels channel) {
        channel.useOnContext(() -> write(pipe -> shaderVertexes.draw()));
        return this;
    }

    @Override public void drawPipeFrame() { drawOnce(); }
    @Override public PipeChannels getFrameOutput() { return fbo.outputChannels(); }

    private void drawOnce() {
        screenProgram.useOnContext(() -> {
            for (ShaderProgramUploader h : handles) h.uploadShaderData(screenProgram);
            fbo.readFrameBufferWith(buffer -> shaderVertexes.draw());
        });
    }

    @Override public void resize(int w, int h) { fbo.resize(w, h); }
    @Override public void release() { fbo.release(); screenProgram.release(); shaderVertexes.release(); }
    @Override public CooShaderProgram getProgram() { return screenProgram; }
}