package com.github.nalamodikk.particle.event.api;

/**
 * 硬中斷
 * 如果事件實現了這個並且讓 isInterrupted 為 true
 * 那麼不會執行在那之後的事件處理器
 */
public interface EventInterruptible {
    boolean isInterrupted();
    void setInterrupted(boolean interrupted);
}
