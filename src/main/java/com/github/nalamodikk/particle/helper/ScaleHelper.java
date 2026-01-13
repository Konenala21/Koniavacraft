package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;

import java.util.UUID;

/**
 * 縮放控制助手
 * 提供粒子大小變化的各種效果
 */
public class ScaleHelper {

    /**
     * 縮放變化（線性插值）
     * @param particle 粒子
     * @param fromScale 起始縮放
     * @param toScale 結束縮放
     * @param duration 持續時間（tick）
     */
    public static void scaleOverTime(ControlableParticle particle, float fromScale, float toScale, int duration) {
        particle.setScale(fromScale);

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                float scale = fromScale + (toScale - fromScale) * progress;
                particle.setScale(scale);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 縮放脈衝效果
     * @param particle 粒子
     * @param minScale 最小縮放
     * @param maxScale 最大縮放
     * @param period 週期（tick）
     * @return 任務 UUID（可用於取消）
     */
    public static UUID pulse(ControlableParticle particle, float minScale, float maxScale, int period) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 使用正弦波產生脈衝效果
                float progress = (float) Math.sin(2 * Math.PI * tick / period);
                // 映射到 [minScale, maxScale] 範圍
                float scale = minScale + (maxScale - minScale) * (progress + 1.0f) / 2.0f;
                particle.setScale(scale);

                tick++;
            }
        }, 0, 1); // 無限循環
    }

    /**
     * 放大效果
     * @param particle 粒子
     * @param duration 持續時間（tick）
     * @param targetScale 目標縮放
     */
    public static void scaleUp(ControlableParticle particle, int duration, float targetScale) {
        float startScale = particle.getScale();
        scaleOverTime(particle, startScale, targetScale, duration);
    }

    /**
     * 縮小效果
     * @param particle 粒子
     * @param duration 持續時間（tick）
     * @param targetScale 目標縮放
     */
    public static void scaleDown(ControlableParticle particle, int duration, float targetScale) {
        float startScale = particle.getScale();
        scaleOverTime(particle, startScale, targetScale, duration);
    }

    /**
     * 爆炸式縮放（先快速放大，再緩慢縮小）
     * @param particle 粒子
     * @param maxScale 最大縮放
     * @param expandDuration 擴張持續時間（tick）
     * @param shrinkDuration 收縮持續時間（tick）
     */
    public static void explosion(ControlableParticle particle, float maxScale, int expandDuration, int shrinkDuration) {
        float startScale = particle.getScale();

        // 第一階段：快速放大
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / expandDuration;
                // 使用平方插值加速
                float scale = startScale + (maxScale - startScale) * progress * progress;
                particle.setScale(scale);

                tick++;
            }
        }, 0, 1, expandDuration);

        // 第二階段：緩慢縮小
        CooScheduler.getInstance().runTaskLater(() -> {
            if (!particle.isRemoved()) {
                CooScheduler.getInstance().runTaskTimer(new Runnable() {
                    int tick = 0;

                    @Override
                    public void run() {
                        if (particle.isRemoved()) {
                            return;
                        }

                        float progress = (float) tick / shrinkDuration;
                        // 使用平方根插值減速
                        float smoothProgress = (float) Math.sqrt(progress);
                        float scale = maxScale + (0.0f - maxScale) * smoothProgress;
                        particle.setScale(Math.max(0.01f, scale));

                        tick++;
                    }
                }, 0, 1, shrinkDuration);
            }
        }, expandDuration);
    }

    /**
     * 緩入緩出縮放變化
     * @param particle 粒子
     * @param fromScale 起始縮放
     * @param toScale 結束縮放
     * @param duration 持續時間（tick）
     */
    public static void smoothScale(ControlableParticle particle, float fromScale, float toScale, int duration) {
        particle.setScale(fromScale);

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
                float scale = fromScale + (toScale - fromScale) * smoothProgress;
                particle.setScale(scale);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 彈跳縮放效果
     * @param particle 粒子
     * @param targetScale 目標縮放
     * @param duration 持續時間（tick）
     */
    public static void bounce(ControlableParticle particle, float targetScale, int duration) {
        float startScale = particle.getScale();

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                // 彈跳公式：使用阻尼正弦波
                float bounce = (float) Math.exp(-3.0 * t) * (float) Math.cos(10.0 * t);
                float scale = targetScale + (startScale - targetScale) * bounce;
                particle.setScale(Math.max(0.01f, scale));

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 設置粒子縮放
     * @param particle 粒子
     * @param scale 縮放值
     */
    public static void setScale(ControlableParticle particle, float scale) {
        particle.setScale(Math.max(0.01f, scale));
    }
}
