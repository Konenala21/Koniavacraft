package com.github.nalamodikk.particle.utils;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * 粒子系統數學工具庫
 * 提供各種幾何圖形的點生成算法
 */
public class MathUtil {

    /**
     * 生成圓形點集
     * @param radius 半徑
     * @param points 點數量
     * @return 點的列表（相對原點）
     */
    public static List<Vec3> getCirclePoints(double radius, int points) {
        List<Vec3> result = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            result.add(new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
        }
        return result;
    }

    /**
     * 生成擺線（Cycloid）/ 魔法陣圖形點集
     * 支持外擺線 (Epicycloid) 和內擺線 (Hypocycloid)
     *
     * @param R 大圓半徑
     * @param r 小圓半徑
     * @param points 總點數
     * @param winding 繞圈數 (通常為了閉合圖形需要多圈)
     * @return 點的列表
     */
    public static List<Vec3> getCycloidPoints(double R, double r, int points, double winding) {
        List<Vec3> result = new ArrayList<>(points);
        double step = (2 * Math.PI * winding) / points;

        for (int i = 0; i < points; i++) {
            double t = i * step;
            // 內擺線/外擺線通用公式 (取決於 r 的正負)
            // 這裡使用內擺線變體，因為它產生的星形/花形更像魔法陣
            double x = (R - r) * Math.cos(t) + r * Math.cos(((R - r) / r) * t);
            double z = (R - r) * Math.sin(t) - r * Math.sin(((R - r) / r) * t);
            result.add(new Vec3(x, 0, z));
        }
        return result;
    }

    /**
     * 生成螺旋線點集
     * @param startRadius 起始半徑
     * @param endRadius 結束半徑
     * @param rounds 圈數
     * @param points 點數量
     */
    public static List<Vec3> getSpiralPoints(double startRadius, double endRadius, double rounds, int points) {
        List<Vec3> result = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            double angle = 2 * Math.PI * rounds * progress;
            double currentRadius = startRadius + (endRadius - startRadius) * progress;
            
            result.add(new Vec3(Math.cos(angle) * currentRadius, 0, Math.sin(angle) * currentRadius));
        }
        return result;
    }
    
    /**
     * 生成五芒星/多芒星
     * @param outerRadius 外半徑
     * @param innerRadius 內半徑
     * @param corners 角數 (例如 5 為五芒星)
     */
    public static List<Vec3> getStarPoints(double outerRadius, double innerRadius, int corners) {
        List<Vec3> result = new ArrayList<>();
        int totalPoints = corners * 2; // 每個角有外點和內點
        
        for (int i = 0; i < totalPoints; i++) {
            double angle = Math.PI * i / corners; // 步長為 PI/corners (半個角)
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            
            // 修正角度讓星星尖端朝上
            angle -= Math.PI / 2;
            
            result.add(new Vec3(Math.cos(angle) * r, 0, Math.sin(angle) * r));
        }
        // 閉合路徑
        result.add(result.get(0));
        return result;
    }
    
    /**
     * 在兩點之間進行線性插值，生成線段上的點
     */
    public static List<Vec3> getLinePoints(Vec3 start, Vec3 end, int points) {
        List<Vec3> result = new ArrayList<>(points);
        if (points <= 1) {
            result.add(start);
            return result;
        }
        
        for (int i = 0; i < points; i++) {
            double t = (double) i / (points - 1);
            result.add(start.lerp(end, t));
        }
        return result;
    }
}
