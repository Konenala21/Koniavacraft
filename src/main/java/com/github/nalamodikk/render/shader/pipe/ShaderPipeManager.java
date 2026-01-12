package com.github.nalamodikk.render.shader.pipe;

import com.github.nalamodikk.render.client.ClientRenderEntityManager;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Shader 管道管理器
 * 負責初始化與維護渲染鏈
 */
public class ShaderPipeManager {
    private static final List<ShaderPipe> ACTIVE_PIPES = new ArrayList<>();

    public static void init() {
        // 註冊預設管道與特效管道
        // TODO: 實作特定的 ShaderPipe 類別
    }

    public static void onResize(int width, int height) {
        for (ShaderPipe pipe : ACTIVE_PIPES) {
            pipe.resize(width, height);
        }
    }

    public static void release() {
        for (ShaderPipe pipe : ACTIVE_PIPES) {
            pipe.release();
        }
        ACTIVE_PIPES.clear();
    }
}
