package com.github.nalamodikk.particle.utils.math;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * 相對位置類
 * 類似於 Vec3，但是可變的 (Mutable)，方便進行高效的幾何運算
 */
public class RelativeLocation implements Cloneable {
    public double x;
    public double y;
    public double z;

    public RelativeLocation(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public RelativeLocation() {
        this(0, 0, 0);
    }

    public static RelativeLocation of(Vec3 vec) {
        return new RelativeLocation(vec.x, vec.y, vec.z);
    }

    public static RelativeLocation of(Vector3d vec) {
        return new RelativeLocation(vec.x, vec.y, vec.z);
    }

    public static RelativeLocation of(double x, double y, double z) {
        return new RelativeLocation(x, y, z);
    }
    
    public static RelativeLocation yAxis() {
        return new RelativeLocation(0, 1, 0);
    }

    public Vec3 toVector() {
        return new Vec3(x, y, z);
    }

    public Vector3d toVector3d() {
        return new Vector3d(x, y, z);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    public double distance(RelativeLocation other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public RelativeLocation normalize() {
        double len = length();
        if (len != 0) {
            return new RelativeLocation(x / len, y / len, z / len);
        }
        return new RelativeLocation(0, 0, 0);
    }

    public RelativeLocation add(RelativeLocation other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }
    
    public RelativeLocation add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public RelativeLocation subtract(RelativeLocation other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        return this;
    }

    public RelativeLocation multiply(double factor) {
        this.x *= factor;
        this.y *= factor;
        this.z *= factor;
        return this;
    }
    
    public RelativeLocation multiplyClone(double factor) {
        return this.clone().multiply(factor);
    }

    public double dot(RelativeLocation other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public RelativeLocation cross(RelativeLocation other) {
        return new RelativeLocation(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        );
    }

    @Override
    public RelativeLocation clone() {
        return new RelativeLocation(x, y, z);
    }

    @Override
    public String toString() {
        return "RelativeLocation{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
    
    // 運算符重載模擬 (Java 不支援，但提供 fluent API)
    public RelativeLocation plus(RelativeLocation other) {
        return new RelativeLocation(x + other.x, y + other.y, z + other.z);
    }
    
    public RelativeLocation minus(RelativeLocation other) {
        return new RelativeLocation(x - other.x, y - other.y, z - other.z);
    }
}
