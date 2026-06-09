package com.github.nalamodikk.common.item.tool.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * 結構建造杖要求的單一方塊位置。
 * @param pos       世界座標
 * @param satisfied 此格現有 state 是否已算「滿足」(例如柱子可被 mana_block 或 altar_pillar 滿足)
 * @param toPlace   不滿足且可放時,要放下的 state
 * @param cost      生存模式放置時要消耗的物品(創造不消耗)
 */
public record RequiredBlock(BlockPos pos, Predicate<BlockState> satisfied, BlockState toPlace, Item cost) {
}
