package com.github.nalamodikk.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.event.api.KoniavaEvent;
import com.github.nalamodikk.reflect.KoniavaScanner;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Koniava 專屬事件總線
 * 負責事件的分發與監聽管理
 */
public class KoniavaEventBus {
    private static final Map<Class<? extends KoniavaEvent>, TreeMap<EventPriority, List<Consumer<KoniavaEvent>>>> HANDLERS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    /**
     * 從掃描結果中初始化所有監聽器
     */
    public static void init() {
        if (initialized)
            return;
        initialized = true;

        KoniavaScanner.getWithAnnotation(EventListener.class).forEach(info -> {
            try {
                registerClass(info.toClass());
            } catch (Exception e) {
                KoniavacraftMod.LOGGER.error("無法註冊事件監聽器: " + info.className(), e);
            }
        });
    }

    private static void registerClass(Class<?> clazz) throws Exception {
        // 尋找實例
        Object instance = null;
        try {
            // 嘗試獲取 INSTANCE 欄位 (支援 Kotlin object 或 單例模式)
            instance = clazz.getDeclaredField("INSTANCE").get(null);
        } catch (Exception e) {
            // 嘗試實例化
            instance = clazz.getDeclaredConstructor().newInstance();
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (KoniavaEvent.class.isAssignableFrom(paramType)) {
                    EventHandler anno = method.getAnnotation(EventHandler.class);
                    registerHandler((Class<? extends KoniavaEvent>) paramType, anno.priority(), instance, method);
                }
            }
        }
    }

    private static void registerHandler(Class<? extends KoniavaEvent> eventClass, EventPriority priority,
            Object instance, Method method) {
        method.setAccessible(true);
        HANDLERS.computeIfAbsent(eventClass, k -> new TreeMap<>())
                .computeIfAbsent(priority, k -> new ArrayList<>())
                .add(event -> {
                    try {
                        method.invoke(instance, event);
                    } catch (Exception e) {
                        KoniavacraftMod.LOGGER.error("執行事件處理程序時出錯: " + method.getName(), e);
                    }
                });
    }

    /**
     * 發布一個事件
     */
    public static <T extends KoniavaEvent> T post(T event) {
        TreeMap<EventPriority, List<Consumer<KoniavaEvent>>> priorityMap = HANDLERS.get(event.getClass());
        if (priorityMap != null) {
            // 按優先級執行 (TreeMap 預設是升序，所以如果是 HIGHEST 到 LOWEST，需要注意排序)
            // 這裡假設我們想要 HIGHEST 先執行，我們可以反轉或手動遍歷
            for (Map.Entry<EventPriority, List<Consumer<KoniavaEvent>>> entry : priorityMap.entrySet()) {
                for (Consumer<KoniavaEvent> handler : entry.getValue()) {
                    handler.accept(event);
                }
            }
        }
        return event;
    }
}
