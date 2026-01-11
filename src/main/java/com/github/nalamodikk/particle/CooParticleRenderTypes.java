package com.github.nalamodikk.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * 自定義粒子渲染類型
 *
 * 實現加法混合（Additive Blending）以產生發光效果
 */
public class CooParticleRenderTypes {

    /**
     * 加法混合渲染類型
     *
     * 使用 GL_ONE, GL_ONE 混合模式
     * 粒子顏色會與背景相加，產生發光效果
     */
    public static final ParticleRenderType ADDITIVE_BLEND = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            // 綁定粒子紋理圖集
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

            // 啟用混合
            RenderSystem.enableBlend();

            // 設置加法混合模式：GL_ONE + GL_ONE
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE
            );

            // 禁用深度寫入（粒子不遮擋背後的物體）
            RenderSystem.depthMask(false);

            // 開始構建頂點
            return tesselator.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.PARTICLE
            );
        }

        @Override
        public String toString() {
            return "KONIAVA_ADDITIVE_BLEND";
        }

        @Override
        public boolean isTranslucent() {
            return true;
        }
    };

    /**
     * 標準透明混合渲染類型（作為備用）
     *
     * 使用 GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA 混合模式
     */
    public static final ParticleRenderType TRANSLUCENT = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();

            // 標準透明混合
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );

            RenderSystem.depthMask(false);

            return tesselator.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.PARTICLE
            );
        }

        @Override
        public String toString() {
            return "KONIAVA_TRANSLUCENT";
        }

        @Override
        public boolean isTranslucent() {
            return true;
        }
    };

    /**
     * 自發光混合（結合加法與透明度）
     *
     * 使用 GL_SRC_ALPHA, GL_ONE 混合模式
     * 適合需要半透明但又要發光的粒子
     */
    public static final ParticleRenderType GLOW = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();

            // 自發光混合
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE
            );

            RenderSystem.depthMask(false);

            return tesselator.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.PARTICLE
            );
        }

        @Override
        public String toString() {
            return "KONIAVA_GLOW";
        }

        @Override
        public boolean isTranslucent() {
            return true;
        }
    };
}
