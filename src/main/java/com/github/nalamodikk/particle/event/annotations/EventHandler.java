package com.github.nalamodikk.particle.event.annotations;

import com.github.nalamodikk.particle.event.api.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記方法為事件處理器
 *
 * 示例:
 * <pre>{@code
 * @EventListener(modId = "koniava")
 * public class MyEventListener {
 *     @EventHandler(priority = EventPriority.HIGH)
 *     public void onParticleSpawn(ParticleSpawnEvent event) {
 *         // 處理事件
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    EventPriority priority() default EventPriority.NORMAL;
}
