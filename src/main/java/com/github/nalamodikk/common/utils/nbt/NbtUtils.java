/**
 * 通用 NBT 序列化/反序列化工具。
 * 支援 NeoForge 1.21.1 的 HolderLookup.Provider 介面，
 * 並擴充支援 List、Map、EnumMap 與 Optional 等常見資料結構的 NBT 操作。
 */
package com.github.nalamodikk.common.utils.nbt;

import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NbtUtils {
    /**
     * 讀取單一 INBTSerializable 物件。
     */
    public static <T extends INBTSerializable<CompoundTag>> void read(
            CompoundTag tag,
            String key,
            @Nullable T data,
            HolderLookup.Provider provider
    ) {
        if (data != null && tag.contains(key, Tag.TAG_COMPOUND)) {
            data.deserializeNBT(provider, tag.getCompound(key));
        }
    }

    /**
     * 寫入單一 INBTSerializable 物件。
     */
    public static <T extends INBTSerializable<CompoundTag>> void write(
            CompoundTag tag,
            String key,
            @Nullable T data,
            HolderLookup.Provider provider
    ) {
        if (data != null) {
            tag.put(key, data.serializeNBT(provider));
        }
    }

    /**
     * 使用外部反序列化函式與目標物件進行資料讀取。
     */
    public static <T extends INBTSerializable<CompoundTag>> void read(
            CompoundTag tag,
            String key,
            HolderLookup.Provider provider,
            BiConsumer<T, CompoundTag> deserializer,
            T target
    ) {
        if (tag.contains(key)) {
            deserializer.accept(target, tag.getCompound(key));
        }
    }

    /**
     * 使用函式建構物件後，傳入 provider 進行進一步處理。
     */
    public static <T> void readOptional(
            CompoundTag tag,
            String key,
            HolderLookup.Provider provider,
            Function<CompoundTag, T> reader,
            BiConsumer<T, HolderLookup.Provider> withProvider
    ) {
        if (tag.contains(key)) {
            T result = reader.apply(tag.getCompound(key));
            withProvider.accept(result, provider);
        }
    }

    /**
     * Optional 結果版本的反序列化（舊式格式相容）。
     */
    public static <T extends INBTSerializable<CompoundTag>> Optional<T> readOptional(
            CompoundTag tag,
            String key,
            Function<CompoundTag, T> constructor
    ) {
        if (tag.contains(key)) {
            return Optional.of(constructor.apply(tag.getCompound(key)));
        }
        return Optional.empty();
    }

    /**
     * 將 List<T> 序列化為 ListTag，適用於 NBT 陣列存儲。
     */
    public static <T extends INBTSerializable<CompoundTag>> void writeList(
            CompoundTag tag,
            String key,
            List<T> list,
            HolderLookup.Provider provider
    ) {
        ListTag listTag = new ListTag();
        for (T item : list) {
            listTag.add(item.serializeNBT(provider));
        }
        tag.put(key, listTag);
    }

    /**
     * 將 ListTag 反序列化為 List<T>，並使用 constructor 創建新實例。
     */
    public static <T extends INBTSerializable<CompoundTag>> void readList(
            CompoundTag tag,
            String key,
            HolderLookup.Provider provider,
            Function<CompoundTag, T> constructor,
            Collection<T> output
    ) {
        if (tag.contains(key, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(key, Tag.TAG_COMPOUND);
            for (Tag element : listTag) {
                if (element instanceof CompoundTag compound) {
                    T instance = constructor.apply(compound);
                    output.add(instance);
                }
            }
        }
    }

    /**
     * 將 Map<String, T> 序列化為 CompoundTag。
     */
    public static <T extends INBTSerializable<CompoundTag>> void writeMap(
            CompoundTag tag,
            String key,
            Map<String, T> map,
            HolderLookup.Provider provider
    ) {
        CompoundTag result = new CompoundTag();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue().serializeNBT(provider));
        }
        tag.put(key, result);
    }

    /**
     * 將 CompoundTag 反序列化為 Map<String, T>。
     */
    public static <T extends INBTSerializable<CompoundTag>> void readMap(
            CompoundTag tag,
            String key,
            HolderLookup.Provider provider,
            Function<CompoundTag, T> constructor,
            BiConsumer<String, T> put
    ) {
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag mapTag = tag.getCompound(key);
            for (String mapKey : mapTag.getAllKeys()) {
                CompoundTag dataTag = mapTag.getCompound(mapKey);
                T value = constructor.apply(dataTag);
                put.accept(mapKey, value);
            }
        }
    }

    /**
     * 將 EnumMap<Enum, Boolean> 寫入為 CompoundTag。
     */
    public static <E extends Enum<E>> void writeEnumBooleanMap(
            CompoundTag tag,
            String key,
            EnumMap<E, Boolean> map
    ) {
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<E, Boolean> entry : map.entrySet()) {
            mapTag.putBoolean(entry.getKey().name(), entry.getValue());
        }
        tag.put(key, mapTag);
    }

    /**
     * 從 CompoundTag 讀取 EnumMap<Enum, Boolean>。
     */
    public static <E extends Enum<E>> void readEnumBooleanMap(
            CompoundTag tag,
            String key,
            EnumMap<E, Boolean> map
    ) {
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag mapTag = tag.getCompound(key);
            for (E e : map.keySet()) {
                map.put(e, mapTag.getBoolean(e.name()));
            }
        }
    }


    /**
     * 將指定的 {@link ItemStack} 物品堆疊序列化為 NBT，並以 CompoundTag 格式回傳。
     * <p>
     * 此方法使用 NeoForge 1.21.1 所推薦的 Codec 系統，透過 {@link ItemStack#CODEC}
     * 搭配 {@link HolderLookup.Provider} 正確編碼資料組件（DataComponent）。
     * 另外會在結果中附加 "Slot" 欄位來標示物品欄位位置。
     *
     * @param stack    欲儲存的 ItemStack（不可為空）
     * @param slotIndex 此 ItemStack 所在的欄位編號（通常為 inventory index）
     * @param provider 提供 DataComponent 編碼所需的 {@link HolderLookup.Provider}
     * @return 含有完整序列化資訊的 CompoundTag（含 id, count, components, Slot 等欄位）
     * @throws IllegalStateException 若 Codec 序列化失敗或輸出並非 CompoundTag
     */
    public static CompoundTag writeItemStack(ItemStack stack, int slotIndex, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Slot", (byte) slotIndex);

        Tag encoded = ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack)
                .resultOrPartial(error -> {
                    throw new IllegalStateException("Failed to encode ItemStack: " + error);
                })
                .orElseThrow();

        if (!(encoded instanceof CompoundTag compound)) {
            throw new IllegalStateException("Encoded ItemStack is not a CompoundTag");
        }

        for (String key : compound.getAllKeys()) {
            tag.put(key, compound.get(key));
        }

        return tag;
    }

    /**
     * 從指定的 {@link CompoundTag} 讀取並還原為 {@link ItemStack}。
     * <p>
     * 該 NBT 資料應來自 {@link #writeItemStack(ItemStack, int, HolderLookup.Provider)}，
     * 或其他使用 {@link ItemStack#CODEC} 所序列化的物品堆疊資料。
     *
     * @param provider 用於還原 DataComponent 所需的 {@link HolderLookup.Provider}
     * @param tag      含有物品資料的 CompoundTag（應包含 id, count, components 等欄位）
     * @return 還原後的 {@link ItemStack}，若解析失敗將回傳 {@link ItemStack#EMPTY}
     */
    public static ItemStack readItemStack(HolderLookup.Provider provider, CompoundTag tag) {
        return ItemStack.parseOptional(provider, tag);
    }


    public static void writeEnumIOTypeMap(CompoundTag tag, String key, EnumMap<Direction, IOHandlerUtils.IOType> map) {
        CompoundTag ioTag = new CompoundTag();
        for (Map.Entry<Direction, IOHandlerUtils.IOType> entry : map.entrySet()) {
            ioTag.putString(entry.getKey().getName(), entry.getValue().name());
        }
        tag.put(key, ioTag);
    }


    public static EnumMap<Direction, IOHandlerUtils.IOType> readEnumIOTypeMap(CompoundTag tag, String key) {
        EnumMap<Direction, IOHandlerUtils.IOType> result = new EnumMap<>(Direction.class);
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag ioTag = tag.getCompound(key);
            for (Direction dir : Direction.values()) {
                String dirName = dir.getName();
                if (ioTag.contains(dirName)) {
                    try {
                        IOHandlerUtils.IOType type = IOHandlerUtils.IOType.valueOf(ioTag.getString(dirName));
                        result.put(dir, type);
                    } catch (IllegalArgumentException ignored) {
                        result.put(dir, IOHandlerUtils.IOType.DISABLED);
                    }
                } else {
                    result.put(dir, IOHandlerUtils.IOType.DISABLED);
                }
            }
        }
        return result;
    }

}
