package com.github.nalamodikk.common.utils.data;

import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TechDataComponents {

    /**
     * 儲存該方塊每個方向的輸出設定，並寫入 ItemStack 的 DataComponent 中
     */
    public static void saveConfigDirections(ItemStack stack, BlockPos pos, IConfigurableBlock configBlock) {
        Map<Direction, Boolean> map = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            map.put(dir, configBlock.isOutput(dir));
        }
        stack.set(ModDataComponents.SAVED_DIRECTIONS, map);
    }

    /**
     * 將已儲存的方向設定加入 Tooltip 顯示
     */
    public static void appendSavedDirectionTooltip(ItemStack stack, List<Component> tooltip) {
        Map<Direction, Boolean> map = stack.get(ModDataComponents.SAVED_DIRECTIONS);
        if (map != null) {
            for (Direction dir : Direction.values()) {
                if (map.containsKey(dir)) {
                    boolean isOutput = map.get(dir);
                    tooltip.add(Component.translatable(
                            "tooltip.koniava.saved_direction_config",
                            dir.getName(),
                            Component.translatable(isOutput
                                    ? "mode.koniava.output"
                                    : "mode.koniava.input")
                    ));
                }
            }
        }

        BlockPos pos = stack.get(ModDataComponents.SAVED_BLOCK_POS); // ⚠️ 這需要你有對應的 component
        if (pos != null) {
            tooltip.add(Component.translatable(
                    "tooltip.koniava.saved_block_position",
                    pos.getX(), pos.getY(), pos.getZ()
            ));
        }
    }

}
