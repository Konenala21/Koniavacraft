package com.github.nalamodikk.common.block.blockentity.altar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class AltarPillarBlock extends BaseEntityBlock {

    public static final MapCodec<AltarPillarBlock> CODEC = simpleCodec(AltarPillarBlock::new);

    /** true = 柱子頂段（y=-1），false = 柱子底段（y=-2） */
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public AltarPillarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TOP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(TOP);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarPillarBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BER 負責全部視覺，方塊本身不渲染
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            notifyController(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void notifyController(Level level, BlockPos pos) {
        for (int dy = 1; dy <= 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    if (level.getBlockEntity(check) instanceof AspectAltarBlockEntity altar) {
                        altar.checkStructure();
                        return;
                    }
                }
            }
        }
    }
}
