package com.github.nalamodikk.particle.animation;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 路徑運動
 * 定義粒子沿路徑移動的行為
 */
public class PathMotion {
    private final List<Vec3> pathPoints;
    private final int totalDuration;
    private final InterpolationType interpolation;
    private final boolean loop;
    private final boolean reverse;

    private PathMotion(Builder builder) {
        this.pathPoints = new ArrayList<>(builder.pathPoints);
        this.totalDuration = builder.duration;
        this.interpolation = builder.interpolation;
        this.loop = builder.loop;
        this.reverse = builder.reverse;
    }

    /**
     * 根據進度獲取路徑上的位置
     * @param progress 進度（0.0 - 1.0）
     * @return 插值後的位置
     */
    public Vec3 getPositionAt(float progress) {
        if (pathPoints.isEmpty()) {
            return Vec3.ZERO;
        }

        if (pathPoints.size() == 1) {
            return pathPoints.get(0);
        }

        // 處理循環
        if (loop) {
            progress = progress % 1.0f;
        } else {
            progress = Math.max(0.0f, Math.min(1.0f, progress));
        }

        // 處理反向
        if (reverse) {
            progress = 1.0f - progress;
        }

        // 應用插值
        float interpolatedProgress = interpolation.interpolate(progress);

        // 計算路徑位置
        return interpolateAlongPath(interpolatedProgress);
    }

    /**
     * 根據 tick 獲取位置
     * @param currentTick 當前 tick
     * @return 位置
     */
    public Vec3 getPositionAtTick(int currentTick) {
        float progress = (float) currentTick / totalDuration;
        return getPositionAt(progress);
    }

    /**
     * 獲取路徑總時長
     * @return 總時長（tick）
     */
    public int getTotalDuration() {
        return totalDuration;
    }

    /**
     * 獲取路徑點數量
     * @return 路徑點數量
     */
    public int getPointCount() {
        return pathPoints.size();
    }

    /**
     * 獲取插值類型
     * @return 插值類型
     */
    public InterpolationType getInterpolation() {
        return interpolation;
    }

    /**
     * 是否循環
     * @return 是否循環
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * 在路徑上進行插值
     * @param progress 進度（0.0 - 1.0）
     * @return 插值後的位置
     */
    private Vec3 interpolateAlongPath(float progress) {
        if (pathPoints.size() == 1) {
            return pathPoints.get(0);
        }

        // 將進度映射到路徑段
        float scaledProgress = progress * (pathPoints.size() - 1);
        int segmentIndex = Math.min((int) scaledProgress, pathPoints.size() - 2);
        float segmentProgress = scaledProgress - segmentIndex;

        Vec3 start = pathPoints.get(segmentIndex);
        Vec3 end = pathPoints.get(segmentIndex + 1);

        return start.lerp(end, segmentProgress);
    }

    /**
     * 獲取路徑方向（切線）
     * @param progress 進度（0.0 - 1.0）
     * @return 方向向量（已歸一化）
     */
    public Vec3 getDirectionAt(float progress) {
        if (pathPoints.size() < 2) {
            return new Vec3(0, 1, 0); // 默認向上
        }

        // 使用微小的增量計算切線
        float delta = 0.01f;
        Vec3 pos1 = getPositionAt(Math.max(0, progress - delta));
        Vec3 pos2 = getPositionAt(Math.min(1, progress + delta));

        return pos2.subtract(pos1).normalize();
    }

    /**
     * 計算路徑總長度（近似）
     * @return 路徑長度
     */
    public double getTotalLength() {
        double length = 0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            length += pathPoints.get(i).distanceTo(pathPoints.get(i + 1));
        }
        return length;
    }

    /**
     * Builder 模式構建 PathMotion
     */
    public static class Builder {
        private final List<Vec3> pathPoints = new ArrayList<>();
        private int duration = 100;
        private InterpolationType interpolation = InterpolationType.LINEAR;
        private boolean loop = false;
        private boolean reverse = false;

        /**
         * 添加路徑點
         * @param point 路徑點
         * @return Builder
         */
        public Builder addPoint(Vec3 point) {
            this.pathPoints.add(point);
            return this;
        }

        /**
         * 添加多個路徑點
         * @param points 路徑點陣列
         * @return Builder
         */
        public Builder addPoints(Vec3... points) {
            this.pathPoints.addAll(Arrays.asList(points));
            return this;
        }

        /**
         * 添加路徑點列表
         * @param points 路徑點列表
         * @return Builder
         */
        public Builder addPoints(List<Vec3> points) {
            this.pathPoints.addAll(points);
            return this;
        }

        /**
         * 設置總時長
         * @param duration 時長（tick）
         * @return Builder
         */
        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        /**
         * 設置插值類型
         * @param interpolation 插值類型
         * @return Builder
         */
        public Builder interpolation(InterpolationType interpolation) {
            this.interpolation = interpolation;
            return this;
        }

        /**
         * 設置循環
         * @param loop 是否循環
         * @return Builder
         */
        public Builder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        /**
         * 設置反向
         * @param reverse 是否反向
         * @return Builder
         */
        public Builder reverse(boolean reverse) {
            this.reverse = reverse;
            return this;
        }

        /**
         * 構建 PathMotion
         * @return PathMotion 實例
         */
        public PathMotion build() {
            if (pathPoints.isEmpty()) {
                throw new IllegalStateException("PathMotion must have at least one path point");
            }
            return new PathMotion(this);
        }
    }

    /**
     * 創建線性路徑
     * @param start 起點
     * @param end 終點
     * @param duration 時長
     * @return PathMotion
     */
    public static PathMotion linear(Vec3 start, Vec3 end, int duration) {
        return new Builder()
            .addPoint(start)
            .addPoint(end)
            .duration(duration)
            .interpolation(InterpolationType.LINEAR)
            .build();
    }

    /**
     * 創建平滑路徑
     * @param start 起點
     * @param end 終點
     * @param duration 時長
     * @return PathMotion
     */
    public static PathMotion smooth(Vec3 start, Vec3 end, int duration) {
        return new Builder()
            .addPoint(start)
            .addPoint(end)
            .duration(duration)
            .interpolation(InterpolationType.EASE_IN_OUT)
            .build();
    }

    /**
     * 創建圓形路徑
     * @param center 中心點
     * @param radius 半徑
     * @param segments 段數
     * @param duration 時長
     * @return PathMotion
     */
    public static PathMotion circular(Vec3 center, double radius, int segments, int duration) {
        Builder builder = new Builder().duration(duration).loop(true);

        for (int i = 0; i < segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            builder.addPoint(new Vec3(x, center.y, z));
        }

        return builder.build();
    }
}
