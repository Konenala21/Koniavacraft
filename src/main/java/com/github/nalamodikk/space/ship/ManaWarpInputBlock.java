package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.capability.mana.ManaAction;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    /** 右鍵拿燃料物品 → 直接燒成能量(跟燃料槽一樣);非燃料 → 落到 useWithoutItem 顯示能量。 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        int energy = ShipFuels.energyOf(stack);
        if (energy <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
        if (!(level.getBlockEntity(pos) instanceof ManaWarpInputBlockEntity in))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int room = ManaWarpInputBlockEntity.CAPACITY - in.getEnergy().getManaStored();
        int n = Math.min(stack.getCount(), room / energy);
        if (n <= 0) {
            player.displayClientMessage(Component.translatable("message.koniava.fuel_tank.full"), true);
            return ItemInteractionResult.SUCCESS;
        }
        in.getEnergy().receiveMana(n * energy, ManaAction.EXECUTE);
        if (!player.getAbilities().instabuild) stack.shrink(n);
        player.displayClientMessage(Component.translatable("message.koniava.fuel_tank.refueled",
                n, in.getEnergy().getManaStored(), ManaWarpInputBlockEntity.CAPACITY), true);
        return ItemInteractionResult.SUCCESS;
    }

    /** 右鍵空手/非燃料：動作列顯示目前曲速能量 / 容量。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ManaWarpInputBlockEntity in) {
            player.displayClientMessage(Component.translatable("message.koniava.warp_input.energy",
                    in.getEnergy().getManaStored(), ManaWarpInputBlockEntity.CAPACITY), true);
        }
        return InteractionResult.SUCCESS;
    }
}
