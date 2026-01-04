package com.github.nalamodikk.common.block.blockentity.conduit.manager.core;

import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

/**
 * 導管IO配置管理器
 * 負責管理導管各個方向的輸入/輸出配置和優先級
 */
public class IOManager {

    // === 配置資料 ===
    private final EnumMap<Direction, IOHandlerUtils.IOType> ioConfig = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Integer> routePriority = new EnumMap<>(Direction.class);

    // === 回調接口 ===
    private IOConfigChangeListener changeListener;

    public interface IOConfigChangeListener {
        void onIOConfigChanged(Direction direction, IOHandlerUtils.IOType newType);
        void onPriorityChanged(Direction direction, int newPriority);
    }

    // === 建構子 ===
    public IOManager() {
        // 初始化默認配置
        for (Direction dir : Direction.values()) {
            ioConfig.put(dir, IOHandlerUtils.IOType.BOTH);
            routePriority.put(dir, 0);
        }
    }

    // === 設定回調 ===
    public void setChangeListener(IOConfigChangeListener listener) {
        this.changeListener = listener;
    }

    // === IO配置管理 ===

    /**
     * 設定指定方向的IO類型
     */
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        IOHandlerUtils.IOType oldType = ioConfig.get(direction);
        if (oldType != type) {
            ioConfig.put(direction, type);

            // 通知變更
            if (changeListener != null) {
                changeListener.onIOConfigChanged(direction, type);
            }
        }
    }

    /**
     * 獲取指定方向的IO類型
     */
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return ioConfig.getOrDefault(direction, IOHandlerUtils.IOType.BOTH);
    }

    /**
     * 獲取所有IO配置的副本
     */
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return new EnumMap<>(ioConfig);
    }

    /**
     * 批量設定IO配置
     */
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> newIOMap) {
        boolean hasChanges = false;

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType newType = newIOMap.getOrDefault(dir, IOHandlerUtils.IOType.BOTH);
            if (ioConfig.get(dir) != newType) {
                ioConfig.put(dir, newType);
                hasChanges = true;

                // 個別通知每個變更
                if (changeListener != null) {
                    changeListener.onIOConfigChanged(dir, newType);
                }
            }
        }
    }

    // === 優先級管理 ===

    /**
     * 設定指定方向的優先級
     */
    public void setPriority(Direction direction, int priority) {
        // 限制在安全範圍內
        int clampedPriority = Math.max(Integer.MIN_VALUE + 1, Math.min(Integer.MAX_VALUE - 1, priority));

        if (routePriority.get(direction) != clampedPriority) {
            routePriority.put(direction, clampedPriority);

            // 通知變更
            if (changeListener != null) {
                changeListener.onPriorityChanged(direction, clampedPriority);
            }
        }
    }

    /**
     * 獲取指定方向的優先級
     */
    public int getPriority(Direction direction) {
        return routePriority.getOrDefault(direction, 0);
    }

    /**
     * 重置所有優先級為預設值
     */
    public void resetAllPriorities() {
        boolean hasChanges = false;

        for (Direction dir : Direction.values()) {
            if (routePriority.get(dir) != 0) {
                routePriority.put(dir, 0);
                hasChanges = true;

                // 個別通知每個變更
                if (changeListener != null) {
                    changeListener.onPriorityChanged(dir, 0);
                }
            }
        }
    }

    // === 查詢方法 ===

    /**
     * 檢查指定方向是否可以輸出
     */
    public boolean canOutput(Direction direction) {
        IOHandlerUtils.IOType type = getIOConfig(direction);
        return type == IOHandlerUtils.IOType.OUTPUT || type == IOHandlerUtils.IOType.BOTH;
    }

    /**
     * 檢查指定方向是否可以輸入
     */
    public boolean canInput(Direction direction) {
        IOHandlerUtils.IOType type = getIOConfig(direction);
        return type == IOHandlerUtils.IOType.INPUT || type == IOHandlerUtils.IOType.BOTH;
    }

    /**
     * 檢查指定方向是否被禁用
     */
    public boolean isDisabled(Direction direction) {
        return getIOConfig(direction) == IOHandlerUtils.IOType.DISABLED;
    }

    /**
     * 獲取所有可以輸出的方向
     */
    public Direction[] getOutputDirections() {
        return ioConfig.entrySet().stream()
                .filter(entry -> canOutput(entry.getKey()))
                .map(EnumMap.Entry::getKey)
                .toArray(Direction[]::new);
    }

    /**
     * 獲取所有可以輸入的方向
     */
    public Direction[] getInputDirections() {
        return ioConfig.entrySet().stream()
                .filter(entry -> canInput(entry.getKey()))
                .map(EnumMap.Entry::getKey)
                .toArray(Direction[]::new);
    }

    // === NBT 序列化 ===

    /**
     * 保存到NBT
     */
    public void saveToNBT(CompoundTag tag) {
        // 保存IO配置
        CompoundTag ioTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            ioTag.putString(dir.name(), ioConfig.get(dir).name());
        }
        tag.put("IOConfig", ioTag);

        // 保存優先級
        CompoundTag priorityTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            priorityTag.putInt(dir.name(), routePriority.get(dir));
        }
        tag.put("RoutePriority", priorityTag);
    }

    /**
     * 從NBT載入
     */
    public void loadFromNBT(CompoundTag tag) {
        // 載入IO配置
        if (tag.contains("IOConfig")) {
            CompoundTag ioTag = tag.getCompound("IOConfig");
            for (Direction dir : Direction.values()) {
                if (ioTag.contains(dir.name())) {
                    try {
                        IOHandlerUtils.IOType type = IOHandlerUtils.IOType.valueOf(ioTag.getString(dir.name()));
                        ioConfig.put(dir, type);
                    } catch (IllegalArgumentException e) {
                        // 如果讀取失敗，使用預設值
                        ioConfig.put(dir, IOHandlerUtils.IOType.BOTH);
                    }
                }
            }
        }

        // 載入優先級
        if (tag.contains("RoutePriority")) {
            CompoundTag priorityTag = tag.getCompound("RoutePriority");
            for (Direction dir : Direction.values()) {
                if (priorityTag.contains(dir.name())) {
                    routePriority.put(dir, priorityTag.getInt(dir.name()));
                }
            }
        }
    }

    // === 調試和信息 ===

    /**
     * 獲取配置的調試信息
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("IO Configuration:\n");

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = getIOConfig(dir);
            int priority = getPriority(dir);
            sb.append(String.format("  %s: %s (Priority: %d)\n",
                    dir.name(), type.name(), priority));
        }

        return sb.toString();
    }

    /**
     * 檢查配置是否有效
     */
    public boolean isConfigurationValid() {
        // 檢查是否至少有一個方向可以輸入或輸出
        boolean hasInput = false;
        boolean hasOutput = false;

        for (Direction dir : Direction.values()) {
            if (canInput(dir)) hasInput = true;
            if (canOutput(dir)) hasOutput = true;
        }

        return hasInput || hasOutput; // 至少要有一個方向有功能
    }
}