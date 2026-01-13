package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 軌跡助手
 * 提供粒子軌跡效果（簡化版）
 */
public class TrailHelper {

    /**
     * 創建粒子軌跡
     * @param particle 粒子
     * @param level 世界
     * @param trailParticle 軌跡粒子類型
     * @param interval 生成間隔（tick）
     * @return 任務 UUID
     */
    public static UUID createTrail(ControlableParticle particle, ClientLevel level, ParticleOptions trailParticle, int interval) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            level.addParticle(trailParticle, pos.x, pos.y, pos.z, 0, 0, 0);
        }, 0, interval);
    }

    /**
     * 創建淡化軌跡
     * @param particle 粒子
     * @param level 世界
     * @param trailParticle 軌跡粒子類型
     * @param interval 生成間隔（tick）
     * @param count 每次生成數量
     * @return 任務 UUID
     */
    public static UUID createFadingTrail(ControlableParticle particle, ClientLevel level, ParticleOptions trailParticle, int interval, int count) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            for (int i = 0; i < count; i++) {
                double offset = i * 0.1;
                level.addParticle(trailParticle,
                    pos.x, pos.y - offset, pos.z,
                    0, 0, 0);
            }
        }, 0, interval);
    }

    /**
     * 停止軌跡生成
     * @param taskId 任務 UUID
     */
    public static void stopTrail(UUID taskId) {
        CooScheduler.getInstance().cancelTask(taskId);
    }
}
