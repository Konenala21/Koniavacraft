package com.github.nalamodikk.space.ship;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 飛船核心方塊：飛船的駕駛/錨點，會跟著船走。組裝不在這裡觸發，
 * 要在組裝台 GUI 進行（核心必須蓋在組裝台投射的建造盒內）。
 * 右鍵核心只提示玩家去用組裝台。
 */
public class ShipCoreBlock extends Block {

    public ShipCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.ship_core.use_pad"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
