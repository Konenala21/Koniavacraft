package com.github.nalamodikk.experimental.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 能量射線效果實現類
 * 
 * 負責渲染能量射線，支援動態位置、脈衝效果和粒子軌跡
 */
@OnlyIn(Dist.CLIENT)
public class BeamEffect extends MagicEffect {
    
    private Vec3 startPos;
    private Vec3 endPos;
    private final Supplier<Vec3> dynamicStart;
    private final Supplier<Vec3> dynamicEnd;
    private final Entity startEntity;
    private final Entity endEntity;
    private final int color;
    private final float thickness;
    private final boolean pulseEffect;
    private final float pulseSpeed;
    private final boolean fadeInOut;
    private final float alpha;
    private final int segments;
    private final boolean animated;
    private final float animationSpeed;
    private final boolean particleTrail;
    private final Consumer<BeamEffect> onCompleteCallback;
    private final Consumer<BeamEffect> onTickCallback;
    
    // 動畫狀態
    private float currentAlpha;
    private float currentThickness;
    private float pulsePhase = 0.0f;
    private float animationPhase = 0.0f;
    
    public BeamEffect(Vec3 startPos, Vec3 endPos, Supplier<Vec3> dynamicStart, Supplier<Vec3> dynamicEnd,
                      Entity startEntity, Entity endEntity, int color, float thickness, int duration,
                      boolean pulseEffect, float pulseSpeed, boolean fadeInOut, float alpha, int segments,
                      boolean animated, float animationSpeed, boolean particleTrail,
                      Consumer<BeamEffect> onComplete, Consumer<BeamEffect> onTick) {
        super(duration);
        
        this.startPos = startPos;
        this.endPos = endPos;
        this.dynamicStart = dynamicStart;
        this.dynamicEnd = dynamicEnd;
        this.startEntity = startEntity;
        this.endEntity = endEntity;
        this.color = color;
        this.thickness = thickness;
        this.pulseEffect = pulseEffect;
        this.pulseSpeed = pulseSpeed;
        this.fadeInOut = fadeInOut;
        this.alpha = alpha;
        this.segments = segments;
        this.animated = animated;
        this.animationSpeed = animationSpeed;
        this.particleTrail = particleTrail;
        this.onCompleteCallback = onComplete;
        this.onTickCallback = onTick;
        
        // 初始化動畫狀態
        this.currentAlpha = fadeInOut ? 0.0f : alpha;
        this.currentThickness = fadeInOut ? 0.0f : thickness;
    }
    
    @Override
    protected void onTick() {
        // 更新動態位置
        updatePositions();
        
        // 更新動畫相位
        if (animated) {
            animationPhase += animationSpeed;
            if (animationPhase > Math.PI * 2) {
                animationPhase -= Math.PI * 2;
            }
        }
        
        // 更新脈衝相位
        if (pulseEffect) {
            pulsePhase += pulseSpeed;
            if (pulsePhase > Math.PI * 2) {
                pulsePhase -= Math.PI * 2;
            }
        }
        
        // 計算淡入淡出
        if (fadeInOut) {
            float progress = getProgress();
            if (progress < 0.2f) {
                // 淡入階段 (前 20%)
                float fadeInProgress = progress / 0.2f;
                currentAlpha = alpha * fadeInProgress;
                currentThickness = thickness * fadeInProgress;
            } else if (progress > 0.8f) {
                // 淡出階段 (後 20%)
                float fadeOutProgress = (1.0f - progress) / 0.2f;
                currentAlpha = alpha * fadeOutProgress;
                currentThickness = thickness * fadeOutProgress;
            } else {
                // 穩定階段
                currentAlpha = alpha;
                currentThickness = thickness;
            }
        } else {
            currentAlpha = alpha;
            currentThickness = thickness;
        }
        
        // 應用脈衝效果
        if (pulseEffect) {
            float pulseFactor = 0.7f + 0.3f * (float) Math.sin(pulsePhase);
            currentAlpha *= pulseFactor;
            currentThickness *= pulseFactor;
        }
        
        // 調用自定義 tick 回調
        if (onTickCallback != null) {
            onTickCallback.accept(this);
        }
    }
    
