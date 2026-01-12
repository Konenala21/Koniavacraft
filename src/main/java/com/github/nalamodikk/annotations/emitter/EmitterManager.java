package com.github.nalamodikk.annotations.emitter;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.display.DisplayEntityManager;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 發射器管理器
 * 負責處理帶有 @EmitterField 的對象
 */
public class EmitterManager {
    private static final Random RANDOM = new Random();
    private static final List<EmitterTask> TASKS = new ArrayList<>();

    public static void register(Object instance) {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(EmitterField.class)) {
                field.setAccessible(true);
                EmitterField anno = field.getAnnotation(EmitterField.class);
                TASKS.add(new EmitterTask(instance, field, anno));
            }
        }
    }

    public static void tick() {
        TASKS.forEach(EmitterTask::update);
    }

    private static class EmitterTask {
        private final Object instance;
        private final Field field;
        private final EmitterField config;
        private int timer = 0;

        public EmitterTask(Object instance, Field field, EmitterField config) {
            this.instance = instance;
            this.field = field;
            this.config = config;
        }

        public void update() {
            timer++;
            if (timer >= config.interval()) {
                timer = 0;
                if (RANDOM.nextDouble() <= config.chance()) {
                    try {
                        Object val = field.get(instance);
                        if (val instanceof Vec3 pos) {
                            // 這裡示範產生一個隨機 UUID 的實體
                            DisplayEntityManager.create(config.type(), UUID.randomUUID()).setPos(pos);
                        }
                    } catch (Exception e) {
                        KoniavacraftMod.LOGGER.error("發射器執行失敗", e);
                    }
                }
            }
        }
    }
}
