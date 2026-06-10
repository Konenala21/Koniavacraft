package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 曲速引擎結構的輸入方塊。帶 BE 把魔力網路/燃料物品/燃料流體轉成能量緩衝，曲速從這吃。
 * 組裝結構偵測時算一個必要部件(整座結構恰好 1 個)。
 */
public class ManaWarpInputBlock extends BaseEntityBlock {
    public static final MapCodec<ManaWarpInputBlock> CODEC = simpleCodec(ManaWarpInputBlock::new);

    public ManaWarpInputBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaWarpInputBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.MANA_WARP_INPUT_BE.get(), ManaWarpInputBlockEntity::serverTick);
    }
}
