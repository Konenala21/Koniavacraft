package com.github.nalamodikk.particle.effects;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.ModParticles;
import com.github.nalamodikk.particle.ParticleController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 客戶端魔法陣效果
 *
 * 直接在客戶端生成並控制粒子，實現旋轉動畫
 */
@OnlyIn(Dist.CLIENT)
public class ClientMagicCircleEffect {

    private final ClientLevel level;
    private final Vec3 center;
    private final List<CircleRing> rings = new ArrayList<>();

    private int tickCounter = 0;
    private final int duration;
    private boolean isActive = true;

    public ClientMagicCircleEffect(BlockPos pos, Config config) {
        this.level = (ClientLevel) Minecraft.getInstance().level;
        if (this.level == null) {
            this.isActive = false;
            this.duration = 0;
            this.center = Vec3.ZERO; // 初始化 center 避免編譯錯誤
            return;
        }

        this.center = Vec3.atCenterOf(pos).add(0, config.yOffset, 0);
        this.duration = config.duration;

        // 生成多層圓環
        for (int i = 0; i < config.ringCount; i++) {
            float radius = config.baseRadius + i * config.radiusStep;
            int particleCount = (int)(config.particlesPerRing * (1 + i * 0.3f));
            float rotationSpeed = config.baseRotationSpeed * (1 + i * 0.15f);

            int color = config.ringColors.length > i ?
                config.ringColors[i] : config.ringColors[0];

            CircleRing ring = new CircleRing(
                radius,
                particleCount,
                rotationSpeed,
                config.particleSize,
                color,
                config.particleAlpha
            );

            rings.add(ring);
            ring.spawnParticles(level, center);
        }
    }

    /**
     * 每 tick 更新
     */
    public void tick() {
        if (!isActive) return;

        tickCounter++;

        // 更新所有圓環
        for (CircleRing ring : rings) {
            ring.update(center);
        }

        // 檢查是否結束
        if (duration > 0 && tickCounter >= duration) {
            stop();
        }
    }

    /**
     * 停止效果
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

    // ========== 內部類：圓環 ==========

    private static class CircleRing {
        private final float radius;
        private final int particleCount;
        private final float rotationSpeed; // 弧度/tick
        private final float particleSize;
        private final int color;
        private final float alpha;

        private final List<ParticleController> particleControllers = new ArrayList<>();
        private float currentRotation = 0;

        public CircleRing(float radius, int particleCount, float rotationSpeed,
                         float particleSize, int color, float alpha) {
            this.radius = radius;
            this.particleCount = particleCount;
            this.rotationSpeed = (float)Math.toRadians(rotationSpeed); // 轉換為弧度
            this.particleSize = particleSize;
            this.color = color;
            this.alpha = alpha;
        }

        /**
         * 生成圓環上的粒子
         */
        public void spawnParticles(ClientLevel level, Vec3 center) {
            for (int i = 0; i < particleCount; i++) {
                float angle = (float)(2 * Math.PI * i / particleCount);
                Vec3 pos = calculatePosition(center, angle);

                // 創建粒子選項
                CooParticleOptions options = new CooParticleOptions(
                    particleSize,
                    color,
                    alpha
                );

                // 使用粒子引擎創建粒子（會自動設置 sprite）
                ControlableParticle particle = (ControlableParticle) Minecraft.getInstance().particleEngine
                    .createParticle(options, pos.x, pos.y, pos.z, 0, 0, 0);

                if (particle != null) {
                    // 設置生命週期
                    particle.setLifetime(200); // 10 秒

                    // 創建控制器
                    ParticleController controller = new ParticleController(particle.getParticleId());
                    particleControllers.add(controller);
                }
            }
        }

        /**
         * 更新粒子位置（實現旋轉）
         */
        public void update(Vec3 center) {
            // 更新旋轉角度
            currentRotation += rotationSpeed;
            if (currentRotation > Math.PI * 2) {
                currentRotation -= Math.PI * 2;
            }

            // 更新每個粒子的位置
            for (int i = 0; i < particleControllers.size(); i++) {
                float baseAngle = (float)(2 * Math.PI * i / particleCount);
                float angle = baseAngle + currentRotation;

                Vec3 newPos = calculatePosition(center, angle);
                particleControllers.get(i).setPosition(newPos);
            }
        }

        /**
         * 計算圓上的位置
         */
        private Vec3 calculatePosition(Vec3 center, float angle) {
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y;
            double z = center.z + Math.sin(angle) * radius;
            return new Vec3(x, y, z);
        }

        /**
         * 移除所有粒子
         */
        public void removeParticles() {
            for (ParticleController controller : particleControllers) {
                controller.remove();
            }
            particleControllers.clear();
        }
    }

    // ========== 配置類 ==========

    public static class Config {
        public int ringCount = 3;
        public float baseRadius = 0.6f;
        public float radiusStep = 0.25f;
        public int particlesPerRing = 24;
        public float baseRotationSpeed = 3f; // 度/tick
        public float particleSize = 0.3f; // 增大粒子尺寸讓其更容易看見
        public float particleAlpha = 0.8f;
        public float yOffset = 0.05f;
        public int duration = 60; // ticks

        public int[] ringColors = {
            0x9D46DF, // 紫色
            0x6B7FF7, // 藍紫色
            0x3BBAF7  // 青色
        };

        public static Config manaGenerator() {
            Config config = new Config();
            config.duration = 40; // 2 秒
            return config;
        }

        public static Config persistent() {
            Config config = new Config();
            config.duration = -1; // 永久
            return config;
        }
    }
}
