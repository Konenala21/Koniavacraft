package com.github.nalamodikk.common.block.blockentity.mana_deployer;

import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MANA_DEPLOYER_BE.get(),
                ManaDeployerBlockEntity::tick);
    }

    // Sneak + right-click (empty hand) → 切換模式
    // Right-click (empty hand) → 取回物品
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

        // 取回持有物品
        ItemStack held = be.getHeldItem();
        if (!held.isEmpty()) {
            player.getInventory().placeItemBackInInventory(held);
            be.setHeldItem(ItemStack.EMPTY);
            be.setChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // Right-click with item → 設定持有物品（slot 空才插入）
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof ManaDeployerBlockEntity be))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (be.getHeldItem().isEmpty()) {
            ItemStack toInsert = stack.split(1);
            be.setHeldItem(toInsert);
            be.setChanged();
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
