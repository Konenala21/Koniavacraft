package com.github.nalamodikk.common.block.blockentity.altar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AspectAltarBlock extends BaseEntityBlock {

    public static final MapCodec<AspectAltarBlock> CODEC = simpleCodec(AspectAltarBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public AspectAltarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AspectAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return AspectAltarBlockEntity.getTicker(level, type);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 成形後由 BER 渲染動畫，未成形用靜態方塊模型
        return state.getValue(FORMED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof AspectAltarBlockEntity altar) {
                int tier = altar.getUpgradeTier();
                if (tier > 0) {
                    AltarTierSavedData.get(serverLevel).saveTier(pos, tier);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof AspectAltarBlockEntity altar)) return InteractionResult.PASS;

        // 空手右鍵只顯示狀態，不啟動儀式（需用儀式魔杖）
        Component status;
        if (!altar.isFormed()) {
            status = net.minecraft.network.chat.Component.translatable("block.koniava.aspect_altar.not_formed");
        } else if (altar.isActive()) {
            int pct = (int)(altar.getProgress() * 100);
            status = net.minecraft.network.chat.Component.translatable(
                    "block.koniava.aspect_altar.status_active", pct, altar.getManaStored());
        } else {
            status = net.minecraft.network.chat.Component.translatable(
                    "block.koniava.aspect_altar.status_idle",
                    altar.getManaStored(), altar.getMaxMana());
        }
        player.displayClientMessage(status, true);
        return InteractionResult.SUCCESS;
    }

}
