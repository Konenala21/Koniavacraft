package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 吸引子助手
 * 提供各種吸引力場效果
 */
public class AttractorHelper {

    /**
     * 點吸引子（恆定力）
     * @param particle 粒子
     * @param attractor 吸引點
     * @param strength 吸引力強度
     * @return 任務 UUID
     */
    public static UUID pointAttractor(ControlableParticle particle, Vec3 attractor, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toAttractor = attractor.subtract(pos);
            double distance = toAttractor.length();

            if (distance > 0.001) {
                Vec3 force = toAttractor.normalize().scale(strength);
                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 平方反比吸引子（類似重力）
     * @param particle 粒子
     * @param attractor 吸引點
     * @param strength 吸引力強度
     * @return 任務 UUID
     */
    public static UUID inverseSquareAttractor(ControlableParticle particle, Vec3 attractor, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toAttractor = attractor.subtract(pos);
            double distanceSquared = toAttractor.lengthSqr();

            if (distanceSquared > 0.001) {
                double forceMagnitude = strength / distanceSquared;
                Vec3 force = toAttractor.normalize().scale(forceMagnitude);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 動態吸引子（吸引點可移動）
     * @param particle 粒子
     * @param attractorSupplier 吸引點供應器
     * @param strength 吸引力強度
     * @return 任務 UUID
     */
    public static UUID dynamicAttractor(ControlableParticle particle, Supplier<Vec3> attractorSupplier, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 attractor = attractorSupplier.get();
            if (attractor == null) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toAttractor = attractor.subtract(pos);
            double distance = toAttractor.length();

            if (distance > 0.001) {
                Vec3 force = toAttractor.normalize().scale(strength);
                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 範圍限制吸引子（僅在一定範圍內生效）
     * @param particle 粒子
     * @param attractor 吸引點
     * @param strength 吸引力強度
     * @param minRange 最小範圍
     * @param maxRange 最大範圍
     * @return 任務 UUID
     */
    public static UUID rangedAttractor(ControlableParticle particle, Vec3 attractor, double strength, double minRange, double maxRange) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            double distance = pos.distanceTo(attractor);

            if (distance >= minRange && distance <= maxRange) {
                Vec3 toAttractor = attractor.subtract(pos).normalize();
                Vec3 force = toAttractor.scale(strength);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 磁力吸引子（力隨距離線性變化）
     * @param particle 粒子
     * @param attractor 吸引點
     * @param maxStrength 最大力強度
     * @param range 有效範圍
     * @return 任務 UUID
     */
    public static UUID magneticAttractor(ControlableParticle particle, Vec3 attractor, double maxStrength, double range) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toAttractor = attractor.subtract(pos);
            double distance = toAttractor.length();

            if (distance < range && distance > 0.001) {
                // 力與距離成反比（線性）
                double strength = maxStrength * (1.0 - distance / range);
                Vec3 force = toAttractor.normalize().scale(strength);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 渦旋吸引子（螺旋吸引）
     * @param particle 粒子
     * @param center 渦旋中心
     * @param strength 吸引力強度
     * @param rotationSpeed 旋轉速度
     * @return 任務 UUID
     */
    public static UUID vortexAttractor(ControlableParticle particle, Vec3 center, double strength, double rotationSpeed) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = center.subtract(pos);
            double distance = toCenter.length();

            if (distance > 0.001) {
                // 向心力
                Vec3 centripetalForce = toCenter.normalize().scale(strength);

                // 切線力（旋轉）
                Vec3 tangent = new Vec3(-toCenter.z, 0, toCenter.x).normalize();
                Vec3 tangentialForce = tangent.scale(rotationSpeed);

                // 合力
                Vec3 totalForce = centripetalForce.add(tangentialForce);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(totalForce));
            }
        }, 0, 1);
    }

    /**
     * 脈衝吸引子（吸引力週期性變化）
     * @param particle 粒子
     * @param attractor 吸引點
     * @param minStrength 最小力強度
     * @param maxStrength 最大力強度
     * @param period 週期（tick）
     * @return 任務 UUID
     */
    public static UUID pulsingAttractor(ControlableParticle particle, Vec3 attractor, double minStrength, double maxStrength, int period) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 計算當前力強度（正弦波）
                float progress = (float) Math.sin(2 * Math.PI * tick / period);
                double strength = minStrength + (maxStrength - minStrength) * (progress + 1.0f) / 2.0f;

                Vec3 pos = particle.getPosition();
                Vec3 toAttractor = attractor.subtract(pos);
                double distance = toAttractor.length();

                if (distance > 0.001) {
                    Vec3 force = toAttractor.normalize().scale(strength);
                    Vec3 velocity = particle.getVelocity();
                    particle.setVelocity(velocity.add(force));
                }

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 軌道吸引子（使粒子進入穩定軌道）
     * @param particle 粒子
     * @param center 軌道中心
     * @param orbitRadius 目標軌道半徑
     * @param strength 力強度
     * @return 任務 UUID
     */
    public static UUID orbitalAttractor(ControlableParticle particle, Vec3 center, double orbitRadius, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = center.subtract(pos);
            double currentRadius = toCenter.length();

            if (currentRadius > 0.001) {
                // 計算所需的向心力
                double radiusDiff = currentRadius - orbitRadius;
                Vec3 radialForce = toCenter.normalize().scale(strength * radiusDiff / orbitRadius);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(radialForce));
            }
        }, 0, 1);
    }

    /**
     * 停止吸引子效果
     * @param taskId 任務 UUID
     */
    public static void stopAttractor(UUID taskId) {
        CooScheduler.getInstance().cancelTask(taskId);
    }
}
