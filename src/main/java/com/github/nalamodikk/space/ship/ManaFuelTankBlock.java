package com.github.nalamodikk.space.ship;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 飛船魔力燃料槽方塊。組裝必要骨架之一（≥1 才能啟動）。帶 BE 儲存魔力燃料，接船上魔力網路補給。
 */
public class ManaFuelTankBlock extends BaseEntityBlock {
    public static final MapCodec<ManaFuelTankBlock> CODEC = simpleCodec(ManaFuelTankBlock::new);

    public ManaFuelTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaFuelTankBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
