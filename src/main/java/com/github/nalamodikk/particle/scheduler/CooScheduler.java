package com.github.nalamodikk.particle.scheduler;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 粒子系統調度器
 * 管理延遲任務、定時任務和限制執行次數的任務
 */
public class CooScheduler {
    private static final CooScheduler INSTANCE = new CooScheduler();

    private final ConcurrentLinkedQueue<TickRunnable> ticks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TickRunnable> taskQueue = new ConcurrentLinkedQueue<>();

    private CooScheduler() {}

    public static CooScheduler getInstance() {
        return INSTANCE;
    }

    /**
     * 每個 tick 調用，處理所有任務
     */
    public void tick() {
        var iterator = ticks.iterator();
        while (iterator.hasNext()) {
            TickRunnable tick = iterator.next();
            tick.doTick();
            if (tick.isCanceled()) {
                iterator.remove();
            }
        }
        ticks.addAll(taskQueue);
        taskQueue.clear();
    }

    /**
     * 延遲執行任務（單次）
     * @param delay 延遲的 tick 數
     * @param runnable 要執行的任務
     * @return TickRunnable 可用於取消任務
     */
    public TickRunnable runTask(int delay, Runnable runnable) {
        TickRunnable tick = new TickRunnable(runnable);
        tick.singleDelay = delay;
        taskQueue.add(tick);
        return tick;
    }

    /**
     * 每 delay 個 tick 運行一次（無限循環）
     * @param delay 執行間隔
     * @param runnable 要執行的任務
     * @return TickRunnable 可用於取消任務
     */
    public TickRunnable runTaskTimer(int delay, Runnable runnable) {
        TickRunnable tick = new TickRunnable(runnable);
        tick.singleDelay = delay;
        tick.loop();
        taskQueue.add(tick);
        return tick;
    }

    /**
     * 每 tick 運行一次，最多運行 maxLoopTick 次
     * @param maxLoopTick 最大執行次數
     * @param runnable 要執行的任務
     * @return TickRunnable 可用於取消任務
     */
    public TickRunnable runTaskTimerMaxTick(int maxLoopTick, Runnable runnable) {
        TickRunnable tick = new TickRunnable(runnable);
        tick.maxTick = maxLoopTick;
        tick.loopTimer();
        taskQueue.add(tick);
        return tick;
    }

    /**
     * 每 preDelay 個 tick 運行一次，最多運行 maxLoopTick 次
     * @param preDelay 每次執行的延遲
     * @param maxLoopTick 最大執行次數
     * @param runnable 要執行的任務
     * @return TickRunnable 可用於取消任務
     */
    public TickRunnable runTaskTimerMaxTick(int preDelay, int maxLoopTick, Runnable runnable) {
        TickRunnable tick = new TickRunnable(runnable);
        tick.maxTick = maxLoopTick;
        tick.singleDelay = preDelay;
        tick.loopTimer();
        taskQueue.add(tick);
        return tick;
    }

    /**
     * 可執行的 Tick 任務
     */
    public static class TickRunnable {
        public final UUID uuid = UUID.randomUUID();
        private final Consumer<TickRunnable> runnable;

        /**
         * loopTimer 為 true 時啟用
         * 代表執行的最大 Tick
         */
        int maxTick = 0;

        /**
         * 單次執行的時間間隔
         * looped 和 loopTimer 都為 false 時代表一次 task 的延遲執行時間
         */
        int singleDelay = 1;

        private int currentTick = 0;
        private boolean canceled = false;
        private boolean looped = false;
        private boolean loopTimer = false;

        private Runnable finishCallable = () -> {};
        private Predicate<TickRunnable> cancelPredicate = t -> false;

        public TickRunnable(Runnable task) {
            this.runnable = (tickRunnable) -> task.run();
        }

        public TickRunnable(Consumer<TickRunnable> runnable) {
            this.runnable = runnable;
        }

        public TickRunnable setFinishCallback(Runnable callable) {
            this.finishCallable = callable;
            return this;
        }

        public TickRunnable setCancelPredicate(Predicate<TickRunnable> predicate) {
            this.cancelPredicate = predicate;
            return this;
        }

        /**
         * 每 singleDelay tick 執行一次（無限循環）
         */
        TickRunnable loop() {
            looped = true;
            currentTick = maxTick;
            return this;
        }

        /**
         * 每 singleDelay tick 執行一次
         * 執行到 maxTick 結束
         */
        TickRunnable loopTimer() {
            loopTimer = true;
            return this;
        }

        public void cancel() {
            canceled = true;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public int getCurrentTick() {
            return currentTick;
        }

        void doTick() {
            if (canceled) {
                return;
            }

            if (loopTimer) {
                boolean canInvoke = currentTick++ % singleDelay == 0;
                if (canInvoke) {
                    runnable.accept(this);
                }
                if (cancelPredicate.test(this)) {
                    canceled = true;
                    finishCallable.run();
                    return;
                }
                if (currentTick >= maxTick) {
                    canceled = true;
                    finishCallable.run();
                    return;
                }
                return;
            }

            if (looped) {
                if (currentTick++ >= singleDelay) {
                    runnable.accept(this);
                    if (cancelPredicate.test(this)) {
                        canceled = true;
                        finishCallable.run();
                    }
                    currentTick = 0;
                }
                return;
            }

            if (currentTick++ >= singleDelay) {
                runnable.accept(this);
                finishCallable.run();
                canceled = true;
            }
        }
    }

    // ========== Helper 兼容方法 ==========

    /**
     * 延遲執行任務（兼容方法，返回 UUID）
     * @param task 任務
     * @param delay 延遲（tick）
     * @return 任務 UUID
     */
    public UUID runTaskLater(Runnable task, int delay) {
        TickRunnable tickTask = runTask(delay, task);
        return tickTask.uuid;
    }

    /**
     * 定時執行任務（兼容方法，返回 UUID）
     * @param task 任務
     * @param delay 初始延遲（目前未使用，為了兼容性保留）
     * @param period 執行間隔
     * @return 任務 UUID
     */
    public UUID runTaskTimer(Runnable task, int delay, int period) {
        TickRunnable tickTask = runTaskTimer(period, task);
        return tickTask.uuid;
    }

    /**
     * 定時執行任務，最多執行 maxTicks 次（兼容方法，返回 UUID）
     * @param task 任務
     * @param delay 初始延遲（目前未使用，為了兼容性保留）
     * @param period 執行間隔
     * @param maxTicks 最大執行次數
     * @return 任務 UUID
     */
    public UUID runTaskTimer(Runnable task, int delay, int period, int maxTicks) {
        TickRunnable tickTask = runTaskTimerMaxTick(period, maxTicks, task);
        return tickTask.uuid;
    }
}
