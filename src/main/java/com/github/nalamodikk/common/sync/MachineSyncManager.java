package com.github.nalamodikk.common.sync;

import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.compat.energy.ModNeoNalaEnergyStorage;
import com.github.nalamodikk.common.sync.annotation.Sync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.inventory.ContainerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 升級版的同步管理器。
 * 支援動態註冊不同類型的數據（int, float, boolean, long），並自動管理索引。
 * 
 * 用法範例：
 * MachineSyncManager sync = new MachineSyncManager();
 * sync.autoRegister(this); // 自動掃描標有 @Sync 的欄位與方法
 */
public class MachineSyncManager implements ContainerData {
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineSyncManager.class);
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private final List<ISyncableData> syncables = new ArrayList<>();
    private int totalIntCount = 0;
    private boolean isDirty = false;
    private int[] lastSnapshot;

    public boolean isDirty() {
        return isDirty;
    }
    
    public void markDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    /**
     * 重新掃描同步資料，若有變化則標記 dirty。
     * 會順便更新快照，避免重複觸發。
     */
    public boolean refreshDirty() {
        if (totalIntCount <= 0) {
            return false;
        }
        if (lastSnapshot == null || lastSnapshot.length != totalIntCount) {
            lastSnapshot = new int[totalIntCount];
            captureSnapshot();
            return false;
        }

        boolean changed = false;
        int index = 0;
        for (ISyncableData data : syncables) {
            for (int i = 0; i < data.getSize(); i++) {
                int value = data.get(i);
                if (lastSnapshot[index] != value) {
                    lastSnapshot[index] = value;
                    changed = true;
                }
                index++;
            }
        }
        if (changed) {
            isDirty = true;
        }
        return changed;
    }

    private void captureSnapshot() {
        int index = 0;
        for (ISyncableData data : syncables) {
            for (int i = 0; i < data.getSize(); i++) {
                lastSnapshot[index++] = data.get(i);
            }
        }
    }

    // --- 自動註冊邏輯 ---

    /**
     * 自動掃描並註冊物件中帶有 @Sync 註解的欄位與方法。
     */
    public void autoRegister(Object provider) {
        Class<?> clazz = provider.getClass();
        
        // 1. 處理欄位
        List<Field> fields = FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> syncFields = new ArrayList<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    if (f.isAnnotationPresent(Sync.class)) {
                        f.setAccessible(true);
                        syncFields.add(f);
                    }
                }
                current = current.getSuperclass();
            }
            return syncFields;
        });

        for (Field f : fields) {
            registerField(provider, f);
        }

        // 2. 處理方法
        List<Method> methods = METHOD_CACHE.computeIfAbsent(clazz, c -> {
            List<Method> syncMethods = new ArrayList<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Method m : current.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(Sync.class) && m.getParameterCount() == 0) {
                        m.setAccessible(true);
                        syncMethods.add(m);
                    }
                }
                current = current.getSuperclass();
            }
            return syncMethods;
        });

        for (Method m : methods) {
            registerMethod(provider, m);
        }
    }

    private void registerMethod(Object provider, Method m) {
        Class<?> returnType = m.getReturnType();
        Sync sync = m.getAnnotation(Sync.class);
        boolean readOnly = sync != null && sync.readOnly();
        // 嘗試尋找對應的 Setter
        String name = m.getName();
        if (name.startsWith("get") || name.startsWith("is")) {
            String baseName = name.startsWith("get") ? name.substring(3) : name.substring(2);
            String setterName = "set" + baseName;
            try {
                if (!readOnly) {
                    Method setter = provider.getClass().getMethod(setterName, returnType);
                    setter.setAccessible(true);
                    bindMethod(provider, m, setter, returnType);
                    return;
                }
            } catch (NoSuchMethodException ignored) {
                if (!readOnly) {
                    LOGGER.warn("Missing sync setter: {}.{}({})", provider.getClass().getSimpleName(), setterName, returnType.getSimpleName());
                }
            }
        }

        // 沒找到 Setter 則作為唯讀
        bindMethod(provider, m, null, returnType);
    }

    private void bindMethod(Object provider, Method getter, Method setter, Class<?> type) {
        AtomicBoolean warned = new AtomicBoolean(false);
        if (type == int.class || type == Integer.class) {
            trackInt(() -> {
                try { return (Integer) getter.invoke(provider); } catch (Exception e) { logInvokeFailureOnce(warned, getter, e); return 0; }
            }, v -> {
                if (setter != null) try { setter.invoke(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, setter, e); }
            });
        } else if (type == boolean.class || type == Boolean.class) {
            trackBoolean(() -> {
                try { return (Boolean) getter.invoke(provider); } catch (Exception e) { logInvokeFailureOnce(warned, getter, e); return false; }
            }, v -> {
                if (setter != null) try { setter.invoke(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, setter, e); }
            });
        } else if (type == float.class || type == Float.class) {
            trackFloat(() -> {
                try { return (Float) getter.invoke(provider); } catch (Exception e) { logInvokeFailureOnce(warned, getter, e); return 0f; }
            }, v -> {
                if (setter != null) try { setter.invoke(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, setter, e); }
            });
        } else if (type == long.class || type == Long.class) {
            trackLong(() -> {
                try { return (Long) getter.invoke(provider); } catch (Exception e) { logInvokeFailureOnce(warned, getter, e); return 0L; }
            }, v -> {
                if (setter != null) try { setter.invoke(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, setter, e); }
            });
        } else if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            trackInt(() -> {
                try {
                    Object val = getter.invoke(provider);
                    return val instanceof Enum<?> e ? e.ordinal() : 0;
                } catch (Exception e) { logInvokeFailureOnce(warned, getter, e); return 0; }
            }, v -> {
                if (setter != null) {
                    try {
                        if (v >= 0 && v < constants.length) {
                            setter.invoke(provider, constants[v]);
                        }
                    } catch (Exception e) { logInvokeFailureOnce(warned, setter, e); }
                }
            });
        } else if (type == String.class) {
            int maxLength = 64;
            Sync sync = getter.getAnnotation(Sync.class);
            if (sync != null) {
                maxLength = Math.max(0, sync.maxLength());
            }
            trackString(() -> {
                try { return (String) getter.invoke(provider); } catch (Exception e) { logInvokeFailureOnce(warned, getter, e); return ""; }
            }, v -> {
                if (setter != null) try { setter.invoke(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, setter, e); }
            }, maxLength);
        } else if (type == CompoundTag.class) {
            int maxLength = 256;
            String[] allowedKeys = new String[0];
            Sync sync = getter.getAnnotation(Sync.class);
            if (sync != null) {
                maxLength = Math.max(0, sync.maxLength());
                allowedKeys = sync.allowedKeys();
            }
            final int finalMaxLength = maxLength;
            final String[] finalAllowedKeys = allowedKeys;
            trackString(() -> {
                try {
                    CompoundTag tag = (CompoundTag) getter.invoke(provider);
                    CompoundTag filtered = filterCompoundTag(tag, finalAllowedKeys);
                    return filtered != null ? filtered.toString() : "";
                } catch (Exception e) { logInvokeFailureOnce(warned, getter, e); return ""; }
            }, v -> {
                if (setter == null) return;
                try {
                    CompoundTag parsed = v.isEmpty() ? new CompoundTag() : TagParser.parseTag(v);
                    CompoundTag filtered = filterCompoundTag(parsed, finalAllowedKeys);
                    setter.invoke(provider, filtered);
                } catch (Exception e) { logInvokeFailureOnce(warned, setter, e); }
            }, finalMaxLength);
        }
    }

    private void registerField(Object provider, Field f) {
        Class<?> type = f.getType();
        Sync sync = f.getAnnotation(Sync.class);
        boolean readOnly = sync != null && sync.readOnly();
        AtomicBoolean warned = new AtomicBoolean(false);
        
        try {
            Object value = f.get(provider);
            if (value == null && !type.isPrimitive()) return;

            if (type == int.class || type == Integer.class) {
                trackInt(() -> {
                    try { return f.getInt(provider); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); return 0; }
                }, v -> {
                    if (!readOnly) try { f.set(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); }
                });
            } else if (type == boolean.class || type == Boolean.class) {
                trackBoolean(() -> {
                    try { return f.getBoolean(provider); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); return false; }
                }, v -> {
                    if (!readOnly) try { f.set(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); }
                });
            } else if (type == float.class || type == Float.class) {
                trackFloat(() -> {
                    try { return f.getFloat(provider); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); return 0f; }
                }, v -> {
                    if (!readOnly) try { f.set(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); }
                });
            } else if (type == long.class || type == Long.class) {
                trackLong(() -> {
                    try { return f.getLong(provider); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); return 0L; }
                }, v -> {
                    if (!readOnly) try { f.set(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); }
                });
            } else if (type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                trackInt(() -> {
                    try {
                        Object val = f.get(provider);
                        return val instanceof Enum<?> e ? e.ordinal() : 0;
                    } catch (Exception e) { logInvokeFailureOnce(warned, f, e); return 0; }
                }, v -> {
                    if (!readOnly) {
                        try {
                            if (v >= 0 && v < constants.length) {
                                f.set(provider, constants[v]);
                            }
                        } catch (Exception e) { logInvokeFailureOnce(warned, f, e); }
                    }
                });
            } else if (type == ManaStorage.class) {
                ManaStorage storage = (ManaStorage) value;
                trackInt(storage::getManaStored, readOnly ? null : storage::setMana);
                trackInt(storage::getMaxManaStored, readOnly ? null : storage::setCapacity);
            } else if (type == ModNeoNalaEnergyStorage.class) {
                ModNeoNalaEnergyStorage storage = (ModNeoNalaEnergyStorage) value;
                trackInt(storage::getEnergyStored, readOnly ? null : v -> storage.setEnergyStored(BigInteger.valueOf(v)));
                trackInt(storage::getMaxEnergyStored, null);
            } else if (type == String.class) {
                int maxLength = sync != null ? Math.max(0, sync.maxLength()) : 64;
                trackString(() -> {
                    try {
                        Object val = f.get(provider);
                        return val != null ? val.toString() : "";
                    } catch (Exception e) { logInvokeFailureOnce(warned, f, e); return ""; }
                }, v -> {
                    if (!readOnly) try { f.set(provider, v); } catch (Exception e) { logInvokeFailureOnce(warned, f, e); }
                }, maxLength);
            } else if (type == CompoundTag.class) {
                int maxLength = sync != null ? Math.max(0, sync.maxLength()) : 256;
                String[] allowedKeys = sync != null ? sync.allowedKeys() : new String[0];
                final int finalMaxLength = maxLength;
                final String[] finalAllowedKeys = allowedKeys;
                trackString(() -> {
                    try {
                        CompoundTag tag = (CompoundTag) f.get(provider);
                        CompoundTag filtered = filterCompoundTag(tag, finalAllowedKeys);
                        return filtered != null ? filtered.toString() : "";
                    } catch (Exception e) { logInvokeFailureOnce(warned, f, e); return ""; }
                }, v -> {
                    if (readOnly) return;
                    try {
                        CompoundTag parsed = v.isEmpty() ? new CompoundTag() : TagParser.parseTag(v);
                        CompoundTag filtered = filterCompoundTag(parsed, finalAllowedKeys);
                        f.set(provider, filtered);
                    } catch (Exception e) { logInvokeFailureOnce(warned, f, e); }
                }, finalMaxLength);
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to register sync field: {}.{}", provider.getClass().getSimpleName(), f.getName(), e);
        }
    }

    // --- 註冊方法 ---

    /**
     * 追蹤一個整數 (佔用 1 個 slot)
     */
    public void trackInt(Supplier<Integer> getter, Consumer<Integer> setter) {
        syncables.add(new IntSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 1;
    }

    /**
     * 追蹤一個布林值 (佔用 1 個 slot，0 或 1)
     */
    public void trackBoolean(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        syncables.add(new BooleanSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 1;
    }

    /**
     * 追蹤一個浮點數 (佔用 1 個 slot，透過 Float.floatToIntBits 轉換)
     * 注意：這會失去一點點精度，但對於 GUI 顯示通常足夠。
     */
    public void trackFloat(Supplier<Float> getter, Consumer<Float> setter) {
        syncables.add(new FloatSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 1;
    }

    /**
     * 追蹤一個長整數 (佔用 2 個 slot，拆分為高低位)
     * 適用於大數值能量或極大數量的物品。
     */
    public void trackLong(Supplier<Long> getter, Consumer<Long> setter) {
        syncables.add(new LongSyncData(getter, setter, totalIntCount, this));
        totalIntCount += 2;
    }

    /**
     * 追蹤一個字串 (佔用 maxLength + 1 個 slot，第一格為長度)
     */
    public void trackString(Supplier<String> getter, Consumer<String> setter, int maxLength) {
        int length = Math.max(0, maxLength);
        syncables.add(new StringSyncData(getter, setter, totalIntCount, length, this));
        totalIntCount += length + 1;
    }
    
    /**
     * 追蹤一個 Enum (佔用 1 個 slot，存 ordinal)
     */
    public <E extends Enum<E>> void trackEnum(Supplier<E> getter, Consumer<E> setter, E[] values) {
        syncables.add(new IntSyncData(
            () -> getter.get().ordinal(), 
            val -> setter.accept(values[Math.max(0, Math.min(val, values.length - 1))]), 
            totalIntCount,
            this
        ));
        totalIntCount += 1;
    }

    // --- ContainerData 實作 ---

    @Override
    public int get(int index) {
        for (ISyncableData data : syncables) {
            if (index >= data.getStartIndex() && index < data.getStartIndex() + data.getSize()) {
                return data.get(index - data.getStartIndex());
            }
        }
        return 0;
    }

    @Override
    public void set(int index, int value) {
        for (ISyncableData data : syncables) {
            if (index >= data.getStartIndex() && index < data.getStartIndex() + data.getSize()) {
                data.set(index - data.getStartIndex(), value);
                return;
            }
        }
    }

    @Override
    public int getCount() {
        return totalIntCount;
    }

    // --- 內部介面與實作 ---

    private interface ISyncableData {
        int get(int localIndex);
        void set(int localIndex, int value);
        int getStartIndex();
        int getSize();
    }

    private static class IntSyncData implements ISyncableData {
        private final Supplier<Integer> getter;
        private final Consumer<Integer> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public IntSyncData(Supplier<Integer> getter, Consumer<Integer> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override public int get(int i) { return getter.get(); }
        @Override public void set(int i, int v) { 
            if (setter == null) return;
            if (getter.get() != v) {
                setter.accept(v); 
                manager.markDirty(true);
            }
        }
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 1; }
    }

    private static class BooleanSyncData implements ISyncableData {
        private final Supplier<Boolean> getter;
        private final Consumer<Boolean> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public BooleanSyncData(Supplier<Boolean> getter, Consumer<Boolean> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override public int get(int i) { return getter.get() ? 1 : 0; }
        @Override public void set(int i, int v) { 
            if (setter == null) return;
            boolean newVal = v != 0;
            if (getter.get() != newVal) {
                setter.accept(newVal); 
                manager.markDirty(true);
            }
        }
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 1; }
    }

    private static class FloatSyncData implements ISyncableData {
        private final Supplier<Float> getter;
        private final Consumer<Float> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public FloatSyncData(Supplier<Float> getter, Consumer<Float> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override public int get(int i) { return Float.floatToIntBits(getter.get()); }
        @Override public void set(int i, int v) { 
            if (setter == null) return;
            float newVal = Float.intBitsToFloat(v);
            if (Math.abs(getter.get() - newVal) > 0.0001f) {
                setter.accept(newVal);
                manager.markDirty(true);
            }
        }
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 1; }
    }
    
    private static class LongSyncData implements ISyncableData {
        private final Supplier<Long> getter;
        private final Consumer<Long> setter;
        private final int startIndex;
        private final MachineSyncManager manager;

        public LongSyncData(Supplier<Long> getter, Consumer<Long> setter, int startIndex, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.manager = manager;
        }

        @Override 
        public int get(int i) { 
            long val = getter.get();
            if (i == 0) return (int)(val & 0xFFFFFFFFL);
            else return (int)((val >>> 32) & 0xFFFFFFFFL);
        }
        
        @Override 
        public void set(int i, int v) { 
            if (setter == null) return;
            long current = getter.get();
            long newVal = current;
            if (i == 0) {
                long high = current & 0xFFFFFFFF00000000L;
                long low = Integer.toUnsignedLong(v);
                newVal = high | low;
            } else {
                long low = current & 0xFFFFFFFFL;
                long high = ((long)v) << 32;
                newVal = high | low;
            }
            if (current != newVal) {
                setter.accept(newVal);
                manager.markDirty(true);
            }
        }
        
        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return 2; }
    }

    private static class StringSyncData implements ISyncableData {
        private final Supplier<String> getter;
        private final Consumer<String> setter;
        private final int startIndex;
        private final int maxLength;
        private final MachineSyncManager manager;
        private final char[] buffer;
        private int length;

        public StringSyncData(Supplier<String> getter, Consumer<String> setter, int startIndex, int maxLength, MachineSyncManager manager) {
            this.getter = getter;
            this.setter = setter;
            this.startIndex = startIndex;
            this.maxLength = maxLength;
            this.manager = manager;
            this.buffer = new char[maxLength];
            this.length = 0;
        }

        @Override
        public int get(int i) {
            String value = getter.get();
            if (value == null) {
                value = "";
            }
            int len = Math.min(maxLength, value.length());
            if (i == 0) {
                return len;
            }
            int charIndex = i - 1;
            if (charIndex >= 0 && charIndex < len) {
                return value.charAt(charIndex);
            }
            return 0;
        }

        @Override
        public void set(int i, int v) {
            if (setter == null) return;
            if (i == 0) {
                length = Math.max(0, Math.min(v, maxLength));
            } else {
                int charIndex = i - 1;
                if (charIndex >= 0 && charIndex < maxLength) {
                    buffer[charIndex] = (char) v;
                }
            }
            String newValue = length > 0 ? new String(buffer, 0, length) : "";
            if (!newValue.equals(getter.get())) {
                setter.accept(newValue);
                manager.markDirty(true);
            }
        }

        @Override public int getStartIndex() { return startIndex; }
        @Override public int getSize() { return maxLength + 1; }
    }

    private static void logInvokeFailureOnce(AtomicBoolean warned, Object member, Exception e) {
        if (warned.compareAndSet(false, true)) {
            String name = member instanceof Method m ? m.getName() : member instanceof Field f ? f.getName() : member.toString();
            LOGGER.error("Sync reflection failed: {}", name, e);
        }
    }

    private static CompoundTag filterCompoundTag(CompoundTag tag, String[] allowedKeys) {
        if (tag == null) {
            return new CompoundTag();
        }
        if (allowedKeys == null || allowedKeys.length == 0) {
            return tag;
        }
        CompoundTag filtered = new CompoundTag();
        for (String key : allowedKeys) {
            if (tag.contains(key)) {
                Tag value = tag.get(key);
                if (value != null) {
                    filtered.put(key, value.copy());
                }
            }
        }
        return filtered;
    }

    /**
     * 追蹤一個 DataComponent。
     */
    public <T> void trackComponent(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.core.component.DataComponentType<T> type, Class<T> clazz) {
        if (type == com.github.nalamodikk.register.ModDataComponents.MANA_STORED || clazz == Integer.class || clazz == int.class) {
            trackInt(
                () -> {
                    T val = be.components().get(type);
                    return val instanceof Integer i ? i : 0;
                },
                v -> {
                    // 注意：BE 的組件更新通常需要透過 applyComponents 或直接 set
                    // 這裡簡化為直接 set (如果支援)
                }
            );
        }
    }
}
