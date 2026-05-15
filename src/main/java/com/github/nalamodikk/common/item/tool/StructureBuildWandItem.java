package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.network.packet.client.BlockHighlightPacket;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class StructureBuildWandItem extends Item {

    public StructureBuildWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos altarPos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null || level.isClientSide()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(altarPos) instanceof AspectAltarBlockEntity altar)) return InteractionResult.PASS;

        List<BlockPos> missing  = new ArrayList<>();
        List<BlockPos> blocked  = new ArrayList<>();
        collectPositions(level, altarPos, altar, missing, blocked);

        // 有被其他方塊佔用的位置：橘色邊框高亮 + 取消建造
        if (!blocked.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                BlockHighlightPacket.sendToPlayer(sp, blocked, 1); // 1 = 橘色
            }
            player.displayClientMessage(
                    Component.translatable("message.koniava.build_wand.blocked", blocked.size()), true);
            return InteractionResult.SUCCESS;
        }

        if (missing.isEmpty()) {
            altar.refreshUpgradeTier();
            Component msg;
            if (!altar.isFormed()) {
                // 柱子已就位但結構尚未成形
                msg = Component.translatable("message.koniava.build_wand.ready_to_form");
            } else if (altar.getUpgradeTier() >= AspectAltarBlockEntity.ALL_RINGS.size()) {
                // 全部環完成（動態傳入最大 tier 數）
                msg = Component.translatable("message.koniava.build_wand.all_rings_done",
                        AspectAltarBlockEntity.ALL_RINGS.size());
            } else {
                // 當前 tier 的環已全部就位，環偵測將在下一個 tick 更新
                msg = Component.translatable("message.koniava.build_wand.ring_ready",
                        altar.getUpgradeTier() + 1);
            }
            player.displayClientMessage(msg, true);
            return InteractionResult.SUCCESS;
        }

        int placed = 0;
        List<BlockPos> stillMissing = new ArrayList<>();
        for (BlockPos pos : missing) {
            if (player.isCreative()) {
                level.setBlock(pos, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                placed++;
            } else {
                int slot = findManaBlockInInventory(player);
                if (slot >= 0) {
                    level.setBlock(pos, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                    player.getInventory().getItem(slot).shrink(1);
                    placed++;
                } else {
                    stillMissing.add(pos);
                }
            }
        }

        // 材料不足的位置：紅色邊框提示
        if (!stillMissing.isEmpty() && player instanceof ServerPlayer sp) {
            BlockHighlightPacket.sendToPlayer(sp, stillMissing, 0); // 0 = 紅色
        }

        if (placed > 0) altar.refreshUpgradeTier();

        player.displayClientMessage(
                Component.translatable("message.koniava.build_wand.placed",
                        placed, placed + stillMissing.size()),
                true);
        return InteractionResult.SUCCESS;
    }

    private void collectPositions(Level level, BlockPos altarPos, AspectAltarBlockEntity altar,
                                   List<BlockPos> missing, List<BlockPos> blocked) {
        if (!altar.isFormed()) {
            // 尚未成形：只顯示基礎柱位置
            checkPositions(level, altarPos, AspectAltarBlockEntity.PILLAR_BOTTOM, missing, blocked, true);
            checkPositions(level, altarPos, AspectAltarBlockEntity.PILLAR_TOP,   missing, blocked, true);
        } else {
            // 已成形：顯示下一個升級環的位置（T1 → T2 → T3）
            int nextTier = altar.getUpgradeTier() + 1;
            if (nextTier <= AspectAltarBlockEntity.ALL_RINGS.size()) {
                checkPositions(level, altarPos, AspectAltarBlockEntity.ALL_RINGS.get(nextTier - 1),
                        missing, blocked, false);
            }
        }
    }

    /** isPillar=true 時接受 ALTAR_PILLAR 為已完成，false 時接受 RESONANCE_RING */
    private void checkPositions(Level level, BlockPos altarPos, List<Vec3i> offsets,
                                  List<BlockPos> missing, List<BlockPos> blocked, boolean isPillar) {
        for (Vec3i offset : offsets) {
            BlockPos p = altarPos.offset(offset);
            BlockState s = level.getBlockState(p);

            // 已經是正確的結構方塊 → 跳過
            if (s.is(ModBlocks.MANA_BLOCK.get())) continue;
            if (isPillar && s.is(ModBlocks.ALTAR_PILLAR.get())) continue;
            if (!isPillar && s.is(ModBlocks.RESONANCE_RING.get())) continue;

            // 空氣或可替換方塊 → 待建造
            if (s.isAir() || s.canBeReplaced()) {
                missing.add(p);
            } else {
                // 有其他方塊佔用 → 阻擋
                blocked.add(p);
            }
        }
    }

    private int findManaBlockInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModBlocks.MANA_BLOCK.get().asItem())) return i;
        }
        return -1;
    }
}
