package com.github.nalamodikk.particle.animation;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 動畫管理器
 * 管理粒子的動畫系統
 */
public class AnimateManager {
    private static final AnimateManager INSTANCE = new AnimateManager();

    private final Map<UUID, AnimationTask> activeAnimations = new ConcurrentHashMap<>();

    private AnimateManager() {
    }

    public static AnimateManager getInstance() {
        return INSTANCE;
    }

    /**
     * 為粒子應用路徑動畫
     * @param particle 粒子
     * @param pathMotion 路徑運動
     * @return 動畫 UUID
     */
    public UUID animate(ControlableParticle particle, PathMotion pathMotion) {
        return animate(particle, pathMotion, null);
    }

    /**
     * 為粒子應用路徑動畫（帶完成回調）
     * @param particle 粒子
     * @param pathMotion 路徑運動
     * @param onComplete 完成回調
     * @return 動畫 UUID
     */
    public UUID animate(ControlableParticle particle, PathMotion pathMotion, Runnable onComplete) {
        UUID animationId = UUID.randomUUID();

        UUID schedulerId = CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int currentTick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    stopAnimation(animationId);
                    return;
                }

                // 獲取當前位置
                Vec3 position = pathMotion.getPositionAtTick(currentTick);
                particle.teleportTo(position);

                currentTick++;

                // 檢查是否完成
                if (!pathMotion.isLoop() && currentTick >= pathMotion.getTotalDuration()) {
                    stopAnimation(animationId);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }, 0, 1);

        activeAnimations.put(animationId, new AnimationTask(schedulerId, pathMotion, particle));
        return animationId;
    }

    /**
     * 為粒子應用自定義動畫
     * @param particle 粒子
     * @param duration 持續時間
     * @param animator 動畫函數（接收進度 0.0-1.0）
     * @return 動畫 UUID
     */
    public UUID animateCustom(ControlableParticle particle, int duration, Consumer<Float> animator) {
        return animateCustom(particle, duration, InterpolationType.LINEAR, animator, null);
    }

    /**
     * 為粒子應用自定義動畫（帶插值和回調）
     * @param particle 粒子
     * @param duration 持續時間
     * @param interpolation 插值類型
     * @param animator 動畫函數（接收插值後的進度）
     * @param onComplete 完成回調
     * @return 動畫 UUID
     */
    public UUID animateCustom(ControlableParticle particle, int duration, InterpolationType interpolation,
                             Consumer<Float> animator, Runnable onComplete) {
        UUID animationId = UUID.randomUUID();

        UUID schedulerId = CooScheduler.getInstance().runTaskTimer(new Runnable() {
            int currentTick = 0;

            @Override
            public void run() {
                if (particle.isRemoved()) {
                    stopAnimation(animationId);
                    return;
                }

                float progress = (float) currentTick / duration;
                float interpolatedProgress = interpolation.interpolate(progress);

                animator.accept(interpolatedProgress);

                currentTick++;

                if (currentTick >= duration) {
                    stopAnimation(animationId);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }, 0, 1);

        activeAnimations.put(animationId, new AnimationTask(schedulerId, null, particle));
        return animationId;
    }

    /**
     * 停止動畫
     * @param animationId 動畫 UUID
     */
    public void stopAnimation(UUID animationId) {
        AnimationTask task = activeAnimations.remove(animationId);
        if (task != null) {
            CooScheduler.getInstance().cancelTask(task.schedulerId);
        }
    }

    /**
     * 停止粒子的所有動畫
     * @param particle 粒子
     */
    public void stopAllAnimations(ControlableParticle particle) {
        activeAnimations.entrySet().removeIf(entry -> {
            if (entry.getValue().particle.equals(particle)) {
                CooScheduler.getInstance().cancelTask(entry.getValue().schedulerId);
                return true;
            }
            return false;
        });
    }

    /**
     * 清除所有動畫
     */
    public void clearAll() {
        activeAnimations.forEach((id, task) -> 
            CooScheduler.getInstance().cancelTask(task.schedulerId)
        );
        activeAnimations.clear();
    }

    /**
     * 獲取活動動畫數量
     * @return 動畫數量
     */
    public int getActiveAnimationCount() {
        return activeAnimations.size();
    }

    /**
     * 動畫任務內部類
     */
    private static class AnimationTask {
        final UUID schedulerId;
        final PathMotion pathMotion;
        final ControlableParticle particle;

        AnimationTask(UUID schedulerId, PathMotion pathMotion, ControlableParticle particle) {
            this.schedulerId = schedulerId;
            this.pathMotion = pathMotion;
            this.particle = particle;
        }
    }

    /**
     * 創建位置動畫構建器
     * @param particle 粒子
     * @return 構建器
     */
    public static PositionAnimationBuilder position(ControlableParticle particle) {
        return new PositionAnimationBuilder(particle);
    }

    /**
     * 位置動畫構建器
     */
    public static class PositionAnimationBuilder {
        private final ControlableParticle particle;
        private Vec3 from;
        private Vec3 to;
        private int duration = 20;
        private InterpolationType interpolation = InterpolationType.LINEAR;
        private Runnable onComplete;

        private PositionAnimationBuilder(ControlableParticle particle) {
            this.particle = particle;
            this.from = particle.getPosition();
        }

        public PositionAnimationBuilder from(Vec3 from) {
            this.from = from;
            return this;
        }

        public PositionAnimationBuilder to(Vec3 to) {
            this.to = to;
            return this;
        }

        public PositionAnimationBuilder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public PositionAnimationBuilder interpolation(InterpolationType interpolation) {
            this.interpolation = interpolation;
            return this;
        }

        public PositionAnimationBuilder onComplete(Runnable onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        public UUID start() {
            if (to == null) {
                throw new IllegalStateException("Target position must be set");
            }

            PathMotion path = new PathMotion.Builder()
                .addPoint(from)
                .addPoint(to)
                .duration(duration)
                .interpolation(interpolation)
                .build();

            return getInstance().animate(particle, path, onComplete);
        }
    }
}
