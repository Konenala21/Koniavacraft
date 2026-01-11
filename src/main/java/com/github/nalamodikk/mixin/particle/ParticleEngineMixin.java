package com.github.nalamodikk.mixin.particle;

import com.github.nalamodikk.particle.CooParticleRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader; // Import BufferUploader
import com.mojang.blaze3d.vertex.MeshData; // Import MeshData
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * 注入自定義粒子渲染邏輯
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Shadow
    @Final
    private Map<ParticleRenderType, Queue<Particle>> particles;

    @Shadow
    @Final
    protected TextureManager textureManager;

    /**
     * 在原版渲染結束後，手動渲染我們的自定義類型
     * 使用 javap 確認的正確簽名
     */
    @Inject(
        method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
        at = @At("RETURN")
    )
    private void cooParticlesAPI$renderCustomTypes(LightTexture lightTexture, Camera camera, float partialTick, Frustum clippingHelper, Predicate<ParticleRenderType> renderTypePredicate, CallbackInfo ci) {
        RenderSystem.enableDepthTest();
        
        // 嘗試最簡單的渲染：
        renderCustomType(CooParticleRenderTypes.ADDITIVE_BLEND, camera, partialTick, lightTexture);
        renderCustomType(CooParticleRenderTypes.TRANSLUCENT, camera, partialTick, lightTexture);
        renderCustomType(CooParticleRenderTypes.GLOW, camera, partialTick, lightTexture);
    }

    private void renderCustomType(ParticleRenderType type, Camera camera, float partialTick, LightTexture lightTexture) {
        Queue<Particle> queue = this.particles.get(type);
        if (queue == null || queue.isEmpty()) return;

        RenderSystem.setShader(GameRenderer::getParticleShader);
        Tesselator tesselator = Tesselator.getInstance();
        
        // 呼叫 begin，這會設置混合模式、深度測試等
        BufferBuilder buffer = type.begin(tesselator, this.textureManager);

        for (Particle particle : queue) {
            try {
                particle.render(buffer, camera, partialTick);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // 結束繪製
        MeshData meshData = buffer.buildOrThrow();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
    }
}
