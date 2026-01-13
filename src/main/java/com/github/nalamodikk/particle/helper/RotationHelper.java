package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import org.joml.Quaternionf;

import java.util.UUID;

/**
 * 旋轉控制助手
 * 提供粒子旋轉和方向控制
 */
public class RotationHelper {

    /**
     * 旋轉變化（線性插值）
     * @param particle 粒子
     * @param fromRoll 起始滾轉角（弧度）
     * @param toRoll 結束滾轉角（弧度）
     * @param duration 持續時間（tick）
     */
    public static void rotateOverTime(ControlableParticle particle, float fromRoll, float toRoll, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                float roll = fromRoll + (toRoll - fromRoll) * progress;
                particle.setRoll(roll);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 持續旋轉
     * @param particle 粒子
     * @param speed 旋轉速度（弧度/tick）
     * @return 任務 UUID
     */
    public static UUID spin(ControlableParticle particle, float speed) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            float currentRoll = particle.getRoll();
            particle.setRoll(currentRoll + speed);
        }, 0, 1);
    }

    /**
     * 擺動旋轉
     * @param particle 粒子
     * @param minAngle 最小角度（弧度）
     * @param maxAngle 最大角度（弧度）
     * @param period 週期（tick）
     * @return 任務 UUID
     */
    public static UUID swing(ControlableParticle particle, float minAngle, float maxAngle, int period) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 使用正弦波產生擺動
                float progress = (float) Math.sin(2 * Math.PI * tick / period);
                float angle = minAngle + (maxAngle - minAngle) * (progress + 1.0f) / 2.0f;
                particle.setRoll(angle);

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 設置粒子滾轉角
     * @param particle 粒子
     * @param roll 滾轉角（弧度）
     */
    public static void setRoll(ControlableParticle particle, float roll) {
        particle.setRoll(roll);
    }

    /**
     * 設置粒子俯仰角
     * @param particle 粒子
     * @param pitch 俯仰角（弧度）
     */
    public static void setPitch(ControlableParticle particle, float pitch) {
        particle.setPitch(pitch);
    }

    /**
     * 設置粒子偏航角
     * @param particle 粒子
     * @param yaw 偏航角（弧度）
     */
    public static void setYaw(ControlableParticle particle, float yaw) {
        particle.setYaw(yaw);
    }

    /**
     * 設置完整旋轉（歐拉角）
     * @param particle 粒子
     * @param pitch 俯仰角（弧度）
     * @param yaw 偏航角（弧度）
     * @param roll 滾轉角（弧度）
     */
    public static void setRotation(ControlableParticle particle, float pitch, float yaw, float roll) {
        particle.setPitch(pitch);
        particle.setYaw(yaw);
        particle.setRoll(roll);
    }

    /**
     * 隨機旋轉
     * @param particle 粒子
     */
    public static void randomRotation(ControlableParticle particle) {
        float pitch = (float) (Math.random() * 2 * Math.PI);
        float yaw = (float) (Math.random() * 2 * Math.PI);
        float roll = (float) (Math.random() * 2 * Math.PI);
        setRotation(particle, pitch, yaw, roll);
    }

    /**
     * 緩入緩出旋轉
     * @param particle 粒子
     * @param fromRoll 起始滾轉角
     * @param toRoll 結束滾轉角
     * @param duration 持續時間（tick）
     */
    public static void smoothRotate(ControlableParticle particle, float fromRoll, float toRoll, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                // 緩入緩出公式
                float smoothProgress = t * t * (3.0f - 2.0f * t);
                float roll = fromRoll + (toRoll - fromRoll) * smoothProgress;
                particle.setRoll(roll);

                tick++;
            }
        }, 0, 1, duration);
    }
}
