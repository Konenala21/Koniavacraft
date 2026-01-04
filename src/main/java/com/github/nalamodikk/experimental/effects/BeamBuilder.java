package com.github.nalamodikk.experimental.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 能量射線效果建構器 - 使用流暢 API 模式構建能量射線
 * 
 * 示例用法：
 * <pre>
 * MagicEffectAPI.createBeam()
 *     .from(startPos)
 *     .to(endPos)
 *     .withColor(0xFF00FFFF) // 青色
 *     .withThickness(0.3f)
 *     .withPulseEffect(true)
 *     .withDuration(80)
 *     .spawn(level);
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class BeamBuilder {
    
    private Vec3 startPos;
    private Vec3 endPos;
    private Supplier<Vec3> dynamicStart;
    private Supplier<Vec3> dynamicEnd;
    private Entity startEntity;
    private Entity endEntity;
    private int color = 0xFF00FFFF; // 預設青色
    private float thickness = 0.2f;
    private int duration = 60;
    private boolean pulseEffect = false;
    private float pulseSpeed = 1.0f;
    private boolean fadeInOut = true;
    private float alpha = 1.0f;
    private int segments = 16;
    private boolean animated = true;
    private float animationSpeed = 1.0f;
    private boolean particleTrail = false;
    private Consumer<BeamEffect> onComplete;
    private Consumer<BeamEffect> onTick;
    
    BeamBuilder() {
        // 套件私有構造函數
    }
    
    /**
     * 設置射線起始位置
     * @param pos 起始位置
     * @return 建構器實例
     */
    public BeamBuilder from(Vec3 pos) {
        this.startPos = pos;
        this.dynamicStart = null;
        this.startEntity = null;
        return this;
    }
    
    /**
     * 設置射線起始位置
     * @param x X 座標
     * @param y Y 座標
     * @param z Z 座標
     * @return 建構器實例
     */
    public BeamBuilder from(double x, double y, double z) {
        return from(new Vec3(x, y, z));
    }
    
    /**
     * 設置射線起始位置 (方塊位置)
     * @param pos 方塊位置
     * @return 建構器實例
     */
    public BeamBuilder from(BlockPos pos) {
        return from(Vec3.atCenterOf(pos));
    }
    
    /**
     * 設置動態起始位置 (每 tick 更新)
     * @param positionSupplier 位置提供者
     * @return 建構器實例
     */
    public BeamBuilder fromDynamic(Supplier<Vec3> positionSupplier) {
        this.dynamicStart = positionSupplier;
        this.startPos = null;
        this.startEntity = null;
        return this;
    }
    
    /**
     * 從實體位置開始射線
     * @param entity 起始實體
     * @return 建構器實例
     */
    public BeamBuilder fromEntity(Entity entity) {
        this.startEntity = entity;
        this.startPos = null;
        this.dynamicStart = null;
        return this;
    }
    
    /**
     * 設置射線目標位置
     * @param pos 目標位置
     * @return 建構器實例
     */
    public BeamBuilder to(Vec3 pos) {
        this.endPos = pos;
        this.dynamicEnd = null;
        this.endEntity = null;
        return this;
    }
    
    /**
     * 設置射線目標位置
     * @param x X 座標
     * @param y Y 座標
     * @param z Z 座標
     * @return 建構器實例
     */
    public BeamBuilder to(double x, double y, double z) {
        return to(new Vec3(x, y, z));
    }
    
    /**
     * 設置射線目標位置 (方塊位置)
     * @param pos 方塊位置
     * @return 建構器實例
     */
    public BeamBuilder to(BlockPos pos) {
        return to(Vec3.atCenterOf(pos));
    }
    
    /**
     * 設置動態目標位置 (每 tick 更新)
     * @param positionSupplier 位置提供者
     * @return 建構器實例
     */
    public BeamBuilder toDynamic(Supplier<Vec3> positionSupplier) {
        this.dynamicEnd = positionSupplier;
        this.endPos = null;
        this.endEntity = null;
        return this;
    }
    
    /**
     * 向實體位置發射射線
     * @param entity 目標實體
     * @return 建構器實例
     */
    public BeamBuilder toEntity(Entity entity) {
        this.endEntity = entity;
        this.endPos = null;
        this.dynamicEnd = null;
        return this;
    }
    
    /**
     * 設置射線顏色
     * @param color ARGB 顏色值
     * @return 建構器實例
     */
    public BeamBuilder withColor(int color) {
        this.color = color;
        return this;
    }
    
    /**
     * 設置射線顏色 (RGB + Alpha)
     * @param r 紅色 (0-255)
     * @param g 綠色 (0-255)
     * @param b 藍色 (0-255)
     * @param a 透明度 (0-255)
     * @return 建構器實例
     */
    public BeamBuilder withColor(int r, int g, int b, int a) {
        this.color = (a << 24) | (r << 16) | (g << 8) | b;
        return this;
    }
    
    /**
     * 設置射線粗細
     * @param thickness 粗細 (方塊單位)
     * @return 建構器實例
     */
    public BeamBuilder withThickness(float thickness) {
        this.thickness = Math.max(0.01f, thickness);
        return this;
    }
    
    /**
     * 設置持續時間
     * @param ticks 持續的 tick 數量
     * @return 建構器實例
     */
    public BeamBuilder withDuration(int ticks) {
        this.duration = Math.max(1, ticks);
        return this;
    }
    
    /**
     * 設置是否有脈衝效果
     * @param pulse 是否脈衝
     * @return 建構器實例
     */
    public BeamBuilder withPulseEffect(boolean pulse) {
        this.pulseEffect = pulse;
        return this;
    }
    
    /**
     * 設置脈衝速度
     * @param speed 脈衝速度倍數
     * @return 建構器實例
     */
    public BeamBuilder withPulseSpeed(float speed) {
        this.pulseSpeed = Math.max(0.1f, speed);
        return this;
    }
    
    /**
     * 設置是否有淡入淡出效果
     * @param fadeInOut 是否淡入淡出
     * @return 建構器實例
     */
    public BeamBuilder withFadeInOut(boolean fadeInOut) {
        this.fadeInOut = fadeInOut;
        return this;
    }
    
    /**
     * 設置透明度
     * @param alpha 透明度 (0.0 - 1.0)
     * @return 建構器實例
     */
    public BeamBuilder withAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        return this;
    }
    
    /**
     * 設置射線分段數量 (影響平滑度)
     * @param segments 分段數量
     * @return 建構器實例
     */
    public BeamBuilder withSegments(int segments) {
        this.segments = Math.max(4, segments);
        return this;
    }
    
    /**
     * 設置是否有動畫效果
     * @param animated 是否動畫
     * @return 建構器實例
     */
    public BeamBuilder withAnimation(boolean animated) {
        this.animated = animated;
        return this;
    }
    
    /**
     * 設置動畫速度
     * @param speed 動畫速度倍數
     * @return 建構器實例
     */
    public BeamBuilder withAnimationSpeed(float speed) {
        this.animationSpeed = Math.max(0.1f, speed);
        return this;
    }
    
    /**
     * 設置是否有粒子軌跡
     * @param trail 是否有粒子軌跡
     * @return 建構器實例
     */
    public BeamBuilder withParticleTrail(boolean trail) {
        this.particleTrail = trail;
        return this;
    }
    
    /**
     * 設置完成回調
     * @param callback 當效果結束時調用的回調
     * @return 建構器實例
     */
    public BeamBuilder onComplete(Consumer<BeamEffect> callback) {
        this.onComplete = callback;
        return this;
    }
    
    /**
     * 設置每 tick 回調
     * @param callback 每 tick 調用的回調
     * @return 建構器實例
     */
    public BeamBuilder onTick(Consumer<BeamEffect> callback) {
        this.onTick = callback;
        return this;
    }
    
    /**
     * 創建並在世界中生成射線效果
     * @param level 世界實例
     * @return 創建的效果實例，如果創建失敗則返回 null
     */
    public BeamEffect spawn(Level level) {
        if (getStartPosition() == null || getEndPosition() == null) {
            throw new IllegalStateException("Both start and end positions must be set before spawning beam");
        }
        
        if (!level.isClientSide) {
            return null; // 只在客戶端渲染
        }
        
        // TODO: 實現實際的射線創建邏輯
        BeamEffect effect = new BeamEffect(
            startPos, endPos, dynamicStart, dynamicEnd, startEntity, endEntity,
            color, thickness, duration, pulseEffect, pulseSpeed, fadeInOut, alpha,
            segments, animated, animationSpeed, particleTrail, onComplete, onTick
        );
        
        // 註冊到效果管理器
        MagicEffectRegistry.getInstance().registerEffect(effect);
        
        return effect;
    }
    
    /**
     * 創建效果實例但不立即生成
     * @return 創建的效果實例
     */
    public BeamEffect build() {
        if (getStartPosition() == null || getEndPosition() == null) {
            throw new IllegalStateException("Both start and end positions must be set before building beam");
        }
        
        return new BeamEffect(
            startPos, endPos, dynamicStart, dynamicEnd, startEntity, endEntity,
            color, thickness, duration, pulseEffect, pulseSpeed, fadeInOut, alpha,
            segments, animated, animationSpeed, particleTrail, onComplete, onTick
        );
    }
    
    private Vec3 getStartPosition() {
        if (startPos != null) return startPos;
        if (dynamicStart != null) return dynamicStart.get();
        if (startEntity != null) return startEntity.position();
        return null;
    }
    
    private Vec3 getEndPosition() {
        if (endPos != null) return endPos;
        if (dynamicEnd != null) return dynamicEnd.get();
        if (endEntity != null) return endEntity.position();
        return null;
    }
}