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
 * ????Ｘ???殉???????
 *
 * ?﹝??蹎??????dditive Blending?隤?嚗??瞉????
 */
public class CooParticleRenderTypes {

    /**
     * ?蹎???????遴竣?
     *
     * ?輯撒??GL_ONE, GL_ONE ?????
     * ????遴??????魂??閰????賹赤?????
     */
    public static final ParticleRenderType ADDITIVE_BLEND = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            // ?秋撒????????謖?
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

            // ?賹????
            RenderSystem.enableBlend();

            // ?桀???蹎???????庖L_ONE + GL_ONE
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE
            );

            // ?啾播???撞?????殉?????????????
            RenderSystem.depthMask(false);

            // ????踝???蹇?
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
     * ????????????遴竣?????蝞????
     *
     * ?輯撒??GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA ?????
     */
    public static final ParticleRenderType TRANSLUCENT = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();

            // ?????????
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
     * ??堊赤??斗紗????荒???蹎???倦???其??
     *
     * ?輯撒??GL_SRC_ALPHA, GL_ONE ?????
     * ??????秋撒?????選???秋播謒??????
     */
    public static final ParticleRenderType GLOW = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();

            // ??堊赤??斗紗??
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
