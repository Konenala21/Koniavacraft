package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.space.ship.ShipContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.Nullable;

/**
 * 渲染飛船用的「假世界」：方塊已從真實世界移除，渲染時靠這個回答 getBlockState（鄰面剔除/AO）
 * 與光照（含方塊自身發光值，修復發光方塊變實體後不亮的問題）。
 *
 * 座標用 contraption 的 localPos。光照簡化：天光固定 15，方塊光取該格 + 鄰格的最大發光值，
 * 所以發光石之類自己會亮、也照亮相鄰方塊。getShade/getLightEngine 委派真實 Level。
 */
public class ShipRenderWorld implements BlockAndTintGetter {

    private final Level real;
    private final ShipContraption contraption;

    public ShipRenderWorld(Level real, ShipContraption contraption) {
        this.real = real;
        this.contraption = contraption;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        StructureBlockInfo info = contraption.getBlocks().get(pos);
        return info != null ? info.state() : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null; // naive 渲染不畫需要 BER 的方塊
    }

    @Override
    public int getHeight() {
        return real.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return real.getMinBuildHeight();
    }

    // ── 光照 ──────────────────────────────────────────────────────────────

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        if (layer == LightLayer.SKY) return 15;
        return blockLight(pos);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarken) {
        return Math.max(15 - ambientDarken, blockLight(pos));
    }

    private int blockLight(BlockPos pos) {
        int max = getBlockState(pos).getLightEmission();
        for (Direction d : Direction.values()) {
            max = Math.max(max, getBlockState(pos.relative(d)).getLightEmission());
            if (max >= 15) return 15;
        }
        return max;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return real.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return real.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return -1; // 草/葉的生態著色：飛船上無生態，回白（佔位）
    }
}
