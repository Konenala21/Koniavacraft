package com.github.nalamodikk.particle.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class GraphMathHelper {

    public static float lerp(float delta, float min, float max) {
        return Mth.lerp(delta, min, max);
    }
    
    public static double lerp(double delta, double min, double max) {
        return Mth.lerp(delta, min, max);
    }

    public static Vec3 lerp(double delta, Vec3 min, Vec3 max) {
        return min.lerp(max, delta);
    }

    public static Vector3f lerp(float delta, Vector3f min, Vector3f max) {
        return new Vector3f(min).lerp(max, delta);
    }

    /**
     * SmoothStep 插值
     * 當 x < min 時返回 0
     * 當 x > max 時返回 1
     * 否則返回 0..1 之間的平滑 Hermite 插值
     */
    public static float smoothStep(float min, float max, float x) {
        if (x <= min) return 0.0f;
        if (x >= max) return 1.0f;
        
        float t = (x - min) / (max - min);
        return t * t * (3.0f - 2.0f * t);
    }
}
