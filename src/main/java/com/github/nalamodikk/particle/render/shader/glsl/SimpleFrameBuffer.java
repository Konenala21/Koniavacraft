package com.github.nalamodikk.particle.render.shader.glsl;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.render.shader.api.glsl.GlFrameBuffer;
import com.github.nalamodikk.particle.render.shader.api.pipe.PipeChannels;
import com.github.nalamodikk.particle.render.shader.pipe.manager.FramePipeChannels;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL33;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleFrameBuffer implements GlFrameBuffer {
    private final int colorChannelCount;
    private Supplier<Integer> depthSupplier;
    private final int[] colorAttachments;
    
    private boolean useMipmap = false;
    private int depthAttachment = -1;
    private int fbo = 0;
    private int prevFBO = 0;
    private boolean initialized = false;
    private boolean newDepth = false;
    private int textureFilterMod = GL33.GL_LINEAR;
    private PipeChannels output;

    public SimpleFrameBuffer(int colorChannelCount, Supplier<Integer> depthSupplier) {
        this.colorChannelCount = colorChannelCount;
        this.depthSupplier = depthSupplier;
        this.colorAttachments = new int[colorChannelCount];
    }

    @Override
    public int[] getColorAttachments() {
        return colorAttachments;
    }

    @Override
    public void setDepthSupplier(Supplier<Integer> depthSupplier) {
        this.depthSupplier = depthSupplier;
    }

    @Override
    public void useMipmap() {
        this.useMipmap = true;
    }

    @Override
    public int getOutputChannelCount() {
        return colorChannelCount;
    }

    @Override
    public int getCurrentDepthAttachment() {
        return depthAttachment;
    }

    @Override
    public int width() {
        return Minecraft.getInstance().getMainRenderTarget().width;
    }

    @Override
    public int height() {
        return Minecraft.getInstance().getMainRenderTarget().height;
    }

    @Override
    public int fbo() {
        return fbo;
    }

    @Override
    public void init() {
        if (initialized) return;
        initialized = true;
        fbo = GL33.glGenFramebuffers();
        bindFramebuffer();
        prevFBO = 0;
        initColorChannel();
        initDepthChannel();
        
        int status = GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER);
        if (status != GL33.GL_FRAMEBUFFER_COMPLETE) {
            KoniavacraftMod.LOGGER.error("Failed to bind framebuffer {}", fbo);
            depthAttachment = depthSupplier.get();
        }
        reset();
        if (output == null) {
            output = new FramePipeChannels();
            for (int i = 0; i < colorChannelCount; i++) {
                final int idx = i;
                output.addChannel(() -> colorAttachments[idx]);
            }
        }
    }

    @Override
    public void bindFramebuffer() {
        prevFBO = GL33.glGetInteger(GL33.GL_FRAMEBUFFER_BINDING);
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, fbo);
    }

    @Override
    public void clear(int bit) {
        GL33.glClear(bit);
    }

    @Override
    public void clear() {
        clear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void setTextureFilterMod(int mod) {
        this.textureFilterMod = mod;
    }

    @Override
    public PipeChannels outputChannels() {
        return output;
    }

    @Override
    public void writeFrameBufferWith(Consumer<GlFrameBuffer> writeScope) {
        if (fbo == 0) {
            initialized = false;
            init();
            return;
        }
        bindFramebuffer();
        clear(GL33.GL_COLOR_BUFFER_BIT);
        writeScope.accept(this);
        reset();
    }

    @Override
    public void readFrameBufferWith(Consumer<GlFrameBuffer> readScope) {
        if (fbo == 0) return;
        int zero = GL33.GL_TEXTURE0;
        int prevActive = GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);
        int prevTexture = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        
        for (int i = 0; i < colorChannelCount; i++) {
            GL33.glActiveTexture(zero + i);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, colorAttachments[i]);
        }
        readScope.accept(this);
        GL33.glActiveTexture(prevActive);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, prevTexture);
    }

    @Override
    public void reset() {
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, prevFBO);
    }

    @Override
    public void release() {
        if (!initialized) return;
        GL33.glDeleteFramebuffers(fbo);
        for (int tex : colorAttachments) GL33.glDeleteTextures(tex);
    }

    @Override
    public void copyDepthBuffer(int srcFBO) {
        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();
        GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER, srcFBO);
        GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER, fbo);
        GL33.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL33.GL_DEPTH_BUFFER_BIT, GL33.GL_NEAREST);
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void resize(int width, int height) {
        release();
        initialized = false;
        init();
    }

    private void initColorChannel() {
        for (int i = 0; i < colorChannelCount; i++) {
            int texture = GL33.glGenTextures();
            colorAttachments[i] = texture;
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, texture);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA16F, width(), height(), 0, GL33.GL_RGBA, GL33.GL_FLOAT, (ByteBuffer) null);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, textureFilterMod);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, textureFilterMod);
            GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0 + i, GL33.GL_TEXTURE_2D, texture, 0);
        }
    }

    private void initDepthChannel() {
        int get = depthSupplier.get();
        if (get == -1) {
            depthAttachment = GL33.glGenTextures();
            newDepth = true;
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, depthAttachment);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_DEPTH_COMPONENT, width(), height(), 0, GL33.GL_DEPTH_COMPONENT, GL33.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        } else {
            depthAttachment = get;
        }
        GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_ATTACHMENT, GL33.GL_TEXTURE_2D, depthAttachment, 0);
    }
}