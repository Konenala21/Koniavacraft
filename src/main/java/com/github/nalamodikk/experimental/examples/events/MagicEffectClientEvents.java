package com.github.nalamodikk.experimental.examples.events;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.experimental.effects.MagicEffectRegistry;
import com.github.nalamodikk.experimental.render.effects.MagicCircleRenderQueue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 魔法效果客戶端事件處理器
 * 
 * 負責整合 MagicEffect API 到 Koniavacraft 的事件系統中
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID,  value = Dist.CLIENT)
public class MagicEffectClientEvents {
    
    /**
     * 客戶端 tick 事件 - 更新所有魔法效果
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        try {
            // 更新魔法效果註冊表
            MagicEffectRegistry.getInstance().tick();
        } catch (Exception e) {
            // 記錄錯誤但不讓遊戲崩潰
            System.err.println("Error updating magic effects: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 渲染關卡階段事件 - 在適當的時機渲染魔法效果
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 在透明物體渲染階段渲染魔法效果
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            try {
                // 準備魔法陣渲染
                MagicCircleRenderQueue.getInstance().prepareRender();
                
                // 獲取正確的 partialTicks 值
                float partialTicks = event.getPartialTick().getGameTimeDeltaTicks();
                
                // 更新所有魔法效果 (這會向渲染隊列添加數據)
                MagicEffectRegistry.getInstance().render(partialTicks);
                
                // 執行魔法陣渲染
                Minecraft mc = Minecraft.getInstance();
                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                
                MagicCircleRenderQueue.getInstance().executeRender(poseStack, bufferSource, partialTicks);
                
                // 確保緩衝區被提交
                bufferSource.endBatch();
                
            } catch (Exception e) {
                // 記錄渲染錯誤但不讓遊戲崩潰
                System.err.println("Error rendering magic effects: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}