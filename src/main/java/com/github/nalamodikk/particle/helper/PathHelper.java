package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 路徑跟隨助手
 * 提供沿自定義路徑移動的功能
 */
public class PathHelper {

    /**
     * 沿路徑移動（線性插值）
     * @param particle 粒子
     * @param waypoints 路徑點列表
     * @param duration 總持續時間（tick）
     */
    public static void followPath(ControlableParticle particle, List<Vec3> waypoints, int duration) {
        if (waypoints == null || waypoints.size() < 2) {
            return;
        }

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) tick / duration;
                Vec3 position = interpolateAlongPath(waypoints, progress);
                particle.teleportTo(position);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 循環路徑移動
     * @param particle 粒子
     * @param waypoints 路徑點列表
     * @param cycleDuration 一個循環的持續時間（tick）
     * @return 任務 UUID
     */
    public static UUID followPathLooped(ControlableParticle particle, List<Vec3> waypoints, int cycleDuration) {
        if (waypoints == null || waypoints.size() < 2) {
            return null;
        }

        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = (float) (tick % cycleDuration) / cycleDuration;
                Vec3 position = interpolateAlongPath(waypoints, progress);
                particle.teleportTo(position);

                tick++;
            }
        }, 0, 1);
    }

    /**
     * 沿路徑移動並在終點停留
     * @param particle 粒子
     * @param waypoints 路徑點列表
     * @param duration 總持續時間（tick）
     */
    public static void followPathAndStop(ControlableParticle particle, List<Vec3> waypoints, int duration) {
        if (waypoints == null || waypoints.size() < 2) {
            return;
        }

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = Math.min((float) tick / duration, 1.0f);
                Vec3 position = interpolateAlongPath(waypoints, progress);
                particle.teleportTo(position);

                tick++;
            }
        }, 0, 1, duration + 1);
    }

    /**
     * 反向跟隨路徑
     * @param particle 粒子
     * @param waypoints 路徑點列表
     * @param duration 總持續時間（tick）
     */
    public static void followPathReverse(ControlableParticle particle, List<Vec3> waypoints, int duration) {
        if (waypoints == null || waypoints.size() < 2) {
            return;
        }

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float progress = 1.0f - ((float) tick / duration);
                Vec3 position = interpolateAlongPath(waypoints, progress);
                particle.teleportTo(position);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 在路徑上進行插值計算
     * @param waypoints 路徑點列表
     * @param progress 進度（0.0 - 1.0）
     * @return 插值後的位置
     */
    private static Vec3 interpolateAlongPath(List<Vec3> waypoints, float progress) {
        if (waypoints.size() == 1) {
            return waypoints.get(0);
        }

        // 將進度映射到路徑段
        float scaledProgress = progress * (waypoints.size() - 1);
        int segmentIndex = Math.min((int) scaledProgress, waypoints.size() - 2);
        float segmentProgress = scaledProgress - segmentIndex;

        Vec3 start = waypoints.get(segmentIndex);
        Vec3 end = waypoints.get(segmentIndex + 1);

        return start.lerp(end, segmentProgress);
    }

    /**
     * 沿路徑移動（帶緩入緩出）
     * @param particle 粒子
     * @param waypoints 路徑點列表
     * @param duration 總持續時間（tick）
     */
    public static void followPathSmooth(ControlableParticle particle, List<Vec3> waypoints, int duration) {
        if (waypoints == null || waypoints.size() < 2) {
            return;
        }

        CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                float t = (float) tick / duration;
                // 緩入緩出公式：3t² - 2t³
                float smoothProgress = t * t * (3.0f - 2.0f * t);

                Vec3 position = interpolateAlongPath(waypoints, smoothProgress);
                particle.teleportTo(position);

                tick++;
            }
        }, 0, 1, duration);
    }

    /**
     * 來回移動（乒乓模式）
     * @param particle 粒子
     * @param waypoints 路徑點列表
     * @param cycleDuration 一個循環的持續時間（tick）
     * @return 任務 UUID
     */
    public static UUID followPathPingPong(ControlableParticle particle, List<Vec3> waypoints, int cycleDuration) {
        if (waypoints == null || waypoints.size() < 2) {
            return null;
        }

        return CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    return;
                }

                int cyclePosition = tick % (cycleDuration * 2);
                float progress;

                if (cyclePosition < cycleDuration) {
                    // 正向移動
                    progress = (float) cyclePosition / cycleDuration;
                } else {
                    // 反向移動
                    progress = 1.0f - (float) (cyclePosition - cycleDuration) / cycleDuration;
                }

                Vec3 position = interpolateAlongPath(waypoints, progress);
                particle.teleportTo(position);

                tick++;
            }
        }, 0, 1);
    }
}
