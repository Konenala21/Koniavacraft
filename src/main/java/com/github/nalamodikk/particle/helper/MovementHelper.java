package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 移動控制助手
 * 提供粒子移動路徑和運動模式
 */
public class MovementHelper {

    /**
     * 移動到目標位置（線性插值）
     * @param particle 粒子
     * @param target 目標位置
     * @param duration 持續時間（tick）
     */
    public static void moveToTarget(ControlableParticle particle, Vec3 target, int duration) {
        Vec3 start = particle.getPosition();

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                Vec3 current = start.lerp(target, progress);
                particle.teleportTo(current);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 環繞運動
     * @param particle 粒子
     * @param center 中心點
     * @param radius 半徑
     * @param speed 角速度（每 tick 的弧度）
     * @return 任務 UUID
     */
    public static UUID orbit(ControlableParticle particle, Vec3 center, float radius, float speed) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            float angle = 0.0f;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 計算圓周運動位置
                double x = center.x + radius * Math.cos(angle);
                double y = center.y;
                double z = center.z + radius * Math.sin(angle);

                particle.teleportTo(x, y, z);
                angle += speed;

                // 防止角度溢出
                if (angle > 2 * Math.PI) {
                    angle -= 2 * Math.PI;
                }
            }
        }, 0, 1);
    }

    /**
     * 螺旋運動
     * @param particle 粒子
     * @param axis 軸心位置
     * @param radiusGrowth 半徑增長速度（每 tick）
     * @param angularSpeed 角速度（每 tick 的弧度）
     * @return 任務 UUID
     */
    public static UUID spiral(ControlableParticle particle, Vec3 axis, float radiusGrowth, float angularSpeed) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            float angle = 0.0f;
            float radius = 0.0f;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 計算螺旋位置
                double x = axis.x + radius * Math.cos(angle);
                double y = axis.y + angle * 0.1; // Y 軸隨角度增長
                double z = axis.z + radius * Math.sin(angle);

                particle.teleportTo(x, y, z);

                radius += radiusGrowth;
                angle += angularSpeed;

                // 防止角度溢出
                if (angle > 2 * Math.PI) {
                    angle -= 2 * Math.PI;
                }
            }
        }, 0, 1);
    }

    /**
     * 追蹤目標（每 tick 更新目標位置）
     * @param particle 粒子
     * @param targetSupplier 目標位置供應器
     * @param speed 移動速度
     * @return 任務 UUID
     */
    public static UUID track(ControlableParticle particle, java.util.function.Supplier<Vec3> targetSupplier, float speed) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 current = particle.getPosition();
            Vec3 target = targetSupplier.get();

            if (target == null) {
                return;
            }

            // 計算移動方向
            Vec3 direction = target.subtract(current).normalize();
            Vec3 newPos = current.add(direction.scale(speed));

            particle.teleportTo(newPos);
        }, 0, 1);
    }

    /**
     * 波動運動（正弦波）
     * @param particle 粒子
     * @param startPos 起始位置
     * @param endPos 結束位置
     * @param amplitude 振幅
     * @param frequency 頻率
     * @param duration 持續時間（tick）
     */
    public static void wave(ControlableParticle particle, Vec3 startPos, Vec3 endPos, float amplitude, float frequency, int duration) {
        Vec3 direction = endPos.subtract(startPos).normalize();
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x).normalize(); // 垂直方向

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                Vec3 basePos = startPos.lerp(endPos, progress);

                // 添加正弦波偏移
                float offset = amplitude * (float) Math.sin(frequency * progress * 2 * Math.PI);
                Vec3 finalPos = basePos.add(perpendicular.scale(offset));

                particle.teleportTo(finalPos);
                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 拋物線運動
     * @param particle 粒子
     * @param start 起始位置
     * @param end 結束位置
     * @param height 拋物線高度
     * @param duration 持續時間（tick）
     */
    public static void parabola(ControlableParticle particle, Vec3 start, Vec3 end, float height, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;

                // 水平方向線性插值
                double x = start.x + (end.x - start.x) * t;
                double z = start.z + (end.z - start.z) * t;

                // Y 軸拋物線：h = 4 * height * t * (1 - t)
                double y = start.y + (end.y - start.y) * t + height * 4 * t * (1 - t);

                particle.teleportTo(x, y, z);
                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 隨機遊走
     * @param particle 粒子
     * @param maxStep 最大步長
     * @return 任務 UUID
     */
    public static UUID randomWalk(ControlableParticle particle, float maxStep) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 current = particle.getPosition();

            // 隨機方向
            double randomX = (Math.random() - 0.5) * maxStep;
            double randomY = (Math.random() - 0.5) * maxStep;
            double randomZ = (Math.random() - 0.5) * maxStep;

            Vec3 newPos = current.add(randomX, randomY, randomZ);
            particle.teleportTo(newPos);
        }, 0, 1);
    }

    /**
     * 緩入緩出移動
     * @param particle 粒子
     * @param start 起始位置
     * @param end 結束位置
     * @param duration 持續時間（tick）
     */
    public static void smoothMove(ControlableParticle particle, Vec3 start, Vec3 end, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                // 緩入緩出公式：3t² - 2t³
                float smoothProgress = t * t * (3.0f - 2.0f * t);

                Vec3 current = start.lerp(end, smoothProgress);
                particle.teleportTo(current);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 設置粒子速度
     * @param particle 粒子
     * @param velocity 速度向量
     */
    public static void setVelocity(ControlableParticle particle, Vec3 velocity) {
        particle.setVelocity(velocity);
    }

    /**
     * 添加速度
     * @param particle 粒子
     * @param deltaVelocity 速度增量
     */
    public static void addVelocity(ControlableParticle particle, Vec3 deltaVelocity) {
        Vec3 current = particle.getVelocity();
        particle.setVelocity(current.add(deltaVelocity));
    }
}
