package com.github.nalamodikk.particle.utils;

/**
 * 輕量級物理工具
 * 用於粒子的碰撞檢測與物理計算
 */
public class PhysicsUtil {

    /**
     * 檢查兩個 AABB 是否相交
     * 使用 raw coordinates 以減少物件分配
     *
     * @param minX1 第一個盒子的最小 X
     * @param minY1 第一個盒子的最小 Y
     * @param minZ1 第一個盒子的最小 Z
     * @param maxX1 第一個盒子的最大 X
     * @param maxY1 第一個盒子的最大 Y
     * @param maxZ1 第一個盒子的最大 Z
     * @param minX2 第二個盒子的最小 X
     * @param minY2 第二個盒子的最小 Y
     * @param minZ2 第二個盒子的最小 Z
     * @param maxX2 第二個盒子的最大 X
     * @param maxY2 第二個盒子的最大 Y
     * @param maxZ2 第二個盒子的最大 Z
     * @return 如果相交則返回 true
     */
    public static boolean intersects(double minX1, double minY1, double minZ1,
                                     double maxX1, double maxY1, double maxZ1,
                                     double minX2, double minY2, double minZ2,
                                     double maxX2, double maxY2, double maxZ2) {
        return minX1 < maxX2 && maxX1 > minX2 &&
               minY1 < maxY2 && maxY1 > minY2 &&
               minZ1 < maxZ2 && maxZ1 > minZ2;
    }
}
