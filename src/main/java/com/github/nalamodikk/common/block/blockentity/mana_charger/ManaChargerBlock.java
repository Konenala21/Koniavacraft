package com.github.nalamodikk.common.block.blockentity.mana_charger;

import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ManaChargerBlock extends BaseMachineBlock {

    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    public static final MapCodec<ManaChargerBlock> CODEC = simpleCodec(ManaChargerBlock::new);

    public ManaChargerBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(WORKING, false));
    }

    @Override
    protected MapCodec<? extends ManaChargerBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaChargerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MANA_CHARGER_BE.get(),
                (lvl, pos, st, be) -> { if (!lvl.isClientSide()) be.tickMachine(); });
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManaChargerBlockEntity charger) {
                if (!NaraServerEvents.isGhostBlock(pos)) {
                    ItemStack drop = new ItemStack(this);
                    CompoundTag tag = charger.saveWithoutMetadata(level.registryAccess());
                    net.minecraft.world.item.BlockItem.setBlockEntityData(drop, charger.getType(), tag);
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
                }
                // Clear slot so BaseMachineBlock.onRemove → drops() doesn't duplicate the item
                if (charger.getItemHandler() != null) {
                    charger.getItemHandler().setStackInSlot(ManaChargerBlockEntity.ITEM_SLOT, ItemStack.EMPTY);
                }
                level.invalidateCapabilities(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ManaChargerBlockEntity charger) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> charger.createMenu(id, inv, p),
                    charger.getDisplayName()
            ), charger.getBlockPos());
        }
        return InteractionResult.SUCCESS;
    }
}
