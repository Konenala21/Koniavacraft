package com.github.nalamodikk.common.block.blockentity.altar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AspectPedestalBlock extends BaseEntityBlock {

    public static final MapCodec<AspectPedestalBlock> CODEC = simpleCodec(AspectPedestalBlock::new);

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public AspectPedestalBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AspectPedestalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || hand != InteractionHand.MAIN_HAND)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof AspectPedestalBlockEntity pedestal))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (pedestal.getHeldItem().isEmpty()) {
            if (level.isClientSide()) {
                // 樂觀更新：客戶端先更新視覺，不等封包
                pedestal.setHeldItemClientSide(stack.copyWithCount(1));
                return ItemInteractionResult.SUCCESS;
            }
            ItemStack remaining = pedestal.insertItem(stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, remaining);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AspectPedestalBlockEntity pedestal))
            return InteractionResult.PASS;

        if (level.isClientSide()) {
            // 樂觀更新：客戶端先清除視覺
            if (!pedestal.getHeldItem().isEmpty()) {
                pedestal.setHeldItemClientSide(ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        ItemStack extracted = pedestal.extractItem();
        if (!extracted.isEmpty()) {
            player.getInventory().add(extracted);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AspectPedestalBlockEntity pedestal) {
                ItemStack held = pedestal.getHeldItem();
                if (!held.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        // 鄰居改變時通知 controller 重新驗證
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AspectPedestalBlockEntity pedestal) {
            BlockPos ctrlPos = pedestal.getControllerPos();
            if (ctrlPos != null && level.getBlockEntity(ctrlPos) instanceof AspectAltarBlockEntity altar) {
                altar.checkStructure();
            }
        }
    }
}
