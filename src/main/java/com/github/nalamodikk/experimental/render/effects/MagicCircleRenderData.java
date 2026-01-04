package com.github.nalamodikk.experimental.render.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 魔法陣渲染數據
 * 
 * 存儲單個魔法陣的所有渲染參數
 */
@OnlyIn(Dist.CLIENT)
public class MagicCircleRenderData {
    
    public final Vec3 position;
    public final float rotation;
    public final float size;
    public final int color;
    public final float alpha;
    public final boolean glowEffect;
    public final ResourceLocation runeTexture;
    public final float partialTicks;
    
    public MagicCircleRenderData(Vec3 position, float rotation, float size, int color, 
                                float alpha, boolean glowEffect, ResourceLocation runeTexture, 
                                float partialTicks) {
        this.position = position;
        this.rotation = rotation;
        this.size = size;
        this.color = color;
        this.alpha = alpha;
        this.glowEffect = glowEffect;
        this.runeTexture = runeTexture;
        this.partialTicks = partialTicks;
    }
    
    /**
     * 檢查是否應該渲染此魔法陣
     */
    public boolean shouldRender() {
        return alpha > 0.0f && size > 0.0f && 
               MagicCircleRenderer.shouldRender(position, size);
    }
    
    @Override
    public String toString() {
        return String.format("MagicCircleRenderData[pos=%s, rot=%.2f, size=%.2f, alpha=%.2f]", 
            position, Math.toDegrees(rotation), size, alpha);
    }
}