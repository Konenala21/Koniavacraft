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

    /**
     * 讓圖形的對稱軸指向某個點 (圖形跟著轉變)
     */
    public static List<RelativeLocation> rotatePointsToPoint(List<RelativeLocation> shape, RelativeLocation toPoint, RelativeLocation axis) {
        if (shape.isEmpty()) return shape;

        // 計算旋轉四元數
        org.joml.Quaterniond q = new org.joml.Quaterniond();
        
        RelativeLocation na = axis.normalize();
        double axisYaw = Math.atan2(-na.x, na.z);
        double axisPitch = Math.atan2(na.y, Math.sqrt(na.x * na.x + na.z * na.z));

        RelativeLocation toa = toPoint.normalize();
        double toYaw = Math.atan2(-toa.x, toa.z);
        double toPitch = Math.atan2(toa.y, Math.sqrt(toa.x * toa.x + toa.z * toa.z));

        // 先讓圖形面向 Z 軸
        q.rotateY(axisYaw).rotateLocalX(axisPitch);
        
        // 再轉向目標點
        org.joml.Quaterniond toQ = new org.joml.Quaterniond()
            .rotateY(-toYaw)
            .rotateX(-toPitch);

        // 應用旋轉 (同樣使用並行流優化)
        shape.parallelStream().forEach(it -> {
            org.joml.Vector3d vector = new org.joml.Vector3d(it.x, it.y, it.z);
            vector.rotate(q);
            vector.rotate(toQ);
            it.x = vector.x;
            it.y = vector.y;
            it.z = vector.z;
        });

        return shape;
    }
    
    /**
     * 生成三次貝茲曲線
     */
    public static List<RelativeLocation> generateBezierCurve(
        RelativeLocation target,
        RelativeLocation startHandle,
        RelativeLocation endHandle,
        int count
    ) {
        if (count < 1) throw new IllegalArgumentException("Count must be at least 1");
        
        List<RelativeLocation> result = new ArrayList<>(count);
        RelativeLocation end = target.plus(endHandle);
        
        // P0 = (0,0,0) (因為是 RelativeLocation)
        // P1 = startHandle
        // P2 = target + endHandle
        // P3 = target
        
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            double u = 1 - t;
            double u2 = u * u;
            double t2 = t * t;
            double u3 = u2 * u;
            double t3 = t2 * t;
            
            // B(t) = (1-t)^3 P0 + 3(1-t)^2 t P1 + 3(1-t)t^2 P2 + t^3 P3
            // P0 is origin (0,0,0) so first term is 0
            
            double x = (3 * u2 * t * startHandle.x) +
                       (3 * u * t2 * end.x) +
                       (t3 * target.x);
                       
            double y = (3 * u2 * t * startHandle.y) +
                       (3 * u * t2 * end.y) +
                       (t3 * target.y);
                       
            double z = (3 * u2 * t * startHandle.z) +
                       (3 * u * t2 * end.z) +
                       (t3 * target.z);
                       
            result.add(new RelativeLocation(x, y, z));
        }
        
        return result;
    }
    
    /**
     * 生成閃電效果節點 (遞歸分形)
     */
    public static List<RelativeLocation> getLightningEffectNodes(
        RelativeLocation start, RelativeLocation end, int generations, double offsetRange
    ) {
        List<RelativeLocation> res = new ArrayList<>();
        res.add(start);
        res.addAll(getLightningNodesRecursive(start, end, generations, offsetRange));
        res.add(end);
        return res;
    }

    private static List<RelativeLocation> getLightningNodesRecursive(
        RelativeLocation start, RelativeLocation end, int generations, double offsetRange
    ) {
        List<RelativeLocation> res = new ArrayList<>();
        if (generations <= 0) return res;

        RelativeLocation mid = start.plus(end).multiply(0.5);
        // 隨機偏移 (這裡需要一個隨機向量生成器，暫時用簡單的)
        double dx = (Math.random() - 0.5) * 2 * offsetRange;
        double dy = (Math.random() - 0.5) * 2 * offsetRange;
        double dz = (Math.random() - 0.5) * 2 * offsetRange;
        mid.add(dx, dy, dz);

        res.addAll(getLightningNodesRecursive(start, mid, generations - 1, offsetRange / 2));
        res.add(mid);
        res.addAll(getLightningNodesRecursive(mid, end, generations - 1, offsetRange / 2));
        
        return res;
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