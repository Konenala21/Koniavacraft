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
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL33;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PingPongShaderPipe implements ShaderPipe {
    private final GlShader fragment;
    private int pingpongCount = 2;
    private final int textureFilterMod;
    private final GlShader screenVertex = new FileShader("pipe/vertexes/screen.vsh", GlShaderType.VERTEX);
    private final SimpleVertexBuffer vertexes = VertexBuffers.getScreenBuffer();
    private final List<ShaderProgramUploader> pingHandlers = new ArrayList<>();
    private final List<ShaderProgramUploader> pongHandles = new ArrayList<>();
    private final CooShaderProgram screenBlit;
    private final SimpleFrameBuffer pingFBO;
    private final SimpleFrameBuffer pongFBO;
    private boolean ping = true;

    public PingPongShaderPipe(GlShader fragment, Supplier<Integer> depthSupplier, int colorChannelCount, int textureFilterMod) {
        this.fragment = fragment;
        this.textureFilterMod = textureFilterMod;
        this.screenBlit = new ShaderProgramBuilder().vertex(screenVertex).fragment(fragment).build();
        this.pingFBO = new SimpleFrameBuffer(colorChannelCount, depthSupplier);
        this.pongFBO = new SimpleFrameBuffer(colorChannelCount, depthSupplier);
    }

    public PingPongShaderPipe(GlShader fragment, Supplier<Integer> depthSupplier) {
        this(fragment, depthSupplier, 1, GL33.GL_LINEAR);
    }

    @Override
    public void init() {
        if (fragment.getType() != GlShaderType.FRAGMENT) throw new IllegalArgumentException("Fragment shader required");
        screenBlit.init();
        pingFBO.setTextureFilterMod(textureFilterMod);
        pongFBO.setTextureFilterMod(textureFilterMod);
        pingFBO.init();
        pongFBO.init();
        vertexes.init();
    }

    @Override
    public ShaderPipe addRenderHandler(ShaderProgramUploader handler) { pingHandlers.add(handler); return this; }
    public PingPongShaderPipe addRenderHandlerPong(ShaderProgramUploader handler) { pongHandles.add(handler); return this; }

    @Override
    public GlFrameBuffer fbo() { return ping ? pingFBO : pongFBO; }

    @Override
    public ShaderPipe useMipmap() { pingFBO.useMipmap(); pongFBO.useMipmap(); return this; }

    @Override
    public void write(Consumer<ShaderPipe> invoker) {
        pingFBO.writeFrameBufferWith(buffer -> {
            RenderSystem.disableBlend();
            invoker.accept(this);
        });
    }

    @Override
    public void drawPipeFrame() {
        writePingPong();
        screenBlit.useOnContext(() -> {
            for (ShaderProgramUploader h : getAnotherHandler()) h.uploadShaderData(screenBlit);
            fbo().readFrameBufferWith(buffer -> vertexes.draw());
        });
        ping = true;
    }

    @Override
    public PipeChannels getFrameOutput() { return fbo().outputChannels(); }

    @Override
    public ShaderPipe writeFromChannel(PipeChannels channel) {
        channel.useOnContext(() -> write(pipe -> screenBlit.useOnContext(() -> {
            for (ShaderProgramUploader h : getAnotherHandler()) h.uploadShaderData(screenBlit);
            vertexes.draw();
        })));
        ping = true;
        writePingPong();
        ping = true;
        return this;
    }

    private void writePingPong() {
        for (int i = 0; i < pingpongCount - 1; i++) {
            GlFrameBuffer current = fbo();
            getAnother().writeFrameBufferWith(buffer -> screenBlit.useOnContext(() -> {
                for (ShaderProgramUploader h : getAnotherHandler()) h.uploadShaderData(screenBlit);
                current.readFrameBufferWith(readBuffer -> vertexes.draw());
            }));
            ping = !ping;
        }
    }

    public PingPongShaderPipe setPipeRenderCount(int count) { this.pingpongCount = count; return this; }
    @Override public void resize(int width, int height) { pingFBO.resize(width, height); pongFBO.resize(width, height); }
    @Override public void release() { pingFBO.release(); pongFBO.release(); screenBlit.release(); vertexes.release(); }
    @Override public CooShaderProgram getProgram() { return screenBlit; }
    private GlFrameBuffer getAnother() { return ping ? pongFBO : pingFBO; }
    private List<ShaderProgramUploader> getAnotherHandler() { return ping ? pongHandles : pingHandlers; }
}