package com.github.nalamodikk.particle.utils;

import com.github.nalamodikk.particle.utils.math.RelativeLocation;

/**
 * 旋轉矩陣工具類
 * 用於處理 RelativeLocation 的旋轉
 */
public class RotationMatrix {
    private final double[][] matrix;

    private RotationMatrix(double[][] matrix) {
        this.matrix = matrix;
    }

    /**
     * 建立繞任意軸旋轉的矩陣 (罗德里格旋转公式)
     *
     * @param axis 旋轉軸
     * @param angle 旋轉角度 (弧度)
     * @return 旋轉矩陣
     */
    public static RotationMatrix fromAxisAngle(RelativeLocation axis, double angle) {
        RelativeLocation u = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double oneMinusCos = 1.0 - cos;

        double x = u.x;
        double y = u.y;
        double z = u.z;

        double[][] m = new double[][]{
            {cos + x * x * oneMinusCos, x * y * oneMinusCos - z * sin, x * z * oneMinusCos + y * sin},
            {y * x * oneMinusCos + z * sin, cos + y * y * oneMinusCos, y * z * oneMinusCos - x * sin},
            {z * x * oneMinusCos - y * sin, z * y * oneMinusCos + x * sin, cos + z * z * oneMinusCos}
        };

        return new RotationMatrix(m);
    }

    public RelativeLocation applyToClone(RelativeLocation point) {
        return applyTo(point.clone());
    }

    public RelativeLocation applyTo(RelativeLocation point) {
        double x = point.x;
        double y = point.y;
        double z = point.z;

        point.x = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z;
        point.y = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z;
        point.z = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z;

        return point;
    }
}
