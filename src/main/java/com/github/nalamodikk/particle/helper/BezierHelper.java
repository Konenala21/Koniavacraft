package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

/**
 * 貝茲曲線運動助手
 * 提供平滑的曲線路徑運動
 */
public class BezierHelper {

    /**
     * 二次貝茲曲線運動
     * @param particle 粒子
     * @param p0 起始點
     * @param p1 控制點
     * @param p2 結束點
     * @param duration 持續時間（tick）
     */
    public static void quadraticBezier(ControlableParticle particle, Vec3 p0, Vec3 p1, Vec3 p2, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                Vec3 pos = quadraticBezierPoint(p0, p1, p2, t);
                particle.teleportTo(pos);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 三次貝茲曲線運動
     * @param particle 粒子
     * @param p0 起始點
     * @param p1 第一控制點
     * @param p2 第二控制點
     * @param p3 結束點
     * @param duration 持續時間（tick）
     */
    public static void cubicBezier(ControlableParticle particle, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                Vec3 pos = cubicBezierPoint(p0, p1, p2, p3, t);
                particle.teleportTo(pos);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 沿貝茲曲線路徑移動（支持多個控制點）
     * @param particle 粒子
     * @param controlPoints 控制點列表（至少 2 個點）
     * @param duration 持續時間（tick）
     */
    public static void followBezierPath(ControlableParticle particle, Vec3[] controlPoints, int duration) {
        if (controlPoints.length < 2) {
            throw new IllegalArgumentException("At least 2 control points required");
        }

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                Vec3 pos = generalBezierPoint(controlPoints, t);
                particle.teleportTo(pos);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 計算二次貝茲曲線上的點
     * B(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2
     */
    private static Vec3 quadraticBezierPoint(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float ut2 = 2 * u * t;

        return new Vec3(
            uu * p0.x + ut2 * p1.x + tt * p2.x,
            uu * p0.y + ut2 * p1.y + tt * p2.y,
            uu * p0.z + ut2 * p1.z + tt * p2.z
        );
    }

    /**
     * 計算三次貝茲曲線上的點
     * B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3
     */
    private static Vec3 cubicBezierPoint(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;
        float ut2_3 = 3 * uu * t;
        float utt_3 = 3 * u * tt;

        return new Vec3(
            uuu * p0.x + ut2_3 * p1.x + utt_3 * p2.x + ttt * p3.x,
            uuu * p0.y + ut2_3 * p1.y + utt_3 * p2.y + ttt * p3.y,
            uuu * p0.z + ut2_3 * p1.z + utt_3 * p2.z + ttt * p3.z
        );
    }

    /**
     * 通用貝茲曲線計算（De Casteljau 算法）
     * 支持任意階數的貝茲曲線
     */
    private static Vec3 generalBezierPoint(Vec3[] points, float t) {
        int n = points.length;
        Vec3[] temp = new Vec3[n];
        System.arraycopy(points, 0, temp, 0, n);

        // De Casteljau 算法
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < n - i; j++) {
                temp[j] = temp[j].lerp(temp[j + 1], t);
            }
        }

        return temp[0];
    }

    /**
     * 計算貝茲曲線上指定 t 值處的切線方向
     * @param p0 起始點
     * @param p1 第一控制點
     * @param p2 第二控制點
     * @param p3 結束點
     * @param t 參數 (0.0 - 1.0)
     * @return 歸一化的切線向量
     */
    public static Vec3 cubicBezierTangent(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float u = 1 - t;
        float uu = u * u;
        float tt = t * t;

        // 貝茲曲線導數：B'(t) = 3(1-t)²(P1-P0) + 6(1-t)t(P2-P1) + 3t²(P3-P2)
        Vec3 tangent = new Vec3(
            3 * uu * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * tt * (p3.x - p2.x),
            3 * uu * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * tt * (p3.y - p2.y),
            3 * uu * (p1.z - p0.z) + 6 * u * t * (p2.z - p1.z) + 3 * tt * (p3.z - p2.z)
        );

        return tangent.normalize();
    }

    /**
     * 貝茲曲線運動並旋轉粒子朝向運動方向
     * @param particle 粒子
     * @param p0 起始點
     * @param p1 第一控制點
     * @param p2 第二控制點
     * @param p3 結束點
     * @param duration 持續時間（tick）
     */
    public static void followBezierWithRotation(ControlableParticle particle, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int duration) {
        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;

                // 計算位置
                Vec3 pos = cubicBezierPoint(p0, p1, p2, p3, t);
                particle.teleportTo(pos);

                // 計算切線方向並設置粒子旋轉
                Vec3 tangent = cubicBezierTangent(p0, p1, p2, p3, t);
                // TODO: 設置粒子朝向（需要添加粒子旋轉功能）
                // particle.setRotationFromDirection(tangent);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * S 型曲線運動（使用兩段三次貝茲曲線）
     * @param particle 粒子
     * @param start 起始點
     * @param end 結束點
     * @param curvature 曲率（0.0-1.0，越大曲線越彎）
     * @param duration 持續時間（tick）
     */
    public static void sCurve(ControlableParticle particle, Vec3 start, Vec3 end, float curvature, int duration) {
        Vec3 direction = end.subtract(start);
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x).normalize();

        // 計算 S 曲線的控制點
        Vec3 mid = start.lerp(end, 0.5);
        Vec3 p1 = start.add(direction.scale(0.25)).add(perpendicular.scale(curvature));
        Vec3 p2 = mid.subtract(perpendicular.scale(curvature));
        Vec3 p3 = mid.add(perpendicular.scale(curvature));
        Vec3 p4 = end.subtract(direction.scale(0.25)).subtract(perpendicular.scale(curvature));

        // 使用兩段貝茲曲線
        int halfDuration = duration / 2;

        // 第一段
        cubicBezier(particle, start, p1, p2, mid, halfDuration);

        // 第二段
        CooScheduler.getInstance().runTaskLater(() -> {
            if (!particle.isRemoved()) {
                cubicBezier(particle, mid, p3, p4, end, duration - halfDuration);
            }
        }, halfDuration);
    }
}
