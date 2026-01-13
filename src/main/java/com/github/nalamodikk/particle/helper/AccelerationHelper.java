package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 加速度控制助手
 * 提供粒子加速度相關的物理模擬
 */
public class AccelerationHelper {

    /**
     * 施加恆定加速度
     * @param particle 粒子
     * @param acceleration 加速度向量
     * @return 任務 UUID
     */
    public static UUID applyConstantAcceleration(ControlableParticle particle, Vec3 acceleration) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(acceleration));
        }, 0, 1);
    }

    /**
     * 向心加速度（保持圓周運動）
     * @param particle 粒子
     * @param center 圓心
     * @param magnitude 加速度大小
     * @return 任務 UUID
     */
    public static UUID centripetalAcceleration(ControlableParticle particle, Vec3 center, double magnitude) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = center.subtract(pos).normalize();
            Vec3 acceleration = toCenter.scale(magnitude);

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(acceleration));
        }, 0, 1);
    }

    /**
     * 隨機加速度（布朗運動模擬）
     * @param particle 粒子
     * @param maxAcceleration 最大加速度
     * @return 任務 UUID
     */
    public static UUID randomAcceleration(ControlableParticle particle, double maxAcceleration) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            double ax = (Math.random() - 0.5) * 2 * maxAcceleration;
            double ay = (Math.random() - 0.5) * 2 * maxAcceleration;
            double az = (Math.random() - 0.5) * 2 * maxAcceleration;
            Vec3 acceleration = new Vec3(ax, ay, az);

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(acceleration));
        }, 0, 1);
    }

    /**
     * 線性遞減加速度
     * @param particle 粒子
     * @param initialAcceleration 初始加速度
     * @param duration 持續時間（tick）
     */
    public static void decreasingAcceleration(ControlableParticle particle, Vec3 initialAcceleration, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = 1.0f - (float) tick / duration;
                Vec3 acceleration = initialAcceleration.scale(progress);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(acceleration));

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 爆炸加速度（從中心點向外）
     * @param particle 粒子
     * @param explosionCenter 爆炸中心
     * @param force 爆炸力度
     * @param duration 爆炸持續時間（tick）
     */
    public static void explosionAcceleration(ControlableParticle particle, Vec3 explosionCenter, double force, int duration) {
        Vec3 pos = particle.getPosition();
        Vec3 direction = pos.subtract(explosionCenter).normalize();
        Vec3 initialAcceleration = direction.scale(force);

        decreasingAcceleration(particle, initialAcceleration, duration);
    }

    /**
     * 朝向目標的加速度
     * @param particle 粒子
     * @param target 目標點
     * @param magnitude 加速度大小
     * @return 任務 UUID
     */
    public static UUID accelerateTowards(ControlableParticle particle, Vec3 target, double magnitude) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 direction = target.subtract(pos).normalize();
            Vec3 acceleration = direction.scale(magnitude);

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(acceleration));
        }, 0, 1);
    }

    /**
     * 遠離目標的加速度
     * @param particle 粒子
     * @param repulsionPoint 排斥點
     * @param magnitude 加速度大小
     * @return 任務 UUID
     */
    public static UUID accelerateAway(ControlableParticle particle, Vec3 repulsionPoint, double magnitude) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 direction = pos.subtract(repulsionPoint).normalize();
            Vec3 acceleration = direction.scale(magnitude);

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(acceleration));
        }, 0, 1);
    }

    /**
     * 空氣阻力（與速度方向相反）
     * @param particle 粒子
     * @param dragCoefficient 阻力係數
     * @return 任務 UUID
     */
    public static UUID airResistance(ControlableParticle particle, double dragCoefficient) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            double speed = velocity.length();

            if (speed > 0.001) {
                // 阻力 = -k * v²（簡化為線性阻力）
                Vec3 drag = velocity.normalize().scale(-dragCoefficient * speed);
                particle.setVelocity(velocity.add(drag));
            }
        }, 0, 1);
    }

    /**
     * 正弦波加速度
     * @param particle 粒子
     * @param direction 加速度方向
     * @param amplitude 振幅
     * @param frequency 頻率
     * @return 任務 UUID
     */
    public static UUID sinusoidalAcceleration(ControlableParticle particle, Vec3 direction, double amplitude, double frequency) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                double magnitude = amplitude * Math.sin(frequency * tick);
                Vec3 acceleration = direction.normalize().scale(magnitude);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(acceleration));

                tick++;
            }
        }, 0, 1);
    }
}
