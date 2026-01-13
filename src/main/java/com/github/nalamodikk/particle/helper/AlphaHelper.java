package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;

import java.util.UUID;

/**
 * 透明度控制助手
 * 提供粒子透明度變化的各種效果
 */
public class AlphaHelper {

    /**
     * 淡入效果
     * @param particle 粒子
     * @param duration 持續時間（tick）
     */
    public static void fadeIn(ControlableParticle particle, int duration) {
        fadeIn(particle, duration, 1.0f);
    }

    /**
     * 淡入效果（指定目標透明度）
     * @param particle 粒子
     * @param duration 持續時間（tick）
     * @param targetAlpha 目標透明度（0.0-1.0）
     */
    public static void fadeIn(ControlableParticle particle, int duration, float targetAlpha) {
        float startAlpha = 0.0f;
        particle.setAlpha(startAlpha);

        // 使用調度器每 tick 更新透明度
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                float alpha = startAlpha + (targetAlpha - startAlpha) * progress;
                particle.setAlpha(Math.min(alpha, targetAlpha));

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 淡出效果
     * @param particle 粒子
     * @param duration 持續時間（tick）
     */
    public static void fadeOut(ControlableParticle particle, int duration) {
        fadeOut(particle, duration, 0.0f);
    }

    /**
     * 淡出效果（指定目標透明度）
     * @param particle 粒子
     * @param duration 持續時間（tick）
     * @param targetAlpha 目標透明度（0.0-1.0）
     */
    public static void fadeOut(ControlableParticle particle, int duration, float targetAlpha) {
        float startAlpha = particle.getAlpha();

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                float alpha = startAlpha + (targetAlpha - startAlpha) * progress;
                particle.setAlpha(Math.max(alpha, targetAlpha));

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 透明度脈衝效果
     * @param particle 粒子
     * @param minAlpha 最小透明度
     * @param maxAlpha 最大透明度
     * @param period 週期（tick）
     */
    public static UUID pulse(ControlableParticle particle, float minAlpha, float maxAlpha, int period) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 使用正弦波產生脈衝效果
                float progress = (float) Math.sin(2 * Math.PI * tick / period);
                // 映射到 [minAlpha, maxAlpha] 範圍
                float alpha = minAlpha + (maxAlpha - minAlpha) * (progress + 1.0f) / 2.0f;
                particle.setAlpha(alpha);

                tick++;
            }
        }, 0, 1); // 無限循環
    }

    /**
     * 透明度閃爍效果
     * @param particle 粒子
     * @param onDuration 顯示持續時間（tick）
     * @param offDuration 隱藏持續時間（tick）
     */
    public static UUID blink(ControlableParticle particle, int onDuration, int offDuration) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;
            boolean isOn = true;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                int currentPeriod = isOn ? onDuration : offDuration;
                if (tick >= currentPeriod) {
                    tick = 0;
                    isOn = !isOn;
                    particle.setAlpha(isOn ? 1.0f : 0.0f);
                }

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 線性透明度變化
     * @param particle 粒子
     * @param fromAlpha 起始透明度
     * @param toAlpha 結束透明度
     * @param duration 持續時間（tick）
     */
    public static void linearChange(ControlableParticle particle, float fromAlpha, float toAlpha, int duration) {
        particle.setAlpha(fromAlpha);

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                float alpha = fromAlpha + (toAlpha - fromAlpha) * progress;
                particle.setAlpha(alpha);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 緩入緩出透明度變化（使用三次方插值）
     * @param particle 粒子
     * @param fromAlpha 起始透明度
     * @param toAlpha 結束透明度
     * @param duration 持續時間（tick）
     */
    public static void smoothChange(ControlableParticle particle, float fromAlpha, float toAlpha, int duration) {
        particle.setAlpha(fromAlpha);

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
                float alpha = fromAlpha + (toAlpha - fromAlpha) * smoothProgress;
                particle.setAlpha(alpha);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 設置粒子透明度
     * @param particle 粒子
     * @param alpha 透明度（0.0-1.0）
     */
    public static void setAlpha(ControlableParticle particle, float alpha) {
        particle.setAlpha(Math.max(0.0f, Math.min(1.0f, alpha)));
    }
}
