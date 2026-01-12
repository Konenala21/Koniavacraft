package com.github.nalamodikk.particle.utils.math;

import com.github.nalamodikk.particle.utils.RelativeLocation;

public class RotationMatrix {
    private final double[][] matrix;

    private RotationMatrix(double[][] matrix) {
        this.matrix = matrix;
    }

    public static RotationMatrix fromAxisAngle(RelativeLocation axis, double angle) {
        RelativeLocation u = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double oneMinusCos = 1 - cos;

        return new RotationMatrix(new double[][]{
            {
                cos + u.x * u.x * oneMinusCos,
                u.x * u.y * oneMinusCos - u.z * sin,
                u.x * u.z * oneMinusCos + u.y * sin
            },
            {
                u.y * u.x * oneMinusCos + u.z * sin,
                cos + u.y * u.y * oneMinusCos,
                u.y * u.z * oneMinusCos - u.x * sin
            },
            {
                u.z * u.x * oneMinusCos - u.y * sin,
                u.z * u.y * oneMinusCos + u.x * sin,
                cos + u.z * u.z * oneMinusCos
            }
        });
    }

    public RelativeLocation applyToClone(RelativeLocation point) {
        return applyTo(point.clone());
    }

    public RelativeLocation applyTo(RelativeLocation point) {
        double x = point.x;
        double y = point.y;
        double z = point.z;
        point.x = (matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z);
        point.y = (matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z);
        point.z = (matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z);
        return point;
    }
}
