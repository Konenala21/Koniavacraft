package com.github.nalamodikk.particle.effects;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.github.nalamodikk.particle.ModParticles;
import com.github.nalamodikk.particle.ParticleController;
import com.github.nalamodikk.particle.ParticleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 圓形魔法陣粒子效果
 *
 * 生成多層同心圓，每個圓由粒子組成並旋轉
 */
public class MagicCircleEffect {

    private final Level level;
    private final Vec3 center;
    private final List<CircleRing> rings = new ArrayList<>();

    private int tickCounter = 0;
    private final int duration;
    private boolean isActive = true;

    public MagicCircleEffect(Level level, BlockPos pos, MagicCircleConfig config) {
        this.level = level;
        this.center = Vec3.atCenterOf(pos).add(0, config.yOffset, 0);
        this.duration = config.duration;

        // 生成多層圓環
        for (int i = 0; i < config.ringCount; i++) {
            float radius = config.baseRadius + i * config.radiusStep;
            int particleCount = (int)(config.particlesPerRing * (1 + i * 0.2f)); // 外圈粒子更密集
            float rotationSpeed = config.baseRotationSpeed * (1 + i * 0.1f); // 每層速度稍微不同

            CircleRing ring = new CircleRing(
                radius,
                particleCount,
                rotationSpeed,
                config.particleSize,
                config.ringColors.length > i ? config.ringColors[i] : config.ringColors[0]
            );

            rings.add(ring);
        }

        // 立即生成所有粒子
        spawnAllParticles();
    }

    /**
     * 生成所有圓環的粒子
     */
    private void spawnAllParticles() {
        for (CircleRing ring : rings) {
            ring.spawnParticles(level, center);
        }
    }

    /**
     * 每 tick 更新粒子位置
     */
    public void tick() {
        if (!isActive) return;

        tickCounter++;

        // 更新所有圓環
        for (CircleRing ring : rings) {
            ring.update(center, tickCounter);
        }

        // 檢查是否結束
        if (duration > 0 && tickCounter >= duration) {
            stop();
        }
    }

    /**
     * 停止效果並移除所有粒子
     */
    public void stop() {
        isActive = false;
        for (CircleRing ring : rings) {
            ring.removeParticles();
        }
    }

    public boolean isActive() {
        return isActive;
    }

    // ========== 內部類：單個圓環 ==========

    private static class CircleRing {
        private final float radius;
        private final int particleCount;
        private final float rotationSpeed;
        private final float particleSize;
        private final int color;

        private final List<ParticleController> particles = new ArrayList<>();
        private float currentRotation = 0;

        public CircleRing(float radius, int particleCount, float rotationSpeed, float particleSize, int color) {
            this.radius = radius;
            this.particleCount = particleCount;
            this.rotationSpeed = rotationSpeed; // 度/tick
            this.particleSize = particleSize;
            this.color = color;
        }

        /**
         * 生成圓環上的所有粒子
         */
        public void spawnParticles(Level level, Vec3 center) {
            for (int i = 0; i < particleCount; i++) {
                float angle = (float)(2 * Math.PI * i / particleCount);
                Vec3 pos = calculatePosition(center, angle, 0);

                CooParticleOptions options = new CooParticleOptions(particleSize, color, 0.8f);

                // 生成粒子
                level.addParticle(options, pos.x, pos.y, pos.z, 0, 0, 0);

                // 獲取粒子控制器（粒子生成後會自動註冊到管理器）
                // 我們需要一種方式來獲取剛生成的粒子的 ID
                // 簡化方案：使用返回值或事件
            }
        }

        /**
         * 更新粒子位置（旋轉）
         */
        public void update(Vec3 center, int ticks) {
            // 計算當前旋轉角度
            currentRotation += Math.toRadians(rotationSpeed);
            if (currentRotation > Math.PI * 2) {
                currentRotation -= Math.PI * 2;
            }

            // 更新每個粒子的位置
            for (int i = 0; i < particles.size(); i++) {
                float baseAngle = (float)(2 * Math.PI * i / particleCount);
                float angle = baseAngle + currentRotation;

                Vec3 newPos = calculatePosition(center, angle, 0);
                particles.get(i).setPosition(newPos);
            }
        }

        /**
         * 計算圓上某個角度的位置
         */
        private Vec3 calculatePosition(Vec3 center, float angle, float yOffset) {
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + yOffset;
            double z = center.z + Math.sin(angle) * radius;
            return new Vec3(x, y, z);
        }

        /**
         * 移除所有粒子
         */
        public void removeParticles() {
            for (ParticleController particle : particles) {
                particle.remove();
            }
            particles.clear();
        }
    }

    // ========== 配置類 ==========

    public static class MagicCircleConfig {
        public int ringCount = 3; // 圓環數量
        public float baseRadius = 0.5f; // 最內圈半徑
        public float radiusStep = 0.3f; // 每圈半徑增量
        public int particlesPerRing = 20; // 每圈粒子數
        public float baseRotationSpeed = 2f; // 基礎旋轉速度（度/tick）
        public float particleSize = 0.1f; // 粒子大小
        public float yOffset = 0.1f; // Y 軸偏移
        public int duration = -1; // 持續時間（-1 = 無限）

        // 每層的顏色（可以不同）
        public int[] ringColors = {
            0x9D46DF, // 紫色（內圈）
            0x3B82F7, // 藍色（中圈）
            0x00FFC8  // 青色（外圈）
        };

        public static MagicCircleConfig defaultConfig() {
            return new MagicCircleConfig();
        }

        public static MagicCircleConfig largeConfig() {
            MagicCircleConfig config = new MagicCircleConfig();
            config.ringCount = 5;
            config.baseRadius = 0.8f;
            config.radiusStep = 0.4f;
            config.particlesPerRing = 30;
            return config;
        }

        public static MagicCircleConfig fastConfig() {
            MagicCircleConfig config = new MagicCircleConfig();
            config.baseRotationSpeed = 5f;
            return config;
        }
    }
}
