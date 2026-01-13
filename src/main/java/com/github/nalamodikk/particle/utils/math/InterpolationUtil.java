package com.github.nalamodikk.particle.utils.math;

/**
 * 插值工具類
 * 提供各種插值函數
 */
public class InterpolationUtil {

    /**
     * 線性插值
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double linear(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    /**
     * 緩入（二次方）
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double easeIn(double start, double end, double progress) {
        return linear(start, end, progress * progress);
    }

    /**
     * 緩出（二次方）
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double easeOut(double start, double end, double progress) {
        return linear(start, end, 1 - (1 - progress) * (1 - progress));
    }

    /**
     * 緩入緩出
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double easeInOut(double start, double end, double progress) {
        double t = progress * progress * (3.0 - 2.0 * progress);
        return linear(start, end, t);
    }

    /**
     * 三次貝茲曲線插值
     * @param p0 起點
     * @param p1 控制點1
     * @param p2 控制點2
     * @param p3 終點
     * @param t 參數（0.0 - 1.0）
     * @return 插值結果
     */
    public static double cubicBezier(double p0, double p1, double p2, double p3, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;
        
        return uuu * p0 + 3 * uu * t * p1 + 3 * u * tt * p2 + ttt * p3;
    }

    /**
     * Catmull-Rom 樣條插值
     * @param p0 前一個點
     * @param p1 起點
     * @param p2 終點
     * @param p3 後一個點
     * @param t 參數（0.0 - 1.0）
     * @return 插值結果
     */
    public static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        
        return 0.5 * ((2 * p1) +
                     (-p0 + p2) * t +
                     (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
                     (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    /**
     * 彈性插值
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double elastic(double start, double end, double progress) {
        if (progress == 0 || progress == 1) {
            return progress == 0 ? start : end;
        }
        
        double p = 0.3;
        double s = p / 4.0;
        double range = end - start;
        
        return start + range * (Math.pow(2, -10 * progress) * 
                               Math.sin((progress - s) * (2 * Math.PI) / p) + 1);
    }

    /**
     * 彈跳插值
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double bounce(double start, double end, double progress) {
        double range = end - start;
        double t = progress;
        
        if (t < 1 / 2.75) {
            return start + range * (7.5625 * t * t);
        } else if (t < 2 / 2.75) {
            t -= 1.5 / 2.75;
            return start + range * (7.5625 * t * t + 0.75);
        } else if (t < 2.5 / 2.75) {
            t -= 2.25 / 2.75;
            return start + range * (7.5625 * t * t + 0.9375);
        } else {
            t -= 2.625 / 2.75;
            return start + range * (7.5625 * t * t + 0.984375);
        }
    }

    /**
     * 回彈插值
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double back(double start, double end, double progress) {
        double s = 1.70158;
        double range = end - start;
        return start + range * (progress * progress * ((s + 1) * progress - s));
    }

    /**
     * 圓形插值
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double circular(double start, double end, double progress) {
        double range = end - start;
        return start + range * (1 - Math.sqrt(1 - progress * progress));
    }

    /**
     * 正弦插值
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double sine(double start, double end, double progress) {
        double range = end - start;
        return start + range * (1 - Math.cos(progress * Math.PI / 2));
    }

    /**
     * 指數插值
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double exponential(double start, double end, double progress) {
        if (progress == 0) return start;
        if (progress == 1) return end;
        
        double range = end - start;
        return start + range * Math.pow(2, 10 * (progress - 1));
    }

    /**
     * 限制值在範圍內
     * @param value 值
     * @param min 最小值
     * @param max 最大值
     * @return 限制後的值
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 將值從一個範圍映射到另一個範圍
     * @param value 值
     * @param inMin 輸入最小值
     * @param inMax 輸入最大值
     * @param outMin 輸出最小值
     * @param outMax 輸出最大值
     * @return 映射後的值
     */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + (outMax - outMin) * ((value - inMin) / (inMax - inMin));
    }

    /**
     * 平滑步進（Hermite插值）
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double smoothStep(double start, double end, double progress) {
        progress = clamp(progress, 0, 1);
        progress = progress * progress * (3 - 2 * progress);
        return linear(start, end, progress);
    }

    /**
     * 更平滑的步進（Perlin插值）
     * @param start 起始值
     * @param end 結束值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值結果
     */
    public static double smootherStep(double start, double end, double progress) {
        progress = clamp(progress, 0, 1);
        progress = progress * progress * progress * (progress * (progress * 6 - 15) + 10);
        return linear(start, end, progress);
    }
}
