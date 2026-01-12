package com.github.nalamodikk.particle.render;

import com.github.nalamodikk.particle.render.shader.CooShaderProgramManager;
import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * ????????Shader ????殉??????? */
public class CooShaderRenderType implements ParticleRenderType {
    private final ResourceLocation shaderId;

    public CooShaderRenderType(ResourceLocation shaderId) {
        this.shaderId = shaderId;
    }

    @Override
    public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
        CooShaderProgram program = CooShaderProgramManager.getInstance().getProgram(shaderId);
        
        if (program != null) {
            program.use();
            CooShaderProgramManager.getInstance().applyMatrices(program);
        } else {
            // ?豯阬??????Shader
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
        }

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);

        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }

    @Override
    public String toString() {
        return "COO_SHADER_" + shaderId.toString();
    }

    @Override
    public boolean isTranslucent() {
        return true;
    }
}
