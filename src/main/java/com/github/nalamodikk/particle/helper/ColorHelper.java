package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;

import java.awt.Color;
import java.util.UUID;

/**
 * 顏色控制助手
 * 提供粒子顏色變化效果
 */
public class ColorHelper {

    /**
     * 顏色漸變（線性插值）
     * @param particle 粒子
     * @param fromColor 起始顏色
     * @param toColor 結束顏色
     * @param duration 持續時間（tick）
     */
    public static void colorTransition(ControlableParticle particle, Color fromColor, Color toColor, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                Color currentColor = lerpColor(fromColor, toColor, progress);
                particle.setColor(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 彩虹循環效果
     * @param particle 粒子
     * @param period 週期（tick）
     * @param saturation 飽和度（0.0-1.0）
     * @param brightness 亮度（0.0-1.0）
     * @return 任務 UUID
     */
    public static UUID rainbow(ControlableParticle particle, int period, float saturation, float brightness) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 計算色相（0.0 - 1.0）
                float hue = (float) tick / period;
                hue = hue - (int) hue; // 取小數部分

                Color color = Color.getHSBColor(hue, saturation, brightness);
                particle.setColor(color.getRed(), color.getGreen(), color.getBlue());

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 顏色脈衝效果（在兩個顏色之間振盪）
     * @param particle 粒子
     * @param color1 第一個顏色
     * @param color2 第二個顏色
     * @param period 週期（tick）
     * @return 任務 UUID
     */
    public static UUID pulse(ControlableParticle particle, Color color1, Color color2, int period) {
        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                // 使用正弦波
                float progress = (float) Math.sin(2 * Math.PI * tick / period);
                // 映射到 [0, 1] 範圍
                progress = (progress + 1.0f) / 2.0f;

                Color currentColor = lerpColor(color1, color2, progress);
                particle.setColor(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 顏色閃爍效果
     * @param particle 粒子
     * @param color 目標顏色
     * @param onDuration 顯示持續時間（tick）
     * @param offDuration 關閉持續時間（tick）
     * @return 任務 UUID
     */
    public static UUID blink(ControlableParticle particle, Color color, int onDuration, int offDuration) {
        Color originalColor = new Color(
            particle.getRed(),
            particle.getGreen(),
            particle.getBlue()
        );

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

                    Color targetColor = isOn ? color : originalColor;
                    particle.setColor(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue());
                }

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 淡化顏色到白色
     * @param particle 粒子
     * @param duration 持續時間（tick）
     */
    public static void fadeToWhite(ControlableParticle particle, int duration) {
        Color currentColor = new Color(
            particle.getRed(),
            particle.getGreen(),
            particle.getBlue()
        );
        colorTransition(particle, currentColor, Color.WHITE, duration);
    }

    /**
     * 淡化顏色到黑色
     * @param particle 粒子
     * @param duration 持續時間（tick）
     */
    public static void fadeToBlack(ControlableParticle particle, int duration) {
        Color currentColor = new Color(
            particle.getRed(),
            particle.getGreen(),
            particle.getBlue()
        );
        colorTransition(particle, currentColor, Color.BLACK, duration);
    }

    /**
     * 設置粒子顏色
     * @param particle 粒子
     * @param color 顏色
     */
    public static void setColor(ControlableParticle particle, Color color) {
        particle.setColor(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 設置粒子顏色（RGB）
     * @param particle 粒子
     * @param r 紅色分量（0-255）
     * @param g 綠色分量（0-255）
     * @param b 藍色分量（0-255）
     */
    public static void setColor(ControlableParticle particle, int r, int g, int b) {
        particle.setColor(r, g, b);
    }

    /**
     * 隨機顏色
     * @param particle 粒子
     */
    public static void randomColor(ControlableParticle particle) {
        int r = (int) (Math.random() * 256);
        int g = (int) (Math.random() * 256);
        int b = (int) (Math.random() * 256);
        particle.setColor(r, g, b);
    }

    /**
     * 顏色線性插值
     * @param from 起始顏色
     * @param to 結束顏色
     * @param progress 進度（0.0-1.0）
     * @return 插值後的顏色
     */
    private static Color lerpColor(Color from, Color to, float progress) {
        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * progress);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * progress);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * progress);

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new Color(r, g, b);
    }

    /**
     * 緩入緩出顏色變化
     * @param particle 粒子
     * @param fromColor 起始顏色
     * @param toColor 結束顏色
     * @param duration 持續時間（tick）
     */
    public static void smoothColorTransition(ControlableParticle particle, Color fromColor, Color toColor, int duration) {
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

                Color currentColor = lerpColor(fromColor, toColor, smoothProgress);
                particle.setColor(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 火焰顏色效果（從黃色到紅色漸變）
     * @param particle 粒子
     * @param duration 持續時間（tick）
     */
    public static void fireEffect(ControlableParticle particle, int duration) {
        Color yellow = new Color(255, 255, 0);
        Color orange = new Color(255, 128, 0);
        Color red = new Color(255, 0, 0);

        // 黃 -> 橙
        colorTransition(particle, yellow, orange, duration / 2);

        // 橙 -> 紅
        CooScheduler.getInstance().runTaskLater(() -> {
            if (!particle.isRemoved()) {
                colorTransition(particle, orange, red, duration / 2);
            }
        }, duration / 2);
    }
}
