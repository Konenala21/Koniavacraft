package com.github.nalamodikk.particle.utils;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 四元數運算工具類
 * 用於處理粒子的 3D 旋轉與向量變換
 */
public class QuaternionUtil {

    /**
     * 將向量 v 根據四元數 q 進行旋轉
     * v' = q * v * q^-1
     *
     * @param v 原始向量 (不會被修改)
     * @param q 旋轉四元數
     * @return 旋轉後的新向量
     */
    public static Vector3f rotate(Vector3f v, Quaternionf q) {
        Vector3f result = new Vector3f(v);
        return result.rotate(q);
    }

    /**
     * 對兩個四元數進行球面線性插值 (SLERP)
     *
     * @param start 起始四元數
     * @param end   目標四元數
     * @param alpha 插值因子 (0.0 - 1.0)
     * @return 插值後的新四元數
     */
    public static Quaternionf slerp(Quaternionf start, Quaternionf end, float alpha) {
        Quaternionf result = new Quaternionf(start);
        return result.slerp(end, alpha);
    }
}
