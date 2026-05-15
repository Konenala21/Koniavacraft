package com.github.nalamodikk.common.block.blockentity.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class ResonanceRingBlock extends Block {

    public ResonanceRingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 個別方塊不渲染，整個環的視覺由 AspectAltarRenderer 統一渲染
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            notifyNearbyAltar(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void notifyNearbyAltar(Level level, BlockPos pos) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    if (level.getBlockEntity(pos.offset(dx, dy, dz)) instanceof AspectAltarBlockEntity altar) {
                        altar.refreshUpgradeTier();
                        return;
                    }
                }
            }
        }
    }
}