    private void updatePositions() {
        // 更新動態起始位置
        if (dynamicStart != null) {
            startPos = dynamicStart.get();
        } else if (startEntity != null && !startEntity.isRemoved()) {
            startPos = startEntity.position().add(0, startEntity.getBbHeight() * 0.5, 0);
        }
        
        // 更新動態結束位置
        if (dynamicEnd != null) {
            endPos = dynamicEnd.get();
        } else if (endEntity != null && !endEntity.isRemoved()) {
            endPos = endEntity.position().add(0, endEntity.getBbHeight() * 0.5, 0);
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
        if (currentAlpha <= 0.0f || currentThickness <= 0.0f || startPos == null || endPos == null) {
            return;
        }
        
        // 計算插值動畫相位
        float interpolatedAnimationPhase = animationPhase + (animated ? animationSpeed * partialTicks : 0);
        
        // TODO: 實現實際的渲染邏輯
        // 這裡應該調用 BeamRenderer 來進行實際渲染
        renderBeam(startPos, endPos, currentThickness, color, currentAlpha, segments, 
                  interpolatedAnimationPhase, particleTrail);
    }
    
    /**
     * 實際的射線渲染方法 - 將由渲染器實現
     */
    private void renderBeam(Vec3 start, Vec3 end, float thickness, int color, float alpha, 
                           int segments, float animationPhase, boolean particles) {
        // TODO: 調用實際的渲染器
        // BeamRenderer.render(start, end, thickness, color, alpha, segments, animationPhase, particles);
    }
    
    @Override
    public boolean shouldRender() {
        // 檢查位置是否有效
        if (startPos == null || endPos == null) {
            return false;
        }
        
        // 檢查實體是否仍然存在
        if (startEntity != null && startEntity.isRemoved()) {
            return false;
        }
        if (endEntity != null && endEntity.isRemoved()) {
            return false;
        }
        
        return super.shouldRender();
    }
    
    // ========== Getter 方法 ==========
    
    public Vec3 getStartPos() {
        return startPos;
    }
    
    public Vec3 getEndPos() {
        return endPos;
    }
    
    public Entity getStartEntity() {
        return startEntity;
    }
    
    public Entity getEndEntity() {
        return endEntity;
    }
    
    public int getColor() {
        return color;
    }
    
    public float getThickness() {
        return thickness;
    }
    
    public float getCurrentThickness() {
        return currentThickness;
    }
    
    public boolean hasPulseEffect() {
        return pulseEffect;
    }
    
    public float getPulseSpeed() {
        return pulseSpeed;
    }
    
    public float getPulsePhase() {
        return pulsePhase;
    }
    
    public boolean hasFadeInOut() {
        return fadeInOut;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public float getCurrentAlpha() {
        return currentAlpha;
    }
    
    public int getSegments() {
        return segments;
    }
    
    public boolean isAnimated() {
        return animated;
    }
    
    public float getAnimationSpeed() {
        return animationSpeed;
    }
    
    public float getAnimationPhase() {
        return animationPhase;
    }
    
    public boolean hasParticleTrail() {
        return particleTrail;
    }
    
    /**
     * 獲取射線長度
     * @return 射線長度
     */
    public double getLength() {
        if (startPos == null || endPos == null) {
            return 0.0;
        }
        return startPos.distanceTo(endPos);
    }
    
    /**
     * 獲取射線方向向量 (已標準化)
     * @return 方向向量
     */
    public Vec3 getDirection() {
        if (startPos == null || endPos == null) {
            return Vec3.ZERO;
        }
        return endPos.subtract(startPos).normalize();
    }
    
    @Override
    public String getEffectType() {
        return "Beam";
    }
    
    @Override
    public String getDebugInfo() {
        return String.format("%s [Start: %s, End: %s, Length: %.2f, Thickness: %.2f/%.2f, Alpha: %.2f/%.2f]", 
            super.getDebugInfo(), 
            startPos != null ? String.format("(%.1f,%.1f,%.1f)", startPos.x, startPos.y, startPos.z) : "null",
            endPos != null ? String.format("(%.1f,%.1f,%.1f)", endPos.x, endPos.y, endPos.z) : "null",
            getLength(), currentThickness, thickness, currentAlpha, alpha);
    }
}