package com.github.nalamodikk.particle.render.shader.api.glsl;

import com.github.nalamodikk.particle.render.shader.api.pipe.PipeChannels;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface GlFrameBuffer {

    int[] getColorAttachments();

    /**
     * ?蹎冪 NeoForge ?????????券?????選????迎????秋撮???∴??     * ???????秋??擗?? -1
     */
    void setDepthSupplier(Supplier<Integer> depthSupplier);

    /**
     * ?賹? mipmap
     */
    void useMipmap();

    int getOutputChannelCount();

    int getCurrentDepthAttachment();

    int width();

    int height();

    int fbo();

    void init();

    void bindFramebuffer();

    void clear(int bit);

    void clear();

    void setTextureFilterMod(int mod);

    /**
     * ????岳?曇????謍?
     */
    PipeChannels outputChannels();

    /**
     * ??FrameBuffer ??蟡??     */
    void writeFrameBufferWith(Consumer<GlFrameBuffer> writeScope);

    /**
     * ???FrameBuffer ??寞?????蝞?????隡?
     */
    void readFrameBufferWith(Consumer<GlFrameBuffer> readScope);

    void reset();

    void release();

    void copyDepthBuffer(int srcFBO);

    void resize(int width, int height);
}
