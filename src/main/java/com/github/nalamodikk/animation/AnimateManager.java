package com.github.nalamodikk.animation;

import com.github.nalamodikk.display.DisplayEntity;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 動畫管理器
 * 負責處理 DisplayEntity 的動態視覺變化
 */
public class AnimateManager {
    private static final Map<UUID, List<AnimationSequence>> ACTIVE_ANIMATIONS = new ConcurrentHashMap<>();

    public static void addAnimation(DisplayEntity entity, AnimationSequence sequence) {
        ACTIVE_ANIMATIONS.computeIfAbsent(entity.getUuid(), k -> new ArrayList<>()).add(sequence);
    }

    public static void tick() {
        ACTIVE_ANIMATIONS.forEach((uuid, sequences) -> {
            sequences.removeIf(seq -> {
                seq.update();
                return seq.isFinished();
            });
        });

        // 如果該實體的所有動畫都結束，釋放記憶體
        ACTIVE_ANIMATIONS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * 動畫序列封裝
     */
    public static class AnimationSequence {
        private final DisplayEntity target;
        private final int duration;
        private final Consumer<Float> animator;
        private int elapsed = 0;

        public AnimationSequence(DisplayEntity target, int duration, Consumer<Float> animator) {
            this.target = target;
            this.duration = duration;
            this.animator = animator;
        }

        public void update() {
            if (target.isRemoved()) {
                elapsed = duration;
                return;
            }
            elapsed++;
            float progress = (float) elapsed / duration;
            animator.accept(Math.min(1.0f, progress));
        }

        public boolean isFinished() {
            return elapsed >= duration;
        }
    }
}
