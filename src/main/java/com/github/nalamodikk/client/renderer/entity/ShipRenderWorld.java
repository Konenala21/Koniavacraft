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
    private final ShipContraption contraption;            // 同步路徑(per-frame fallback)：讀 live contraption
    private final java.util.Map<BlockPos, BlockState> snapshot; // 背景烤路徑：讀不可變快照(thread-safe)
    @Nullable private final ShipLight light;              // 快照路徑：BFS 算好的真實光;live 路徑為 null(用 crude fallback)

    public ShipRenderWorld(Level real, ShipContraption contraption) {
        this.real = real;
        this.contraption = contraption;
        this.snapshot = null;
        this.light = null;
    }

    /** 背景執行緒烤 VBO 用：傳入方塊快照(不碰 live contraption，避免併發)。順便在背景算真實光照。 */
    public ShipRenderWorld(Level real, java.util.Map<BlockPos, BlockState> snapshot) {
        this.real = real;
        this.contraption = null;
        this.snapshot = snapshot;
        this.light = new ShipLight(snapshot);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (snapshot != null) return snapshot.getOrDefault(pos, Blocks.AIR.defaultBlockState());
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
        if (light != null) return layer == LightLayer.SKY ? light.sky(pos) : light.block(pos);
        if (layer == LightLayer.SKY) return 15; // live fallback(剛放方塊的暫顯,下次 re-bake 會有真光)
        return blockLight(pos);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarken) {
        if (light != null) return Math.max(light.sky(pos) - ambientDarken, light.block(pos));
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
