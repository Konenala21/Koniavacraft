package com.github.nalamodikk.annotations.emitter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記一個欄位為自動發射器
 * 系統會自動掃描並在該位置產生效果
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EmitterField {
    String type();

    int interval() default 1; // 發射間隔

    double chance() default 1.0; // 發射機率
}
