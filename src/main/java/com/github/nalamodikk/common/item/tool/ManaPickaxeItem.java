package com.github.nalamodikk.common.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class ManaPickaxeItem extends PickaxeItem {

    private static final int MAX_CHAIN_BLOCKS = 32;

    public ManaPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.koniava.mana_pickaxe.tooltip"));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean result = super.mineBlock(stack, level, state, pos, miningEntity);

        if (!level.isClientSide()
                && miningEntity instanceof Player player
                && !player.isShiftKeyDown()
                && state.is(Tags.Blocks.ORES)
                && level instanceof ServerLevel serverLevel) {
            chainMine(stack, serverLevel, state, pos, player);
        }

        return result;
    }

    private void chainMine(ItemStack stack, ServerLevel level, BlockState originalState, BlockPos originalPos, Player player) {
        if (player.isCreative()) return;

        Set<BlockPos> toMine = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        visited.add(originalPos);

        // 從原始方塊的六個面開始搜尋相同方塊
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = originalPos.relative(dir);
            if (visited.add(neighbor) && level.getBlockState(neighbor).is(originalState.getBlock())) {
                toMine.add(neighbor);
                queue.add(neighbor);
            }
        }

        // BFS 擴散
        while (!queue.isEmpty() && toMine.size() < MAX_CHAIN_BLOCKS) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.add(neighbor) && level.getBlockState(neighbor).is(originalState.getBlock())) {
                    toMine.add(neighbor);
                    if (toMine.size() < MAX_CHAIN_BLOCKS) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        // 依序破壞，每格消耗 1 耐久
        for (BlockPos minePos : toMine) {
            if (stack.isEmpty()) break;
            BlockState mineState = level.getBlockState(minePos);
            if (!mineState.is(originalState.getBlock())) continue;

            BlockEntity be = level.getBlockEntity(minePos);
            level.removeBlock(minePos, false);
            Block.dropResources(mineState, level, minePos, be, player, stack);
            stack.hurtAndBreak(1, level,
                    player instanceof ServerPlayer sp ? sp : null,
                    item -> {});
        }
    }
}
