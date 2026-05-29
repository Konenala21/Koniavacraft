package com.github.nalamodikk.common.block.blockentity.conduit;

import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 導管的玩家互動：扳手切換各面 IO 設定、空手或 CONFIGURE_IO 模式顯示導管資訊面板。
 * 純讀寫 {@link ArcaneConduitBlockEntity} 的 public API（getIOConfig / setIOConfig / getTier /
 * getManaStored / getActiveConnectionCount），不碰內部欄位。
 */
final class ConduitInteractionHandler {

    private final ArcaneConduitBlockEntity conduit;

    ConduitInteractionHandler(ArcaneConduitBlockEntity conduit) {
        this.conduit = conduit;
    }

    InteractionResult onUse(BlockState state, Level level, BlockPos pos,
                            Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof BasicTechWandItem wand) {
            BasicTechWandItem.TechWandMode mode = wand.getMode(heldItem);
            Direction hitFace = hit.getDirection();

            switch (mode) {
                case DIRECTION_CONFIG -> {
                    IOHandlerUtils.IOType current = conduit.getIOConfig(hitFace);
                    IOHandlerUtils.IOType next = IOHandlerUtils.nextIOType(current);
                    conduit.setIOConfig(hitFace, next);

                    player.displayClientMessage(Component.translatable(
                            "message.koniava.wrench.conduit_mode",
                            Component.translatable("direction.koniava." + hitFace.name().toLowerCase()),
                            Component.translatable("mode.koniava." + next.name().toLowerCase())
                    ), true);

                    return InteractionResult.SUCCESS;
                }

                case CONFIGURE_IO -> {
                    showConduitInfo(player);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        if (heldItem.isEmpty()) {
            showConduitInfo(player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void showConduitInfo(Player player) {
        player.displayClientMessage(Component.translatable("message.koniava.conduit.info_header"), false);

        // 🆕 顯示等級資訊
        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.tier",
                Component.translatable(conduit.getTier().getDisplayName())), false);

        // 🆕 顯示傳輸速率
        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.transfer_rate",
                conduit.getTier().getTransferRate()), false);

        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.mana_status",
                conduit.getManaStored(), conduit.getMaxManaStored()), false);

        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.connections",
                conduit.getActiveConnectionCount()), false);

        // 顯示IO配置
        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = conduit.getIOConfig(dir);
            String color = switch (type) {
                case INPUT -> "§2";
                case OUTPUT -> "§c";
                case BOTH -> "§b";
                case DISABLED -> "§8";
            };

            player.displayClientMessage(Component.translatable(
                    "message.koniava.conduit.direction_config",
                    Component.translatable("direction.koniava." + dir.name().toLowerCase()),
                    Component.literal(color).append(Component.translatable("mode.koniava." + type.name().toLowerCase()))
            ), false);
        }
    }
}
