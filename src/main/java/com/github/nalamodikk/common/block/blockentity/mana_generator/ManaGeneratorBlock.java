package com.github.nalamodikk.common.block.blockentity.mana_generator;

import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import com.github.nalamodikk.research.ResearchGate;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ManaGeneratorBlock extends BaseMachineBlock {
    public static final MapCodec<ManaGeneratorBlock> CODEC = simpleCodec(ManaGeneratorBlock::new);

    public ManaGeneratorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false)); // 預設不運作

    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active"); // 定義運作狀態



// 1. 在 ManaGeneratorBlock.java 中添加 neighborChanged 方法（重要！）

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        // 魔力發電機收到鄰居變化通知
        // 雖然發電機本身可能不需要特殊處理，但這確保了 neighborChanged 鏈能正常工作
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE); // 註冊 ACTIVE 屬性
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return state.getValue(ACTIVE) ? 15 : 0; // 運作時發光，否則不發光
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide() && !ResearchGate.canUse("mana_generator", player, level)) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ManaGeneratorBlockEntity generator) {
                ((ServerPlayer) player).openMenu(
                        new SimpleMenuProvider(generator, Component.translatable("block.koniava.mana_generator")),
                        pos
                );
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            // 如果是 ManaGeneratorBlockEntity，掉落物品
            if (blockEntity instanceof ManaGeneratorBlockEntity generator) {
                // 掉落升級物品
                Containers.dropContents(level, pos, generator.getUpgradeInventory());
                
                level.invalidateCapabilities(pos); // ❗❗通知 NeoForge: 這個位置的能力不可靠了，清除快取！
            }

        }
        super.onRemove(state, level, pos, newState, isMoving);
    }



    //    state, level, pos, newState, isMovin


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaGeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
        // 這樣 Minecraft 就會用 BlockEntityRenderer 而不是 JSON 模型
    }


}
