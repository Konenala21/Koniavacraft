package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 速度控制助手
 * 提供粒子速度相關的控制功能
 */
public class VelocityHelper {

    /**
     * 線性加速到目標速度
     * @param particle 粒子
     * @param targetVelocity 目標速度
     * @param duration 持續時間（tick）
     */
    public static void accelerateTo(ControlableParticle particle, Vec3 targetVelocity, int duration) {
        Vec3 startVelocity = particle.getVelocity();

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                Vec3 currentVel = startVelocity.lerp(targetVelocity, progress);
                particle.setVelocity(currentVel);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 突然改變速度方向（彈射效果）
     * @param particle 粒子
     * @param newDirection 新方向（會被歸一化）
     * @param speed 速度大小
     */
    public static void launch(ControlableParticle particle, Vec3 newDirection, double speed) {
        Vec3 velocity = newDirection.normalize().scale(speed);
        particle.setVelocity(velocity);
    }

    /**
     * 添加速度（衝量）
     * @param particle 粒子
     * @param impulse 速度增量
     */
    public static void addImpulse(ControlableParticle particle, Vec3 impulse) {
        Vec3 current = particle.getVelocity();
        particle.setVelocity(current.add(impulse));
    }

    /**
     * 限制速度大小
     * @param particle 粒子
     * @param maxSpeed 最大速度
     * @return 任務 UUID
     */
    public static UUID limitSpeed(ControlableParticle particle, double maxSpeed) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            double currentSpeed = velocity.length();

            if (currentSpeed > maxSpeed) {
                Vec3 limitedVelocity = velocity.normalize().scale(maxSpeed);
                particle.setVelocity(limitedVelocity);
            }
        }, 0, 1);
    }

    /**
     * 速度衰減（摩擦力）
     * @param particle 粒子
     * @param damping 衰減係數（0.0-1.0，越小衰減越快）
     * @return 任務 UUID
     */
    public static UUID applyDamping(ControlableParticle particle, double damping) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.scale(damping));
        }, 0, 1);
    }

    /**
     * 圓周運動速度（自動計算切線速度）
     * @param particle 粒子
     * @param center 圓心
     * @param angularSpeed 角速度（弧度/tick）
     * @return 任務 UUID
     */
    public static UUID circularMotion(ControlableParticle particle, Vec3 center, double angularSpeed) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = pos.subtract(center);

            // 計算切線方向（垂直於半徑）
            Vec3 tangent = new Vec3(-toCenter.z, 0, toCenter.x).normalize();
            double radius = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);

            // 切線速度 = 角速度 × 半徑
            Vec3 velocity = tangent.scale(angularSpeed * radius);
            particle.setVelocity(velocity);
        }, 0, 1);
    }

    /**
     * 反彈速度（碰撞後）
     * @param particle 粒子
     * @param normal 碰撞面法向量
     * @param restitution 恢復係數（0.0-1.0，1.0 為完全彈性碰撞）
     */
    public static void bounce(ControlableParticle particle, Vec3 normal, double restitution) {
        Vec3 velocity = particle.getVelocity();
        Vec3 normalizedNormal = normal.normalize();

        // 反射公式：v' = v - 2(v·n)n
        double dotProduct = velocity.dot(normalizedNormal);
        Vec3 reflection = velocity.subtract(normalizedNormal.scale(2 * dotProduct));

        particle.setVelocity(reflection.scale(restitution));
    }

    /**
     * 停止粒子運動
     * @param particle 粒子
     */
    public static void stop(ControlableParticle particle) {
        particle.setVelocity(Vec3.ZERO);
    }

    /**
     * 設置恆定速度
     * @param particle 粒子
     * @param velocity 速度向量
     */
    public static void setConstantVelocity(ControlableParticle particle, Vec3 velocity) {
        particle.setVelocity(velocity);
    }

    /**
     * 隨機速度
     * @param particle 粒子
     * @param maxSpeed 最大速度
     */
    public static void randomVelocity(ControlableParticle particle, double maxSpeed) {
        double x = (Math.random() - 0.5) * 2 * maxSpeed;
        double y = (Math.random() - 0.5) * 2 * maxSpeed;
        double z = (Math.random() - 0.5) * 2 * maxSpeed;
        particle.setVelocity(new Vec3(x, y, z));
    }

    /**
     * 朝向目標點的速度
     * @param particle 粒子
     * @param target 目標點
     * @param speed 速度大小
     */
    public static void towardsPoint(ControlableParticle particle, Vec3 target, double speed) {
        Vec3 pos = particle.getPosition();
        Vec3 direction = target.subtract(pos).normalize();
        particle.setVelocity(direction.scale(speed));
    }
}
