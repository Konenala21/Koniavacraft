package com.github.nalamodikk.animation;

import com.github.nalamodikk.display.DisplayEntity;
import java.util.function.Consumer;

/**
 * 代表一個動畫片段
 * 用於隨時間改變 DisplayEntity 的屬性
 */
public record AnimationFrame(float progress, Consumer<DisplayEntity> action) {
}
