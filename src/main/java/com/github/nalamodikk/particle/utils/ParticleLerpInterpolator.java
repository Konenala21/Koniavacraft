package com.github.nalamodikk.particle.utils;

import org.joml.Quaternionf;
import org.joml.Vector3d;

/**
 * 粒子插值工具
 * 用於計算 Partial Tick 下的平滑位置與旋轉
 */
public class ParticleLerpInterpolator {

    /**
     * 線性插值計算平滑位置
     * P_render = P_prev + (P_curr - P_prev) * delta
     */
    public static Vector3d lerpPosition(double prevX, double prevY, double prevZ,
                                        double currX, double currY, double currZ,
                                        float partialTick) {
        double x = prevX + (currX - prevX) * partialTick;
        double y = prevY + (currY - prevY) * partialTick;
        double z = prevZ + (currZ - prevZ) * partialTick;
        return new Vector3d(x, y, z);
    }

    /**
     * 四元數插值計算平滑旋轉
     */
    public static Quaternionf lerpRotation(Quaternionf prevRot, Quaternionf currRot, float partialTick) {
        return QuaternionUtil.slerp(prevRot, currRot, partialTick);
    }
}
