package com.github.nalamodikk.experimental.effects;

import com.github.nalamodikk.experimental.render.effects.MagicCircleRenderData;
import com.github.nalamodikk.experimental.render.effects.MagicCircleRenderQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/**
 * 魔法陣效果實現類
 * 
 * 負責渲染旋轉的符文魔法陣，包含發光效果和動畫
 */
@OnlyIn(Dist.CLIENT)
public class MagicCircleEffect extends MagicEffect {
    
    private final BlockPos position;
    private final Vec3 offset;
    private final int color;
    private final float size;
    private final float rotationSpeed;
    private final boolean glowEffect;
    private final float alpha;
    private final ResourceLocation runeTexture;
    private final boolean persistant;
    private final Consumer<MagicCircleEffect> onCompleteCallback;
    private final Consumer<MagicCircleEffect> onTickCallback;
    
    // 動畫狀態
    private float currentRotation = 0.0f;
    private float currentAlpha;
    private float currentSize;
    
    public MagicCircleEffect(BlockPos position, Vec3 offset, int color, float size, 
                             float rotationSpeed, int duration, boolean glowEffect, float alpha,
                             ResourceLocation runeTexture, boolean persistant,
                             Consumer<MagicCircleEffect> onComplete, Consumer<MagicCircleEffect> onTick) {
        super(persistant ? Integer.MAX_VALUE : duration);
        
        this.position = position;
        this.offset = offset != null ? offset : Vec3.ZERO;
        this.color = color;
        this.size = size;
        this.rotationSpeed = rotationSpeed;
        this.glowEffect = glowEffect;
        this.alpha = alpha;
        this.runeTexture = runeTexture;
        this.persistant = persistant;
        this.onCompleteCallback = onComplete;
        this.onTickCallback = onTick;
        
        // 初始化動畫狀態
        this.currentAlpha = 0.0f;
        this.currentSize = 0.0f;
    }
    
    @Override
    protected void onTick() {
        // 更新旋轉
        currentRotation += rotationSpeed;
        if (currentRotation > Math.PI * 2) {
            currentRotation -= Math.PI * 2;
        }
        
        // 計算淡入淡出
        float progress = getProgress();
        if (progress < 0.1f) {
            // 淡入階段 (前 10%)
            float fadeInProgress = progress / 0.1f;
            currentAlpha = alpha * fadeInProgress;
            currentSize = size * fadeInProgress;
        } else if (progress > 0.9f && !persistant) {
            // 淡出階段 (後 10%)
            float fadeOutProgress = (1.0f - progress) / 0.1f;
            currentAlpha = alpha * fadeOutProgress;
            currentSize = size * fadeOutProgress;
        } else {
            // 穩定階段
            currentAlpha = alpha;
            currentSize = size;
        }
        
        // 調用自定義 tick 回調
        if (onTickCallback != null) {
            onTickCallback.accept(this);
        }
    }
    
    @Override
    protected void onComplete() {
        if (onCompleteCallback != null) {
            onCompleteCallback.accept(this);
        }
    }
    
    @Override
    public void render(float partialTicks) {
        if (currentAlpha <= 0.0f || currentSize <= 0.0f) {
            return;
        }
        
        // 計算實際渲染位置
        Vec3 renderPos = Vec3.atCenterOf(position).add(offset);
        
        // 計算插值旋轉
        float interpolatedRotation = currentRotation + (rotationSpeed * partialTicks);
        
        // 調用實際的渲染器
        renderMagicCircle(renderPos, interpolatedRotation, currentSize, color, currentAlpha, glowEffect, runeTexture, partialTicks);
    }
    
    /**
     * 實際的魔法陣渲染方法 - 調用 MagicCircleRenderer
     */
    private void renderMagicCircle(Vec3 pos, float rotation, float size, int color, float alpha, boolean glow, ResourceLocation texture, float partialTicks) {
        // 實際的渲染邏輯將在渲染事件中處理
        // 這裡我們將渲染數據存儲起來，在渲染事件中統一處理
        MagicCircleRenderData renderData = new MagicCircleRenderData(
            pos, rotation, size, color, alpha, glow, texture, partialTicks
        );
        
        // 將渲染數據添加到渲染隊列
        MagicCircleRenderQueue.getInstance().addRenderData(renderData);
    }
    
    // ========== Getter 方法 ==========
    
    public BlockPos getPosition() {
        return position;
    }
    
    public Vec3 getOffset() {
        return offset;
    }
    
    public Vec3 getRenderPosition() {
        return Vec3.atCenterOf(position).add(offset);
    }
    
    public int getColor() {
        return color;
    }
    
    public float getSize() {
        return size;
    }
    
    public float getCurrentSize() {
        return currentSize;
    }
    
    public float getRotationSpeed() {
        return rotationSpeed;
    }
    
    public float getCurrentRotation() {
        return currentRotation;
    }
    
    public boolean hasGlowEffect() {
        return glowEffect;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public float getCurrentAlpha() {
        return currentAlpha;
    }
    
    public ResourceLocation getRuneTexture() {
        return runeTexture;
    }
    
    public boolean isPersistant() {
        return persistant;
    }
    
    @Override
    public boolean isFinished() {
        return !persistant && super.isFinished();
    }
    
    @Override
    public String getEffectType() {
        return "MagicCircle";
    }
    
    @Override
    public String getDebugInfo() {
        return String.format("%s [Pos: %s, Size: %.2f/%.2f, Rotation: %.2f°, Alpha: %.2f/%.2f]", 
            super.getDebugInfo(), position.toShortString(), currentSize, size, 
            Math.toDegrees(currentRotation), currentAlpha, alpha);
    }
}