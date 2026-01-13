package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 重力控制助手
 * 提供各種重力效果模擬
 */
public class GravityHelper {

    /** 標準重力加速度（Minecraft 單位） */
    public static final double STANDARD_GRAVITY = 0.04;

    /**
     * 施加標準重力
     * @param particle 粒子
     * @return 任務 UUID
     */
    public static UUID applyGravity(ControlableParticle particle) {
        return applyGravity(particle, STANDARD_GRAVITY);
    }

    /**
     * 施加自定義重力
     * @param particle 粒子
     * @param gravity 重力大小
     * @return 任務 UUID
     */
    public static UUID applyGravity(ControlableParticle particle, double gravity) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(0, -gravity, 0));
        }, 0, 1);
    }

    /**
     * 施加反重力（向上）
     * @param particle 粒子
     * @param antigravity 反重力大小
     * @return 任務 UUID
     */
    public static UUID applyAntigravity(ControlableParticle particle, double antigravity) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(0, antigravity, 0));
        }, 0, 1);
    }

    /**
     * 點重力（向指定點吸引）
     * @param particle 粒子
     * @param gravityCenter 重力中心
     * @param strength 重力強度
     * @return 任務 UUID
     */
    public static UUID pointGravity(ControlableParticle particle, Vec3 gravityCenter, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = gravityCenter.subtract(pos);
            double distance = toCenter.length();

            if (distance > 0.001) {
                // 重力 = G / r²（簡化版本）
                double gravityMagnitude = strength / (distance * distance);
                Vec3 gravityForce = toCenter.normalize().scale(gravityMagnitude);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(gravityForce));
            }
        }, 0, 1);
    }

    /**
     * 行星重力（與距離平方成反比）
     * @param particle 粒子
     * @param planetCenter 行星中心
     * @param mass 行星質量
     * @return 任務 UUID
     */
    public static UUID planetaryGravity(ControlableParticle particle, Vec3 planetCenter, double mass) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = planetCenter.subtract(pos);
            double distanceSquared = toCenter.lengthSqr();

            if (distanceSquared > 0.001) {
                // F = G * M / r²
                double G = 0.001; // 重力常數
                double forceMagnitude = (G * mass) / distanceSquared;
                Vec3 force = toCenter.normalize().scale(forceMagnitude);

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(force));
            }
        }, 0, 1);
    }

    /**
     * 零重力（取消重力影響）
     * @param particle 粒子
     */
    public static void zeroGravity(ControlableParticle particle) {
        // Minecraft 粒子默認有重力，這裡不需要額外處理
        // 只需確保不添加額外的重力
    }

    /**
     * 脈衝重力（重力在兩個值之間振盪）
     * @param particle 粒子
     * @param minGravity 最小重力
     * @param maxGravity 最大重力
     * @param period 週期（tick）
     * @return 任務 UUID
     */
    public static UUID pulsingGravity(ControlableParticle particle, double minGravity, double maxGravity, int period) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 使用正弦波
                float progress = (float) Math.sin(2 * Math.PI * tick / period);
                double gravity = minGravity + (maxGravity - minGravity) * (progress + 1.0f) / 2.0f;

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(0, -gravity, 0));

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 方向重力（任意方向的重力）
     * @param particle 粒子
     * @param direction 重力方向（會被歸一化）
     * @param magnitude 重力大小
     * @return 任務 UUID
     */
    public static UUID directionalGravity(ControlableParticle particle, Vec3 direction, double magnitude) {
        Vec3 gravityVector = direction.normalize().scale(magnitude);

        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(gravityVector));
        }, 0, 1);
    }

    /**
     * 漸增重力（重力隨時間增強）
     * @param particle 粒子
     * @param initialGravity 初始重力
     * @param finalGravity 最終重力
     * @param duration 過渡時間（tick）
     */
    public static void increasingGravity(ControlableParticle particle, double initialGravity, double finalGravity, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = Math.min(1.0f, (float) tick / duration);
                double gravity = initialGravity + (finalGravity - initialGravity) * progress;

                Vec3 velocity = particle.getVelocity();
                particle.setVelocity(velocity.add(0, -gravity, 0));

                tick++;
            }
        }, 0, 1, duration + 20); // 多執行一些 tick 保證效果
    }

    /**
     * 黑洞重力（極強的吸引力）
     * @param particle 粒子
     * @param blackHoleCenter 黑洞中心
     * @param eventHorizonRadius 事件視界半徑
     * @param strength 引力強度
     * @return 任務 UUID
     */
    public static UUID blackHoleGravity(ControlableParticle particle, Vec3 blackHoleCenter, double eventHorizonRadius, double strength) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = blackHoleCenter.subtract(pos);
            double distance = toCenter.length();

            if (distance < eventHorizonRadius) {
                // 進入事件視界，粒子消失
                particle.remove();
                return;
            }

            // 極強的引力
            double gravityMagnitude = strength / Math.pow(distance, 1.5);
            Vec3 gravityForce = toCenter.normalize().scale(gravityMagnitude);

            Vec3 velocity = particle.getVelocity();
            particle.setVelocity(velocity.add(gravityForce));
        }, 0, 1);
    }
}
