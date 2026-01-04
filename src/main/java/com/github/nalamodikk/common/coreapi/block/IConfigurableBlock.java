package com.github.nalamodikk.common.coreapi.block;

import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.Direction;

import java.util.EnumMap;

public interface IConfigurableBlock {
    /**
     * 快捷切換方向配置，依照順序 DISABLED → OUTPUT → INPUT → BOTH → DISABLED
     */
    default void cycleIOConfig(Direction direction) {
        IOHandlerUtils.IOType current = getIOConfig(direction);
        setIOConfig(direction, IOHandlerUtils.nextIOType(current));
    }

        /**
         * 設定指定方向的 IO 類型
         */
        void setIOConfig(Direction direction, IOHandlerUtils.IOType type);

        /**
         * 取得指定方向的 IO 類型
         */
        IOHandlerUtils.IOType getIOConfig(Direction direction);

        /**
         * 取得整張方向配置表
         */
        EnumMap<Direction, IOHandlerUtils.IOType> getIOMap();

        /**
         * 設定整張方向配置表
         */
        void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map);

        /**
         * 是否為輸出（語意糖，對應舊介面邏輯）
         */
        default boolean isOutput(Direction direction) {
            IOHandlerUtils.IOType type = getIOConfig(direction);
            return type == IOHandlerUtils.IOType.OUTPUT || type == IOHandlerUtils.IOType.BOTH;
        }

        /**
         * 是否為輸入（語意糖）
         */
        default boolean isInput(Direction direction) {
            IOHandlerUtils.IOType type = getIOConfig(direction);
            return type == IOHandlerUtils.IOType.INPUT || type == IOHandlerUtils.IOType.BOTH;
        }
}


