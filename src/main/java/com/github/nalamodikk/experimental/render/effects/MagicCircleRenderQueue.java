package com.github.nalamodikk.experimental.render.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 魔法陣渲染隊列
 * 
 * 管理所有需要渲染的魔法陣數據，在適當的渲染階段統一處理
 */
@OnlyIn(Dist.CLIENT)
public class MagicCircleRenderQueue {
    
    private static final MagicCircleRenderQueue INSTANCE = new MagicCircleRenderQueue();
    
    private final List<MagicCircleRenderData> renderQueue = new CopyOnWriteArrayList<>();
    private final List<MagicCircleRenderData> pendingRenders = new ArrayList<>();
    
    private MagicCircleRenderQueue() {
        // 單例模式
    }
    
    /**
     * 獲取渲染隊列實例
     */
    public static MagicCircleRenderQueue getInstance() {
        return INSTANCE;
    }
    
    /**
     * 添加渲染數據到隊列
     */
    public void addRenderData(MagicCircleRenderData renderData) {
        if (renderData != null && renderData.shouldRender()) {
            synchronized (pendingRenders) {
                pendingRenders.add(renderData);
            }
        }
    }
    
    /**
     * 準備渲染 - 在渲染事件開始時調用
     */
    public void prepareRender() {
        // 將待渲染的數據移動到渲染隊列
        synchronized (pendingRenders) {
            if (!pendingRenders.isEmpty()) {
                renderQueue.addAll(pendingRenders);
                pendingRenders.clear();
            }
        }
    }
    
    /**
     * 執行渲染 - 在適當的渲染階段調用
     */
    public void executeRender(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        if (renderQueue.isEmpty()) {
            return;
        }
        
        // 渲染所有魔法陣
        for (MagicCircleRenderData renderData : renderQueue) {
            try {
                if (renderData.shouldRender()) {
                    MagicCircleRenderer.render(
                        poseStack, bufferSource, 
                        renderData.position, renderData.rotation, renderData.size,
                        renderData.color, renderData.alpha, renderData.glowEffect,
                        renderData.runeTexture, partialTicks
                    );
                }
            } catch (Exception e) {
                // 記錄錯誤但繼續渲染其他魔法陣
                System.err.println("Error rendering magic circle: " + e.getMessage());
            }
        }
        
        // 清理渲染隊列
        renderQueue.clear();
    }
    
    /**
     * 清理所有渲染數據
     */
    public void clear() {
        renderQueue.clear();
        synchronized (pendingRenders) {
            pendingRenders.clear();
        }
    }
    
    /**
     * 獲取待渲染的魔法陣數量
     */
    public int getPendingRenderCount() {
        return renderQueue.size();
    }
    
    /**
     * 獲取調試信息
     */
    public String getDebugInfo() {
        return String.format("Magic Circle Queue: %d pending, %d in queue", 
            pendingRenders.size(), renderQueue.size());
    }
}