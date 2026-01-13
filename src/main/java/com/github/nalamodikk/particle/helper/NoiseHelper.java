package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

/**
 * 噪聲助手
 * 提供 Perlin 噪聲和隨機噪聲效果
 */
public class NoiseHelper {

    private static final Random RANDOM = new Random();

    /**
     * 隨機噪聲位移
     * @param particle 粒子
     * @param amplitude 振幅
     * @return 任務 UUID
     */
    public static UUID randomNoise(ControlableParticle particle, double amplitude) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            double dx = (RANDOM.nextDouble() - 0.5) * amplitude;
            double dy = (RANDOM.nextDouble() - 0.5) * amplitude;
            double dz = (RANDOM.nextDouble() - 0.5) * amplitude;

            Vec3 pos = particle.getPosition();
            particle.teleportTo(pos.add(dx, dy, dz));
        }, 0, 1);
    }

    /**
     * Perlin 噪聲位移（簡化版）
     * @param particle 粒子
     * @param amplitude 振幅
     * @param frequency 頻率
     * @return 任務 UUID
     */
    public static UUID perlinNoise(ControlableParticle particle, double amplitude, double frequency) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 簡化的 Perlin 噪聲（使用多個正弦波疊加）
                double t = tick * frequency * 0.01;
                double noise1 = Math.sin(t) * 0.5;
                double noise2 = Math.sin(t * 2.5 + 1.3) * 0.3;
                double noise3 = Math.sin(t * 5.2 + 2.7) * 0.2;
                double noise = (noise1 + noise2 + noise3) * amplitude;

                Vec3 pos = particle.getPosition();
                particle.teleportTo(pos.x + noise * 0.1, pos.y + noise * 0.1, pos.z + noise * 0.1);

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 噪聲速度
     * @param particle 粒子
     * @param maxNoise 最大噪聲
     * @return 任務 UUID
     */
    public static UUID velocityNoise(ControlableParticle particle, double maxNoise) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            double nvx = (RANDOM.nextDouble() - 0.5) * maxNoise;
            double nvy = (RANDOM.nextDouble() - 0.5) * maxNoise;
            double nvz = (RANDOM.nextDouble() - 0.5) * maxNoise;

            particle.setVelocity(velocity.add(nvx, nvy, nvz));
        }, 0, 1);
    }
}
