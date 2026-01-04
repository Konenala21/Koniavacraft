package com.github.nalamodikk.experimental.effects;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 粒子軌跡效果實現類
 * 
 * 負責沿路徑生成粒子軌跡，支援顏色漸變、螺旋效果和動態路徑
 */
@OnlyIn(Dist.CLIENT)
public class ParticleTrailEffect extends MagicEffect {
    
    private Vec3[] pathPoints;
    private final Supplier<Vec3[]> dynamicPath;
    private final Entity followEntity;
    private final ParticleOptions particleType;
    private final int density;
    private final float speed;
    private final int particleLifetime;
    private final float spread;
    private final boolean colorGradient;
    private final int startColor;
    private final int endColor;
    private final boolean spiral;
    private final float spiralRadius;
    private final float spiralSpeed;
    private final Consumer<ParticleTrailEffect> onCompleteCallback;
    
    // 動畫狀態
    private float spiralPhase = 0.0f;
    private int lastSpawnTick = 0;
    private Vec3 lastEntityPosition = null;
    
    public ParticleTrailEffect(Vec3[] pathPoints, Supplier<Vec3[]> dynamicPath, Entity followEntity,
                               ParticleOptions particleType, int density, float speed, int particleLifetime,
                               float spread, int duration, boolean colorGradient, int startColor, int endColor,
                               boolean spiral, float spiralRadius, float spiralSpeed,
                               Consumer<ParticleTrailEffect> onComplete) {
        super(duration);
        
        this.pathPoints = pathPoints;
        this.dynamicPath = dynamicPath;
        this.followEntity = followEntity;
        this.particleType = particleType;
        this.density = density;
        this.speed = speed;
        this.particleLifetime = particleLifetime;
        this.spread = spread;
        this.colorGradient = colorGradient;
        this.startColor = startColor;
        this.endColor = endColor;
        this.spiral = spiral;
        this.spiralRadius = spiralRadius;
        this.spiralSpeed = spiralSpeed;
        this.onCompleteCallback = onComplete;
        
        // 初始化實體位置追蹤
        if (followEntity != null) {
            lastEntityPosition = followEntity.position();
        }
    }
    
    @Override
    protected void onTick() {
        // 更新動態路徑
        updatePath();
        
        // 更新螺旋相位
        if (spiral) {
            spiralPhase += spiralSpeed;
            if (spiralPhase > Math.PI * 2) {
                spiralPhase -= Math.PI * 2;
            }
        }
        
        // 生成粒子
        spawnParticles();
        
        lastSpawnTick = getAge();
    }
    
    private void updatePath() {
        // 更新動態路徑
        if (dynamicPath != null) {
            pathPoints = dynamicPath.get();
        } else if (followEntity != null && !followEntity.isRemoved()) {
            Vec3 currentPos = followEntity.position().add(0, followEntity.getBbHeight() * 0.5, 0);
            
            // 創建軌跡路徑
            if (lastEntityPosition != null) {
                pathPoints = new Vec3[]{lastEntityPosition, currentPos};
            } else {
                pathPoints = new Vec3[]{currentPos};
            }
            
            lastEntityPosition = currentPos;
        }
    }
    
    private void spawnParticles() {
        if (pathPoints == null || pathPoints.length == 0) {
            return;
        }
        
        // 單點路徑
        if (pathPoints.length == 1) {
            spawnParticlesAtPoint(pathPoints[0], 0.0f);
            return;
        }
        
        // 多點路徑
        for (int i = 0; i < pathPoints.length - 1; i++) {
            Vec3 start = pathPoints[i];
            Vec3 end = pathPoints[i + 1];
            
            // 計算路徑進度
            float pathProgress = (float) i / (float) (pathPoints.length - 1);
            
            // 沿線段生成粒子
            spawnParticlesAlongSegment(start, end, pathProgress);
        }
    }
    
    private void spawnParticlesAlongSegment(Vec3 start, Vec3 end, float pathProgress) {
        Vec3 direction = end.subtract(start);
        double segmentLength = direction.length();
        
        if (segmentLength < 0.01) {
            return; // 線段太短，跳過
        }
        
        Vec3 normalizedDirection = direction.normalize();
        int particleCount = Math.max(1, (int) (segmentLength * density));
        
        for (int i = 0; i < particleCount; i++) {
            float segmentProgress = (float) i / (float) particleCount;
            float totalProgress = pathProgress + segmentProgress / (float) (pathPoints.length - 1);
            
            Vec3 particlePos = start.add(normalizedDirection.scale(segmentLength * segmentProgress));
            
            spawnParticlesAtPoint(particlePos, totalProgress);
        }
    }
    
