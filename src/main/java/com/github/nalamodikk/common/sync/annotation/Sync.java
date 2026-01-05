package com.github.nalamodikk.common.sync.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記此欄位或方法需要自動同步到 GUI。
 * 支援類型：int, float, boolean, long, Enum, String, CompoundTag。
 * 對於方法：必須是無參數的 getter 方法。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Sync {
    /**
     * 選擇性：指定在同步管理器中的標籤名稱（目前僅作註解使用）。
     */
    String value() default "";

    /**
     * 是否為唯讀同步欄位（客戶端不允許回寫）。
     */
    boolean readOnly() default false;

    /**
     * String 同步用的最大長度（僅 String 類型使用）。
     */
    int maxLength() default 64;
}
