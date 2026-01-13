package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;

/**
 * 生命週期助手
 * 提供粒子生命週期相關的控制
 */
public class LifetimeHelper {

    /**
     * 設置粒子生命週期
     * @param particle 粒子
     * @param lifetime 生命週期（tick）
     */
    public static void setLifetime(ControlableParticle particle, int lifetime) {
        particle.setLifetime(lifetime);
    }

    /**
     * 延遲移除粒子
     * @param particle 粒子
     * @param delay 延遲（tick）
     */
    public static void removeAfter(ControlableParticle particle, int delay) {
        CooScheduler.getInstance().runTaskLater(() -> {
            if (!particle.isRemoved()) {
                particle.remove();
            }
        }, delay);
    }

    /**
     * 淡出後移除
     * @param particle 粒子
     * @param fadeOutDuration 淡出時間（tick）
     */
    public static void fadeOutAndRemove(ControlableParticle particle, int fadeOutDuration) {
        AlphaHelper.fadeOut(particle, fadeOutDuration);
        removeAfter(particle, fadeOutDuration);
    }

    /**
     * 縮小後移除
     * @param particle 粒子
     * @param shrinkDuration 縮小時間（tick）
     */
    public static void shrinkAndRemove(ControlableParticle particle, int shrinkDuration) {
        ScaleHelper.scaleDown(particle, shrinkDuration, 0.01f);
        removeAfter(particle, shrinkDuration);
    }

    /**
     * 設置最大年齡並開始淡出
     * @param particle 粒子
     * @param maxAge 最大年齡（tick）
     * @param fadeStartPercent 開始淡出的百分比（0.0-1.0）
     */
    public static void setMaxAgeWithFade(ControlableParticle particle, int maxAge, float fadeStartPercent) {
        particle.setLifetime(maxAge);

        int fadeStart = (int) (maxAge * fadeStartPercent);
        int fadeDuration = maxAge - fadeStart;

        CooScheduler.getInstance().runTaskLater(() -> {
            if (!particle.isRemoved()) {
                AlphaHelper.fadeOut(particle, fadeDuration);
            }
        }, fadeStart);
    }

    /**
     * 獲取粒子年齡
     * @param particle 粒子
     * @return 當前年齡（tick）
     */
    public static int getAge(ControlableParticle particle) {
        return particle.getAge();
    }

    /**
     * 獲取粒子生命週期
     * @param particle 粒子
     * @return 生命週期（tick）
     */
    public static int getLifetime(ControlableParticle particle) {
        return particle.getLifetime();
    }

    /**
     * 檢查粒子是否即將死亡
     * @param particle 粒子
     * @param threshold 閾值（tick）
     * @return 是否即將死亡
     */
    public static boolean isNearDeath(ControlableParticle particle, int threshold) {
        return (particle.getLifetime() - particle.getAge()) <= threshold;
    }
}
