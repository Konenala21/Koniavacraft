package com.github.nalamodikk.particle.utils;

import org.joml.Quaternionf;
import org.joml.Vector3d;

/**
 * ?????瞉扔?? * ??踐??殷?? Partial Tick ??????選??????? */
public class ParticleLerpInterpolator {

    /**
     * ?綜垮????瞏?????????     * P_render = P_prev + (P_curr - P_prev) * delta
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
     * ?謜??鞊??瞏?????????     */
    public static Quaternionf lerpRotation(Quaternionf prevRot, Quaternionf currRot, float partialTick) {
        return QuaternionUtil.slerp(prevRot, currRot, partialTick);
    }
}
