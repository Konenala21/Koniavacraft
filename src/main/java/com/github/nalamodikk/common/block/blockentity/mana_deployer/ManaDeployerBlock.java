package com.github.nalamodikk.common.block.blockentity.mana_deployer;

import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ManaDeployerBlock extends BaseMachineBlock {

    public static final MapCodec<ManaDeployerBlock> CODEC = simpleCodec(ManaDeployerBlock::new);

    public ManaDeployerBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<ManaDeployerBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // BER handles rendering
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaDeployerBlockEntity(pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ManaDeployerBlockEntity be) {
            be.onNeighborChanged(level, pos);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MANA_DEPLOYER_BE.get(),
                ManaDeployerBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ManaDeployerBlockEntity be))
            return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            be.toggleMode();
            player.displayClientMessage(
                Component.translatable("block.koniava.mana_deployer.mode."
                    + be.getMode().name().toLowerCase()),
                true);
            return InteractionResult.SUCCESS;
        }

        player.openMenu(be, be.getBlockPos());
        return InteractionResult.SUCCESS;
    }
}
