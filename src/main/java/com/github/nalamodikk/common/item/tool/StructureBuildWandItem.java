package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.block.blockentity.altar.AltarGeometry;
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

        List<BlockPos> missingPillar    = new ArrayList<>();
        List<BlockPos> missingPedestal  = new ArrayList<>();
        List<BlockPos> blocked          = new ArrayList<>();
        collectPositions(level, altarPos, altar, missingPillar, missingPedestal, blocked);

        // 被其他方塊佔用 → 橘色高亮，停止建造
        if (!blocked.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                BlockHighlightPacket.sendToPlayer(sp, blocked, 1);
            player.displayClientMessage(
                    Component.translatable("message.koniava.build_wand.blocked", blocked.size()), true);
            return InteractionResult.SUCCESS;
        }

        // 全部就位
        if (missingPillar.isEmpty() && missingPedestal.isEmpty()) {
            altar.refreshUpgradeTier();
            Component msg;
            if (!altar.isFormed()) {
                msg = Component.translatable("message.koniava.build_wand.ready_to_form");
            } else if (altar.getUpgradeTier() >= AltarGeometry.ALL_RINGS.size()) {
                msg = Component.translatable("message.koniava.build_wand.all_rings_done",
                        AltarGeometry.ALL_RINGS.size());
            } else {
                msg = Component.translatable("message.koniava.build_wand.ring_ready",
                        altar.getUpgradeTier() + 1);
            }
            player.displayClientMessage(msg, true);
            return InteractionResult.SUCCESS;
        }

        int placed = 0;
        List<BlockPos> stillMissing = new ArrayList<>();

        // 放置魔力方塊（柱子）
        for (BlockPos pos : missingPillar) {
            if (player.isCreative()) {
                level.setBlock(pos, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                placed++;
            } else {
                int slot = findInInventory(player, ModBlocks.MANA_BLOCK.get().asItem());
                if (slot >= 0) {
                    level.setBlock(pos, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                    player.getInventory().getItem(slot).shrink(1);
                    placed++;
                } else {
                    stillMissing.add(pos);
                }
            }
        }

        // 放置底座（AspectPedestal）
        for (BlockPos pos : missingPedestal) {
            if (player.isCreative()) {
                level.setBlock(pos, ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState(), 3);
                placed++;
            } else {
                int slot = findInInventory(player, ModBlocks.ASPECT_PEDESTAL.get().asItem());
                if (slot >= 0) {
                    level.setBlock(pos, ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState(), 3);
                    player.getInventory().getItem(slot).shrink(1);
                    placed++;
                } else {
                    stillMissing.add(pos);
                }
            }
        }

        if (!stillMissing.isEmpty() && player instanceof ServerPlayer sp)
            BlockHighlightPacket.sendToPlayer(sp, stillMissing, 0); // 紅色

        if (placed > 0) altar.refreshUpgradeTier();

        player.displayClientMessage(
                Component.translatable("message.koniava.build_wand.placed",
                        placed, placed + stillMissing.size()), true);
        return InteractionResult.SUCCESS;
    }

    private void collectPositions(Level level, BlockPos altarPos, AspectAltarBlockEntity altar,
                                   List<BlockPos> missingPillar, List<BlockPos> missingPedestal,
                                   List<BlockPos> blocked) {
        if (!altar.isFormed()) {
            // 未成形：檢查柱子 + 底座
            checkPillarPositions(level, altarPos, AltarGeometry.PILLAR_BOTTOM, missingPillar, blocked);
            checkPillarPositions(level, altarPos, AltarGeometry.PILLAR_TOP,   missingPillar, blocked);
            checkPedestalPositions(level, altarPos, AltarGeometry.PEDESTAL_OFFSETS, missingPedestal, blocked);
        } else {
            // 已成形：檢查下一個升級環
            int nextTier = altar.getUpgradeTier() + 1;
            if (nextTier <= AltarGeometry.ALL_RINGS.size()) {
                checkRingPositions(level, altarPos, AltarGeometry.ALL_RINGS.get(nextTier - 1),
                        missingPillar, blocked);
            }
        }
    }

    private void checkPillarPositions(Level level, BlockPos altarPos, List<Vec3i> offsets,
                                       List<BlockPos> missing, List<BlockPos> blocked) {
        for (Vec3i offset : offsets) {
            BlockPos p = altarPos.offset(offset);
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MANA_BLOCK.get()) || s.is(ModBlocks.ALTAR_PILLAR.get())) continue;
            if (s.isAir() || s.canBeReplaced()) missing.add(p);
            else blocked.add(p);
        }
    }

    private void checkPedestalPositions(Level level, BlockPos altarPos, List<Vec3i> offsets,
                                         List<BlockPos> missing, List<BlockPos> blocked) {
        for (Vec3i offset : offsets) {
            BlockPos p = altarPos.offset(offset);
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.ASPECT_PEDESTAL.get())) continue;
            if (s.isAir() || s.canBeReplaced()) missing.add(p);
            else blocked.add(p);
        }
    }

    private void checkRingPositions(Level level, BlockPos altarPos, List<Vec3i> offsets,
                                     List<BlockPos> missing, List<BlockPos> blocked) {
        for (Vec3i offset : offsets) {
            BlockPos p = altarPos.offset(offset);
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MANA_BLOCK.get()) || s.is(ModBlocks.RESONANCE_RING.get())) continue;
            if (s.isAir() || s.canBeReplaced()) missing.add(p);
            else blocked.add(p);
        }
    }

    private int findInInventory(Player player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) return i;
        }
        return -1;
    }
}
