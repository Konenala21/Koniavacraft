package com.github.nalamodikk.particle.event.api;

import java.util.function.Consumer;

/**
 * 事件執行器
 *
 * @param modId 該事件監聽器的所屬 mod
 * @param executor 事件執行內容
 */
public record EventExecutor(String modId, Consumer<CooEvent> executor) {
}
