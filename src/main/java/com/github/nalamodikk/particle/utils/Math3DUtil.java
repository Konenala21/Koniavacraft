package com.github.nalamodikk.particle.utils;

import com.github.nalamodikk.particle.utils.math.RelativeLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 3D 數學工具類
 * 移植自 CooParticlesAPI 的 Math3DUtil
 */
public class Math3DUtil {

    /**
     * 在 XZ 平面上生成圓形點集
     */
    public static List<RelativeLocation> getCircleXZ(double r, int count) {
        List<RelativeLocation> res = new ArrayList<>(count);
        double step = 2 * Math.PI / count;
        double angle = 0.0;
        for (int i = 0; i < count; i++) {
            res.add(new RelativeLocation(
                r * Math.cos(angle), 0.0, r * Math.sin(angle)
            ));
            angle += step;
        }
        return res;
    }
    
    /**
     * 在 XZ 平面上生成半圓點集
     */
    public static List<RelativeLocation> getHalfCircleXZ(double r, int count, double rotate) {
        List<RelativeLocation> res = new ArrayList<>(count);
        double step = Math.PI / count;
        double angle = 0.0;
        for (int i = 0; i < count; i++) {
            res.add(new RelativeLocation(
                r * Math.cos(angle), 0.0, r * Math.sin(angle)
            ));
            angle += step;
        }
        if (rotate != 0.0) {
            rotateAsAxis(res, RelativeLocation.yAxis(), rotate);
        }
        return res;
    }

    /**
     * 生成兩點之間的線段點集
     */
    public static List<RelativeLocation> getLineLocations(Vec3 start, Vec3 end, int count) {
        List<RelativeLocation> res = new ArrayList<>(count);
        Vec3 diff = end.subtract(start);
        double step = 1.0 / count;
        for (int i = 0; i <= count; i++) {
            double t = i * step;
            res.add(RelativeLocation.of(start.add(diff.scale(t))));
        }
        return res;
    }

    public static List<RelativeLocation> getLineLocations(RelativeLocation start, RelativeLocation end, int count) {
        return getLineLocations(start.toVector(), end.toVector(), count);
    }
    
    /**
     * 生成圓內接正多邊形
     */
    public static List<RelativeLocation> getPolygonInCircleLocations(int n, int edgeCount, double r) {
        if (n < 3) throw new IllegalArgumentException("n must be at least 3");
        
        List<RelativeLocation> vertices = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double theta = 2 * Math.PI * i / n;
            vertices.add(new RelativeLocation(r * Math.cos(theta), 0, r * Math.sin(theta)));
        }
        
        List<RelativeLocation> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            RelativeLocation start = vertices.get(i);
            RelativeLocation end = vertices.get((i + 1) % n);
            // 注意：這裡直接使用我們自己的 getLineLocations，避免依賴 Vec3
            result.addAll(getLineLocations(start, end, edgeCount));
        }
        return result;
    }

    /**
     * 繞軸旋轉點集
     * 使用 Java Parallel Stream 進行並行計算
     */
    public static List<RelativeLocation> rotateAsAxis(List<RelativeLocation> locList, RelativeLocation axis, double angle) {
        RotationMatrix matrix = RotationMatrix.fromAxisAngle(axis, angle);
        
        // 為了效能，如果列表很小，直接串行處理
        if (locList.size() < 100) {
            for (RelativeLocation loc : locList) {
                matrix.applyTo(loc);
            }
            return locList;
        }

        // 大量數據並行處理
        locList.parallelStream().forEach(matrix::applyTo);
        return locList;
    }
    
    public static Vector3f colorOf(int r, int g, int b) {
        return new Vector3f(r / 255f, g / 255f, b / 255f);
    }
    
    // 轉換角度 (度 -> 弧度)
    public static double toRadians(double degrees) {
        return Math.toRadians(degrees);
    }
    
    // 轉換角度 (弧度 -> 度)
    public static double toDegrees(double radians) {
        return Math.toDegrees(radians);
    }
}
