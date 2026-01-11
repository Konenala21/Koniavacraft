package com.github.nalamodikk.mixin.particle;

import com.github.nalamodikk.particle.CooParticleRenderTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * 注入自定義粒子渲染類型到原版引擎的渲染循環中
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    /**
     * 在 render 方法中攔截對 RENDER_ORDER 的迭代
     * 這裡我們在方法開始時將 RENDER_ORDER 替換為包含我們自定義類型的列表
     * 註：1.21.1 中 RENDER_ORDER 是靜態常量，
     * 我們攔截 render 方法中對其引用的局部變量。
     */
    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At("STORE"),
        ordinal = 0
    )
    private Iterable<ParticleRenderType> cooParticlesAPI$injectRenderTypes(Iterable<ParticleRenderType> original) {
        if (original instanceof List<ParticleRenderType> list) {
            List<ParticleRenderType> newOrder = new ArrayList<>(list);
            if (!newOrder.contains(CooParticleRenderTypes.ADDITIVE_BLEND)) {
                newOrder.add(CooParticleRenderTypes.ADDITIVE_BLEND);
                newOrder.add(CooParticleRenderTypes.TRANSLUCENT);
                newOrder.add(CooParticleRenderTypes.GLOW);
            }
            return newOrder;
        }
        return original;
    }
}