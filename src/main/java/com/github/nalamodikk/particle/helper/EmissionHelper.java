package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 發射助手
 * 提供粒子發射相關的輔助功能（簡化版）
 */
public class EmissionHelper {

    /**
     * 從粒子位置發射新粒子
     * @param particle 源粒子
     * @param level 世界
     * @param particleType 要發射的粒子類型
     * @param count 發射數量
     * @param spread 擴散範圍
     */
    public static void emitParticlesFrom(ControlableParticle particle, ClientLevel level, ParticleOptions particleType, int count, double spread) {
        Vec3 pos = particle.getPosition();

        for (int i = 0; i < count; i++) {
            double offsetX = (Math.random() - 0.5) * spread;
            double offsetY = (Math.random() - 0.5) * spread;
            double offsetZ = (Math.random() - 0.5) * spread;

            double velX = (Math.random() - 0.5) * 0.1;
            double velY = (Math.random() - 0.5) * 0.1;
            double velZ = (Math.random() - 0.5) * 0.1;

            level.addParticle(particleType,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                velX, velY, velZ);
        }
    }

    /**
     * 週期性發射粒子
     * @param particle 源粒子
     * @param level 世界
     * @param particleType 粒子類型
     * @param count 每次發射數量
     * @param period 週期（tick）
     * @return 任務 UUID
     */
    public static UUID emitPeriodically(ControlableParticle particle, ClientLevel level, ParticleOptions particleType, int count, int period) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            emitParticlesFrom(particle, level, particleType, count, 0.5);
        }, 0, period);
    }

    /**
     * 停止週期性發射
     * @param taskId 任務 UUID
     */
    public static void stopEmission(UUID taskId) {
        CooScheduler.getInstance().cancelTask(taskId);
    }
}
