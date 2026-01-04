package com.github.nalamodikk.experimental.effects;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 粒子軌跡效果建構器 - 使用流暢 API 模式構建粒子軌跡
 * 
 * 示例用法：
 * <pre>
 * MagicEffectAPI.createParticleTrail()
 *     .along(path)
 *     .withParticle(ParticleTypes.ENCHANT)
 *     .withDensity(5)
 *     .withSpeed(0.1f)
 *     .withLifetime(40)
 *     .spawn(level);
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class ParticleTrailBuilder {
    
    private Vec3[] pathPoints;
    private Supplier<Vec3[]> dynamicPath;
    private Entity followEntity;
    private ParticleOptions particleType = ParticleTypes.ENCHANT;
    private int density = 3;
    private float speed = 0.05f;
    private int lifetime = 30;
    private float spread = 0.1f;
    private int duration = 60;
    private boolean colorGradient = false;
    private int startColor = 0xFFFFFFFF;
    private int endColor = 0xFF0000FF;
    private boolean spiral = false;
    private float spiralRadius = 0.3f;
    private float spiralSpeed = 1.0f;
    private Consumer<ParticleTrailEffect> onComplete;
    
    ParticleTrailBuilder() {
        // 套件私有構造函數
    }
    
    /**
     * 設置粒子軌跡路徑
     * @param points 路徑點陣列
     * @return 建構器實例
     */
    public ParticleTrailBuilder along(Vec3... points) {
        this.pathPoints = points.clone();
        this.dynamicPath = null;
        this.followEntity = null;
        return this;
    }
    
    /**
     * 設置動態路徑 (每 tick 更新)
     * @param pathSupplier 路徑提供者
     * @return 建構器實例
     */
    public ParticleTrailBuilder alongDynamic(Supplier<Vec3[]> pathSupplier) {
        this.dynamicPath = pathSupplier;
        this.pathPoints = null;
        this.followEntity = null;
        return this;
    }
    
    /**
     * 跟隨實體創建軌跡
     * @param entity 要跟隨的實體
     * @return 建構器實例
     */
    public ParticleTrailBuilder followEntity(Entity entity) {
        this.followEntity = entity;
        this.pathPoints = null;
        this.dynamicPath = null;
        return this;
    }
    
    /**
     * 在兩點間創建直線軌跡
     * @param start 起始點
     * @param end 結束點
     * @return 建構器實例
     */
    public ParticleTrailBuilder between(Vec3 start, Vec3 end) {
        return along(start, end);
    }
    
    /**
     * 設置粒子類型
     * @param particle 粒子類型
     * @return 建構器實例
     */
    public ParticleTrailBuilder withParticle(ParticleOptions particle) {
        this.particleType = particle;
        return this;
    }
    
    /**
     * 設置粒子密度 (每個路徑段的粒子數量)
     * @param density 密度
     * @return 建構器實例
     */
    public ParticleTrailBuilder withDensity(int density) {
        this.density = Math.max(1, density);
        return this;
    }
    
    /**
     * 設置粒子移動速度
     * @param speed 速度
     * @return 建構器實例
     */
    public ParticleTrailBuilder withSpeed(float speed) {
        this.speed = Math.max(0.0f, speed);
        return this;
    }
    
    /**
     * 設置粒子生命周期
     * @param ticks 生命周期 (ticks)
     * @return 建構器實例
     */
    public ParticleTrailBuilder withLifetime(int ticks) {
        this.lifetime = Math.max(1, ticks);
        return this;
    }
    
    /**
     * 設置粒子擴散範圍
     * @param spread 擴散半徑
     * @return 建構器實例
     */
    public ParticleTrailBuilder withSpread(float spread) {
        this.spread = Math.max(0.0f, spread);
        return this;
    }
    
    /**
     * 設置效果持續時間
     * @param ticks 持續時間 (ticks)
     * @return 建構器實例
     */
    public ParticleTrailBuilder withDuration(int ticks) {
        this.duration = Math.max(1, ticks);
        return this;
    }
    
    /**
     * 啟用顏色漸變效果
     * @param startColor 起始顏色 (ARGB)
     * @param endColor 結束顏色 (ARGB)
     * @return 建構器實例
     */
    public ParticleTrailBuilder withColorGradient(int startColor, int endColor) {
        this.colorGradient = true;
        this.startColor = startColor;
        this.endColor = endColor;
        return this;
    }
    
    /**
     * 啟用螺旋效果
     * @param radius 螺旋半徑
     * @param speed 螺旋速度
     * @return 建構器實例
     */
    public ParticleTrailBuilder withSpiral(float radius, float speed) {
        this.spiral = true;
        this.spiralRadius = radius;
        this.spiralSpeed = speed;
        return this;
    }
    
    /**
     * 設置完成回調
     * @param callback 當效果結束時調用的回調
     * @return 建構器實例
     */
    public ParticleTrailBuilder onComplete(Consumer<ParticleTrailEffect> callback) {
        this.onComplete = callback;
        return this;
    }
    
    /**
     * 創建並在世界中生成粒子軌跡效果
     * @param level 世界實例
     * @return 創建的效果實例，如果創建失敗則返回 null
     */
    public ParticleTrailEffect spawn(Level level) {
        if (getPath() == null) {
            throw new IllegalStateException("Path must be set before spawning particle trail");
        }
        
        if (!level.isClientSide) {
            return null; // 只在客戶端渲染
        }
        
        // TODO: 實現實際的粒子軌跡創建邏輯
        ParticleTrailEffect effect = new ParticleTrailEffect(
            pathPoints, dynamicPath, followEntity, particleType, density, speed,
            lifetime, spread, duration, colorGradient, startColor, endColor,
            spiral, spiralRadius, spiralSpeed, onComplete
        );
        
        // 註冊到效果管理器
        MagicEffectRegistry.getInstance().registerEffect(effect);
        
        return effect;
    }
    
    /**
     * 創建效果實例但不立即生成
     * @return 創建的效果實例
     */
    public ParticleTrailEffect build() {
        if (getPath() == null) {
            throw new IllegalStateException("Path must be set before building particle trail");
        }
        
        return new ParticleTrailEffect(
            pathPoints, dynamicPath, followEntity, particleType, density, speed,
            lifetime, spread, duration, colorGradient, startColor, endColor,
            spiral, spiralRadius, spiralSpeed, onComplete
        );
    }
    
    private Vec3[] getPath() {
        if (pathPoints != null) return pathPoints;
        if (dynamicPath != null) return dynamicPath.get();
        if (followEntity != null) return new Vec3[]{followEntity.position()};
        return null;
    }
}