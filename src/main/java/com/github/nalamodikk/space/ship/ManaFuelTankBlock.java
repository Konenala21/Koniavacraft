package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.capability.mana.ManaAction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    /** 右鍵拿著燃料物品 → 加注(固態 tier:任何爐子燃料,energy=燃燒值×K)。非燃料 → 落到 useWithoutItem 顯示液位。 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        int energy = ShipFuels.energyOf(stack);
        if (energy <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // 不是燃料 → 顯示液位
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
        if (!(level.getBlockEntity(pos) instanceof ManaFuelTankBlockEntity tank))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int room = ManaFuelTankBlockEntity.CAPACITY - tank.getManaStorage().getManaStored();
        int n = Math.min(stack.getCount(), room / energy); // 放得下的整數顆
        if (n <= 0) {
            player.displayClientMessage(Component.translatable("message.koniava.fuel_tank.full"), true);
            return ItemInteractionResult.SUCCESS;
        }
        tank.getManaStorage().receiveMana(n * energy, ManaAction.EXECUTE);
        if (!player.getAbilities().instabuild) stack.shrink(n);
        player.displayClientMessage(Component.translatable("message.koniava.fuel_tank.refueled",
                n, tank.getManaStorage().getManaStored(), ManaFuelTankBlockEntity.CAPACITY), true);
        return ItemInteractionResult.SUCCESS;
    }

    /** 右鍵空手/非燃料：動作列顯示這個槽目前的燃料 / 容量。船上的槽走 forwardUseToShadow，level/pos 會是影子的真身。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ManaFuelTankBlockEntity tank) {
            player.displayClientMessage(Component.translatable("message.koniava.fuel_tank.level",
                    tank.getManaStorage().getManaStored(), ManaFuelTankBlockEntity.CAPACITY), true);
        }
        return InteractionResult.SUCCESS;
    }
}
