package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.world.phys.Vec3;

/**
 * 彈跳效果助手
 * 提供各種彈跳相關的功能
 */
public class BounceHelper {

    /**
     * 彈跳動畫（縮放）
     * @param particle 粒子
     * @param targetScale 目標縮放
     * @param bounces 彈跳次數
     * @param duration 總持續時間（tick）
     */
    public static void bounceScale(ControlableParticle particle, float targetScale, int bounces, int duration) {
        float startScale = particle.getScale();

        com.github.nalamodikk.particle.scheduler.CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                // 彈跳公式：使用阻尼正弦波
                float bounce = (float) Math.exp(-3.0 * t) * (float) Math.cos(bounces * Math.PI * t);
                float scale = targetScale + (startScale - targetScale) * bounce;
                particle.setScale(Math.max(0.01f, scale));

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 彈跳運動（Y 軸）
     * @param particle 粒子
     * @param bounceHeight 彈跳高度
     * @param bounces 彈跳次數
     * @param duration 總持續時間（tick）
     */
    public static void bounceMotion(ControlableParticle particle, double bounceHeight, int bounces, int duration) {
        Vec3 startPos = particle.getPosition();

        com.github.nalamodikk.particle.scheduler.CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                // 彈跳高度衰減
                double height = bounceHeight * Math.exp(-2.0 * t) * Math.abs(Math.sin(bounces * Math.PI * t));

                particle.teleportTo(startPos.x, startPos.y + height, startPos.z);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 反彈速度（與表面碰撞）
     * @param particle 粒子
     * @param surfaceNormal 表面法向量
     * @param restitution 恢復係數（0.0-1.0）
     */
    public static void bounceOffSurface(ControlableParticle particle, Vec3 surfaceNormal, double restitution) {
        VelocityHelper.bounce(particle, surfaceNormal, restitution);
    }

    /**
     * 彈跳球效果（持續彈跳）
     * @param particle 粒子
     * @param groundY 地面 Y 座標
     * @param initialVelocityY 初始 Y 速度
     * @param restitution 恢復係數
     * @return 任務 UUID
     */
    public static java.util.UUID bouncingBall(ControlableParticle particle, double groundY, double initialVelocityY, double restitution) {
        Vec3 velocity = particle.getVelocity();
        particle.setVelocity(new Vec3(velocity.x, initialVelocityY, velocity.z));

        return com.github.nalamodikk.particle.scheduler.CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 vel = particle.getVelocity();

            // 模擬重力
            particle.setVelocity(vel.add(0, -GravityHelper.STANDARD_GRAVITY, 0));

            // 檢查是否觸地
            if (pos.y <= groundY && vel.y < 0) {
                // 反彈
                particle.setVelocity(new Vec3(vel.x, -vel.y * restitution, vel.z));
                particle.teleportTo(pos.x, groundY + 0.1, pos.z);

                // 能量耗盡，停止彈跳
                if (Math.abs(vel.y) < 0.01) {
                    particle.setVelocity(new Vec3(vel.x, 0, vel.z));
                }
            }
        }, 0, 1);
    }

    /**
     * 彈跳透明度效果
     * @param particle 粒子
     * @param minAlpha 最小透明度
     * @param maxAlpha 最大透明度
     * @param bounces 彈跳次數
     * @param duration 持續時間（tick）
     */
    public static void bounceAlpha(ControlableParticle particle, float minAlpha, float maxAlpha, int bounces, int duration) {
        com.github.nalamodikk.particle.scheduler.CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                float bounce = (float) Math.exp(-3.0 * t) * (float) Math.abs(Math.cos(bounces * Math.PI * t));
                float alpha = minAlpha + (maxAlpha - minAlpha) * bounce;
                particle.setAlpha(Math.max(0.0f, Math.min(1.0f, alpha)));

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 彈性碰撞（兩個粒子之間）
     * @param particle1 粒子 1
     * @param particle2 粒子 2
     * @param restitution 恢復係數
     */
    public static void elasticCollision(ControlableParticle particle1, ControlableParticle particle2, double restitution) {
        Vec3 pos1 = particle1.getPosition();
        Vec3 pos2 = particle2.getPosition();

        Vec3 vel1 = particle1.getVelocity();
        Vec3 vel2 = particle2.getVelocity();

        // 計算碰撞法線
        Vec3 normal = pos2.subtract(pos1).normalize();

        // 計算相對速度
        Vec3 relativeVel = vel1.subtract(vel2);
        double velAlongNormal = relativeVel.dot(normal);

        // 如果粒子正在遠離，不處理碰撞
        if (velAlongNormal > 0) {
            return;
        }

        // 計算衝量
        double impulse = -(1 + restitution) * velAlongNormal / 2.0; // 假設質量相等

        // 應用衝量
        Vec3 impulseVec = normal.scale(impulse);
        particle1.setVelocity(vel1.add(impulseVec));
        particle2.setVelocity(vel2.subtract(impulseVec));
    }
}
