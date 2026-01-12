package com.github.nalamodikk.display;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.reflect.KoniavaScanner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 顯示實體管理器
 * 管理所有虛擬實體的註冊、生成與同步
 */
public class DisplayEntityManager {
    private static final Map<String, Function<UUID, ? extends DisplayEntity>> REGISTRY = new HashMap<>();
    private static final Map<UUID, DisplayEntity> ENTITIES = new ConcurrentHashMap<>();

    /**
     * 從掃描結果中自動註冊所有帶註解的實體類
     */
    public static void init() {
        KoniavaScanner.getWithAnnotation(DisplayEntityRegister.class).forEach(info -> {
            try {
                Class<?> clazz = info.toClass();
                if (DisplayEntity.class.isAssignableFrom(clazz)) {
                    DisplayEntityRegister anno = clazz.getAnnotation(DisplayEntityRegister.class);
                    register(anno.type(), uuid -> {
                        try {
                            return (DisplayEntity) clazz.getDeclaredConstructor(UUID.class).newInstance(uuid);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (Exception e) {
                KoniavacraftMod.LOGGER.error("無法註冊 DisplayEntity: " + info.className(), e);
            }
        });
    }

    public static void register(String type, Function<UUID, ? extends DisplayEntity> factory) {
        REGISTRY.put(type, factory);
    }

    public static DisplayEntity create(String type, UUID uuid) {
        if (!REGISTRY.containsKey(type))
            return null;
        DisplayEntity entity = REGISTRY.get(type).apply(uuid);
        ENTITIES.put(uuid, entity);
        return entity;
    }

    public static void tick() {
        ENTITIES.values().removeIf(entity -> {
            if (entity.isRemoved())
                return true;
            entity.tick();
            return false;
        });
    }

    public static DisplayEntity get(UUID uuid) {
        return ENTITIES.get(uuid);
    }

    public static void remove(UUID uuid) {
        DisplayEntity entity = ENTITIES.remove(uuid);
        if (entity != null) {
            entity.remove();
        }
    }

    public static java.util.Collection<DisplayEntity> getAllEntities() {
        return ENTITIES.values();
    }
}
