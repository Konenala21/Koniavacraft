package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * 🔮 魔力注入機方塊
 *
 * 繼承自 BaseMachineBlock，獲得：
 * - 標準的方塊實體管理
 * - 朝向屬性支援
 * - 標準的掉落物處理
 *
 * 功能：
 * - 消耗魔力來強化物品
 * - 為物品添加特殊屬性
 * - 創造獨特的強化物品
 */
public class ManaInfuserBlock extends BaseMachineBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 🔧 額外的方塊屬性
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    // 📐 方塊形狀 (稍微小一點，更有科技感)
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    // 🎨 MapCodec (NeoForge 1.21.1 要求)
    public static final MapCodec<ManaInfuserBlock> CODEC = simpleCodec(ManaInfuserBlock::new);

    public ManaInfuserBlock(Properties properties) {
        super(properties);
        // 註冊默認狀態（包含基類的 FACING 和我們的 WORKING）
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WORKING, false));
    }

    @Override
    protected MapCodec<ManaInfuserBlock> codec() {
        return CODEC;
    }

    // === 🏗️ 方塊屬性（擴展基類）===
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 🎯 重要：使用 getOpposite() 讓方塊面向玩家
        // context.getHorizontalDirection() 返回玩家面向的方向
        // 我們通常希望方塊的"正面"面向玩家，所以使用相反方向
        Direction facing = context.getHorizontalDirection().getOpposite();


        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WORKING, false);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // 添加 FACING
        builder.add(WORKING);                      // 添加 WORKING
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // === 🔧 BlockEntity 管理 ===

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaInfuserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MANA_INFUSER.get(),
                (level1, pos, state1, blockEntity) -> {
                    // 使用基類的標準化 tick 方法
                    blockEntity.tick(level1, pos, state1, blockEntity);
                });
    }

    // === 🎮 玩家交互 ===

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ManaInfuserBlockEntity infuser) {

            // 🔧 如果拿著科技魔杖，打開配置界面
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof BasicTechWandItem) {
                return handleWandInteraction(player, infuser, heldItem, hitResult.getDirection());
            }

            // 🖥️ 否則打開主界面
            openManaInfuserMenu((ServerPlayer) player, infuser);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 🔧 處理科技魔杖交互
     */
    private InteractionResult handleWandInteraction(Player player, ManaInfuserBlockEntity infuser, ItemStack wand, Direction hitSide) {
        if (player.isCrouching()) {
            // Shift + 右鍵：切換 IO 模式
            infuser.toggleIOMode(hitSide);

            String modeName = infuser.getIOMode(hitSide).name(); // 使用 .name() 而不是 .getDisplayName()
            player.sendSystemMessage(Component.translatable("message.koniava.io_mode_changed")
                    .append(Component.literal(": "))
                    .append(Component.translatable("mode.koniava." + modeName.toLowerCase()))
                    .withStyle(ChatFormatting.GREEN));
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 🖥️ 打開魔力注入機界面
     */
    private void openManaInfuserMenu(ServerPlayer player, ManaInfuserBlockEntity infuser) {
        MenuProvider menuProvider = new SimpleMenuProvider(
                (id, playerInventory, accessPlayer) -> infuser.createMenu(id, playerInventory, accessPlayer),
                infuser.getDisplayName()
        );

        player.openMenu(menuProvider, infuser.getBlockPos());
    }

    /**
     * 📊 顯示注入機狀態
     */


    // === ⚡ 配置接口實現 ===

    public boolean isConfigurable() {
        return true;
    }

    public InteractionResult handleConfigurationInteraction(Player player, BlockEntity blockEntity, ItemStack tool, Direction hitDirection) {
        if (blockEntity instanceof ManaInfuserBlockEntity infuser) {
            return handleWandInteraction(player, infuser, tool, hitDirection);
        }
        return InteractionResult.PASS;
    }

    // === 🔄 方塊更新通知 ===

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // 通知 BlockEntity 鄰居改變
        if (level.getBlockEntity(pos) instanceof ManaInfuserBlockEntity infuser) {
            infuser.onNeighborChanged();
        }
    }


}