    private void spawnParticlesAtPoint(Vec3 basePos, float pathProgress) {
        Vec3 particlePos = basePos;
        
        // 應用螺旋效果
        if (spiral) {
            float spiralX = (float) Math.cos(spiralPhase + pathProgress * Math.PI * 4) * spiralRadius;
            float spiralZ = (float) Math.sin(spiralPhase + pathProgress * Math.PI * 4) * spiralRadius;
            particlePos = particlePos.add(spiralX, 0, spiralZ);
        }
        
        // 應用擴散
        if (spread > 0) {
            double offsetX = (Math.random() - 0.5) * spread * 2;
            double offsetY = (Math.random() - 0.5) * spread * 2;
            double offsetZ = (Math.random() - 0.5) * spread * 2;
            particlePos = particlePos.add(offsetX, offsetY, offsetZ);
        }
        
        // 計算粒子顏色 (如果啟用漸變)
        int particleColor = colorGradient ? interpolateColor(startColor, endColor, pathProgress) : startColor;
        
        // 計算粒子速度
        Vec3 velocity = new Vec3(
            (Math.random() - 0.5) * speed * 2,
            (Math.random() - 0.5) * speed * 2,
            (Math.random() - 0.5) * speed * 2
        );
        
        // TODO: 實際生成粒子
        // 這裡應該調用 Minecraft 的粒子系統
        spawnActualParticle(particlePos, velocity, particleColor);
    }
    
    /**
     * 實際的粒子生成方法 - 將調用 Minecraft 粒子系統
     */
    private void spawnActualParticle(Vec3 pos, Vec3 velocity, int color) {
        // TODO: 實現實際的粒子生成
        // Minecraft.getInstance().level.addParticle(particleType, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
    }
    
    /**
     * 在兩個顏色之間插值
     */
    private int interpolateColor(int color1, int color2, float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * progress);
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    @Override
    protected void onComplete() {
        if (onCompleteCallback != null) {
            onCompleteCallback.accept(this);
        }
    }
    
    @Override
    public void render(float partialTicks) {
        // 粒子軌跡效果主要透過粒子系統渲染，這裡不需要特殊的渲染邏輯
        // 但可以添加額外的視覺效果，比如連接線等
    }
    
    @Override
    public boolean shouldRender() {
        // 檢查實體是否仍然存在
        if (followEntity != null && followEntity.isRemoved()) {
            return false;
        }
        
        // 檢查路徑是否有效
        if (pathPoints == null || pathPoints.length == 0) {
            return false;
        }
        
        return super.shouldRender();
    }
    
    // ========== Getter 方法 ==========
    
    public Vec3[] getPathPoints() {
        return pathPoints != null ? pathPoints.clone() : null;
    }
    
    public Entity getFollowEntity() {
        return followEntity;
    }
    
    public ParticleOptions getParticleType() {
        return particleType;
    }
    
    public int getDensity() {
        return density;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public int getParticleLifetime() {
        return particleLifetime;
    }
    
    public float getSpread() {
        return spread;
    }
    
    public boolean hasColorGradient() {
        return colorGradient;
    }
    
    public int getStartColor() {
        return startColor;
    }
    
    public int getEndColor() {
        return endColor;
    }
    
    public boolean hasSpiral() {
        return spiral;
    }
    
    public float getSpiralRadius() {
        return spiralRadius;
    }
    
    public float getSpiralSpeed() {
        return spiralSpeed;
    }
    
    public float getSpiralPhase() {
        return spiralPhase;
    }
    
    /**
     * 獲取路徑總長度
     * @return 路徑長度
     */
    public double getPathLength() {
        if (pathPoints == null || pathPoints.length < 2) {
            return 0.0;
        }
        
        double totalLength = 0.0;
        for (int i = 0; i < pathPoints.length - 1; i++) {
            totalLength += pathPoints[i].distanceTo(pathPoints[i + 1]);
        }
        
        return totalLength;
    }
    
    @Override
    public String getEffectType() {
        return "ParticleTrail";
    }
    
    @Override
    public String getDebugInfo() {
        return String.format("%s [Path Points: %d, Length: %.2f, Density: %d, Spiral: %s]", 
            super.getDebugInfo(), 
            pathPoints != null ? pathPoints.length : 0,
            getPathLength(), density, spiral ? "Yes" : "No");
    }
}