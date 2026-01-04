package com.github.nalamodikk.common.utils.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class DirectionIOController {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static BlockState getFacingState(BlockState state, BlockPlaceContext context) {
        return state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public static Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }
}