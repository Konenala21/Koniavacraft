package com.github.nalamodikk.particle.event.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記類為事件監聽器
 *
 * 類必須有無參構造函數或靜態 INSTANCE 字段
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventListener {
    String modId();
}
