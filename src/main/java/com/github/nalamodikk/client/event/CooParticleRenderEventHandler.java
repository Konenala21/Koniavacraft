package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.render.shader.CooShaderProgramManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 處理粒子渲染相關的事件
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class CooParticleRenderEventHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 我們在 AFTER_PARTICLES 階段更新矩陣，雖然這主要用於下一幀或後續渲染
        // 但更好的做法是在渲染前捕捉。NeoForge 的 RenderLevelStageEvent 會在多個階段觸發。
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            Matrix4f projectionMatrix = event.getProjectionMatrix();
            Matrix4f modelViewMatrix = event.getModelViewMatrix();
            
            CooShaderProgramManager.getInstance().updateMatrices(projectionMatrix, modelViewMatrix);
        }
    }
}
