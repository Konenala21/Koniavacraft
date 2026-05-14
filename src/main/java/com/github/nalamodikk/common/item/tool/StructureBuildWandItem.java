package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

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

        // 計算需要補齊的位置：基礎柱 + 下一個 tier 的環
        List<BlockPos> missing = collectMissingPositions(level, altarPos, altar);

        if (missing.isEmpty()) {
            altar.refreshUpgradeTier();
            altar.onWandActivate(player);
            player.displayClientMessage(
                    Component.translatable("message.koniava.build_wand.complete"), true);
            return InteractionResult.SUCCESS;
        }

        int placed = 0;
        List<BlockPos> stillMissing = new ArrayList<>();
        for (BlockPos pos : missing) {
            BlockState current = level.getBlockState(pos);
            if (!current.isAir() && !current.canBeReplaced()) continue;

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

        // 有缺少的格子 → 粒子提示
        if (!stillMissing.isEmpty() && level instanceof ServerLevel serverLevel) {
            DustParticleOptions red = new DustParticleOptions(new Vector3f(1f, 0.2f, 0.2f), 1.2f);
            for (BlockPos pos : stillMissing) {
                serverLevel.sendParticles(red, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        6, 0.2, 0.2, 0.2, 0);
            }
        }

        if (placed > 0) altar.onWandActivate(player);

        player.displayClientMessage(
                Component.translatable("message.koniava.build_wand.placed",
                        placed, placed + stillMissing.size()),
                true);
        return InteractionResult.SUCCESS;
    }

    private List<BlockPos> collectMissingPositions(Level level, BlockPos altarPos, AspectAltarBlockEntity altar) {
        List<BlockPos> missing = new ArrayList<>();

        // 基礎柱位置（尚未成形時補齊）
        if (!altar.isFormed()) {
            for (Vec3i offset : AspectAltarBlockEntity.PILLAR_BOTTOM) {
                BlockPos p = altarPos.offset(offset);
                if (!level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())
                        && !level.getBlockState(p).is(ModBlocks.ALTAR_PILLAR.get())) {
                    missing.add(p);
                }
            }
            for (Vec3i offset : AspectAltarBlockEntity.PILLAR_TOP) {
                BlockPos p = altarPos.offset(offset);
                if (!level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())
                        && !level.getBlockState(p).is(ModBlocks.ALTAR_PILLAR.get())) {
                    missing.add(p);
                }
            }
        }

        // 下一個 tier 的環
        int nextTier = altar.getUpgradeTier() + 1;
        if (nextTier <= AspectAltarBlockEntity.ALL_RINGS.size()) {
            List<Vec3i> ring = AspectAltarBlockEntity.ALL_RINGS.get(nextTier - 1);
            for (Vec3i offset : ring) {
                BlockPos p = altarPos.offset(offset);
                if (!level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())) {
                    missing.add(p);
                }
            }
        }

        return missing;
    }

    private int findManaBlockInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModBlocks.MANA_BLOCK.get().asItem())) return i;
        }
        return -1;
    }
}
