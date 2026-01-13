package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 排斥子助手
 * 提供各種排斥力場效果（與 AttractorHelper 相反）
 */
public class RepulsorHelper {

    /**
     * 點排斥子（恆定力）
     * @param particle 粒子
     * @param repulsor 排斥點
     * @param strength 排斥力強度
     * @return 任務 UUID
     */
    public static UUID pointRepulsor(ControlableParticle particle, Vec3 repulsor, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 fromRepulsor = pos.subtract(repulsor);
            double distance = fromRepulsor.length();

            if (distance > 0.001) {
                Vec3 force = fromRepulsor.normalize().scale(strength);
                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 平方反比排斥子
     * @param particle 粒子
     * @param repulsor 排斥點
     * @param strength 排斥力強度
     * @return 任務 UUID
     */
    public static UUID inverseSquareRepulsor(ControlableParticle particle, Vec3 repulsor, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 fromRepulsor = pos.subtract(repulsor);
            double distanceSquared = fromRepulsor.lengthSqr();

            if (distanceSquared > 0.001) {
                double forceMagnitude = strength / distanceSquared;
                Vec3 force = fromRepulsor.normalize().scale(forceMagnitude);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 範圍限制排斥子
     * @param particle 粒子
     * @param repulsor 排斥點
     * @param strength 排斥力強度
     * @param maxRange 最大範圍
     * @return 任務 UUID
     */
    public static UUID rangedRepulsor(ControlableParticle particle, Vec3 repulsor, double strength, double maxRange) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            double distance = pos.distanceTo(repulsor);

            if (distance < maxRange && distance > 0.001) {
                Vec3 fromRepulsor = pos.subtract(repulsor).normalize();
                Vec3 force = fromRepulsor.scale(strength);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 爆炸排斥（一次性強力推開）
     * @param particle 粒子
     * @param explosionCenter 爆炸中心
     * @param force 爆炸力度
     */
    public static void explosionRepulse(ControlableParticle particle, Vec3 explosionCenter, double force) {
        Vec3 pos = particle.getPosition();
        Vec3 direction = pos.subtract(explosionCenter).normalize();
        Vec3 impulse = direction.scale(force);
        VelocityHelper.addImpulse(particle, impulse);
    }

    /**
     * 停止排斥子效果
     * @param taskId 任務 UUID
     */
    public static void stopRepulsor(UUID taskId) {
        CooScheduler.getInstance().cancelTask(taskId);
    }
}
