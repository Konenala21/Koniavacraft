package com.github.nalamodikk.particle.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.event.annotations.EventHandler;
import com.github.nalamodikk.particle.event.annotations.EventListener;
import com.github.nalamodikk.particle.event.api.CooEvent;
import com.github.nalamodikk.particle.event.api.EventExecutor;
import com.github.nalamodikk.particle.event.api.EventInterruptible;
import com.github.nalamodikk.particle.event.api.EventPriority;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件總線
 * 管理事件監聽器註冊和事件分發
 */
public class CooEventBus {
    private static final CooEventBus INSTANCE = new CooEventBus();

    /**
     * 存儲所有事件處理器
     * Key: 事件類型
     * Value: TreeMap<優先級, 處理器列表>
     */
    private final Map<Class<? extends CooEvent>, TreeMap<EventPriority, List<EventExecutor>>> handlerLists
            = new ConcurrentHashMap<>();

    private boolean initialized = false;

    private CooEventBus() {}

    public static CooEventBus getInstance() {
        return INSTANCE;
    }

    /**
     * 註冊事件監聽器
     * 會掃描類中所有標記了 @EventHandler 的方法
     *
     * @param listener 監聽器實例
     */
    public void register(Object listener) {
        if (!initialized) {
            initialize();
        }

        Class<?> listenerClass = listener.getClass();
        EventListener annotation = listenerClass.getAnnotation(EventListener.class);
        String modId = annotation != null ? annotation.modId() : KoniavacraftMod.MOD_ID;

        findAndRegisterHandlers(listener, listenerClass, modId);
    }

    /**
     * 註冊事件監聽器類（會嘗試獲取 INSTANCE 或創建實例）
     *
     * @param listenerClass 監聽器類
     */
    public void registerClass(Class<?> listenerClass) {
        if (!initialized) {
            initialize();
        }

        try {
            // 嘗試獲取 INSTANCE 字段（用於單例模式）
            Object instance = null;
            try {
                Field instanceField = listenerClass.getDeclaredField("INSTANCE");
                if (Modifier.isStatic(instanceField.getModifiers())) {
                    instanceField.setAccessible(true);
                    instance = instanceField.get(null);
                }
            } catch (NoSuchFieldException ignored) {
                // 沒有 INSTANCE 字段，嘗試創建實例
            }

            if (instance == null) {
                // 創建新實例
                instance = listenerClass.getDeclaredConstructor().newInstance();
            }

            register(instance);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("Failed to register event listener class: " + listenerClass.getName(), e);
        }
    }

    /**
     * 取消註冊事件監聽器
     *
     * @param listener 監聽器實例
     */
    public void unregister(Object listener) {
        handlerLists.values().forEach(priorityMap ->
                priorityMap.values().forEach(executors ->
                        executors.removeIf(executor -> {
                            // 檢查 executor 是否屬於這個 listener
                            // 這需要在 EventExecutor 中存儲 listener 引用
                            return false; // 簡化實現，暫不支持
                        })
                )
        );
    }

    /**
     * 調用事件
     * 會按照優先級順序執行所有註冊的處理器
     *
     * @param event 要調用的事件
     * @return 處理後的事件
     */
    public <T extends CooEvent> T call(T event) {
        if (!initialized) {
            return event;
        }

        Class<?> currentEvent = event.getClass();

        // 處理事件及其所有父類（支持事件繼承）
        while (CooEvent.class.isAssignableFrom(currentEvent)) {
            TreeMap<EventPriority, List<EventExecutor>> handleList = handlerLists.get(currentEvent);

            if (handleList != null) {
                // 按優先級順序執行（HIGHEST -> LOWEST）
                for (Map.Entry<EventPriority, List<EventExecutor>> entry : handleList.entrySet()) {
                    for (EventExecutor executor : entry.getValue()) {
                        try {
                            executor.executor().accept(event);
                        } catch (Exception e) {
                            KoniavacraftMod.LOGGER.error(
                                    "Error handling event: " + event.getClass().getName() +
                                    " | Listener mod: " + executor.modId(),
                                    e
                            );
                        }

                        // 檢查事件是否被中斷
                        if (event instanceof EventInterruptible interruptible && interruptible.isInterrupted()) {
                            return event;
                        }
                    }
                }
            }

            currentEvent = currentEvent.getSuperclass();
        }

        return event;
    }

    /**
     * 初始化事件總線
     */
    private void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        KoniavacraftMod.LOGGER.info("CooEventBus initialized");
    }

    /**
     * 查找並註冊監聽器中的所有處理器方法
     */
    private void findAndRegisterHandlers(Object listener, Class<?> listenerClass, String modId) {
        for (Method method : listenerClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) {
                continue;
            }

            // 檢查方法簽名：必須有且僅有一個參數，且參數類型繼承自 CooEvent
            if (method.getParameterCount() != 1) {
                KoniavacraftMod.LOGGER.warn(
                        "Event handler method must have exactly one parameter: " +
                        listenerClass.getName() + "." + method.getName()
                );
                continue;
            }

            Class<?> paramType = method.getParameterTypes()[0];
            if (!CooEvent.class.isAssignableFrom(paramType)) {
                KoniavacraftMod.LOGGER.warn(
                        "Event handler parameter must extend CooEvent: " +
                        listenerClass.getName() + "." + method.getName()
                );
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends CooEvent> eventType = (Class<? extends CooEvent>) paramType;
            EventHandler handlerAnnotation = method.getAnnotation(EventHandler.class);
            EventPriority priority = handlerAnnotation.priority();

            method.setAccessible(true);

            // 創建事件執行器
            EventExecutor executor = new EventExecutor(modId, event -> {
                try {
                    method.invoke(listener, event);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke event handler", e);
                }
            });

            // 添加到處理器列表
            handlerLists
                    .computeIfAbsent(eventType, k -> new TreeMap<>(Comparator.reverseOrder())) // HIGHEST first
                    .computeIfAbsent(priority, k -> new ArrayList<>())
                    .add(executor);

            KoniavacraftMod.LOGGER.debug(
                    "Registered event handler: {} -> {}.{}() with priority {}",
                    eventType.getSimpleName(),
                    listenerClass.getSimpleName(),
                    method.getName(),
                    priority
            );
        }
    }

    /**
     * 清空所有註冊的處理器（用於測試）
     */
    public void clear() {
        handlerLists.clear();
    }
}
