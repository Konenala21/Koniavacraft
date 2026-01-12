package com.github.nalamodikk.network.animation;

import net.minecraft.world.phys.Vec3;

/**
 * 路徑運動計算工具
 * 提供各種數學曲線路徑
 */
public class PathMotion {

    /**
     * 獲取螺旋路徑上的點
     * 
     * @param center 中心點
     * @param radius 半徑
     * @param angle  當前角度 (弧度)
     * @param height 當前高度
     */
    public static Vec3 getSpiral(Vec3 center, double radius, double angle, double height) {
        return new Vec3(
                center.x + Math.cos(angle) * radius,
                center.y + height,
                center.z + Math.sin(angle) * radius);
    }

    /**
     * 二階貝茲曲線 (Bezier Curve)
     */
    public static Vec3 getBezier(Vec3 start, Vec3 control, Vec3 end, float t) {
        double invT = 1.0 - t;
        return start.scale(invT * invT)
                .add(control.scale(2 * invT * t))
                .add(end.scale(t * t));
    }
}
