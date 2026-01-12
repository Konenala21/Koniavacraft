package com.github.nalamodikk.render.shader.pipe;

import net.minecraft.resources.ResourceLocation;

/**
 * Shader 渲染管道基類
 * 負責管理自定義 FrameBuffer (FBO) 與 Shader 的渲染流程
 */
public abstract class ShaderPipe {
    protected final ResourceLocation id;
    protected int width, height;

    public ShaderPipe(ResourceLocation id) {
        this.id = id;
    }

    /**
     * 當視窗大小改變時重置緩衝區
     */
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        onResize();
    }

    protected abstract void onResize();

    /**
     * 釋放資源
     */
    public abstract void release();

    /**
     * 渲染前的準備 (如綁定 FBO)
     */
    public abstract void preRender();

    /**
     * 執行渲染
     */
    public abstract void render(float partialTick);

    /**
     * 渲染後的清理 (如解除綁定並繪製到主屏)
     */
    public abstract void postRender();

    public ResourceLocation getId() {
        return id;
    }
}
