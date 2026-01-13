package com.github.nalamodikk.particle.utils.math;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 3D 數學工具類
 */
public class Math3DUtil {

    public static double distance(Vec3 p1, Vec3 p2) {
        return p1.distanceTo(p2);
    }

    public static double distanceSqr(Vec3 p1, Vec3 p2) {
        return p1.distanceToSqr(p2);
    }

    public static Vec3 normalize(Vec3 vec) {
        return vec.normalize();
    }

    public static double dot(Vec3 v1, Vec3 v2) {
        return v1.dot(v2);
    }

    public static Vec3 cross(Vec3 v1, Vec3 v2) {
        return v1.cross(v2);
    }

    public static Vec3 lerp(Vec3 start, Vec3 end, double progress) {
        return start.lerp(end, progress);
    }

    public static Vec3 project(Vec3 vec, Vec3 onto) {
        double ontoLengthSq = onto.lengthSqr();
        if (ontoLengthSq == 0) return Vec3.ZERO;
        
        double projection = vec.dot(onto) / ontoLengthSq;
        return onto.scale(projection);
    }

    public static Vec3 reflect(Vec3 vec, Vec3 normal) {
        double dotProduct = vec.dot(normal);
        return vec.subtract(normal.scale(2 * dotProduct));
    }

    public static double angleBetween(Vec3 v1, Vec3 v2) {
        double dot = v1.normalize().dot(v2.normalize());
        return Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
    }

    public static Vec3 rotate(Vec3 vec, Vec3 axis, double angle) {
        Quaternionf quat = new Quaternionf().fromAxisAngleRad(
            (float) axis.x, (float) axis.y, (float) axis.z, (float) angle
        );
        
        Vector3f result = new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
        quat.transform(result);
        
        return new Vec3(result.x, result.y, result.z);
    }

    public static Vec3 rotateAroundY(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(
            vec.x * cos + vec.z * sin,
            vec.y,
            -vec.x * sin + vec.z * cos
        );
    }

    public static boolean isPointInSphere(Vec3 point, Vec3 sphereCenter, double radius) {
        return point.distanceToSqr(sphereCenter) <= radius * radius;
    }

    public static Vec3 clampLength(Vec3 vec, double maxLength) {
        double lengthSq = vec.lengthSqr();
        if (lengthSq > maxLength * maxLength) {
            return vec.normalize().scale(maxLength);
        }
        return vec;
    }

    public static Vec3 randomUnitVector() {
        double theta = Math.random() * 2 * Math.PI;
        double phi = Math.acos(2 * Math.random() - 1);
        
        double x = Math.sin(phi) * Math.cos(theta);
        double y = Math.sin(phi) * Math.sin(theta);
        double z = Math.cos(phi);
        
        return new Vec3(x, y, z);
    }
}
