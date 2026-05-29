package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.common.multiblock.api.MultiblockPattern;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 祭壇多方塊結構的純幾何資料：底座 / 柱子 / 升級環偏移量與成形 pattern。
 *
 * 結構三層（核心在 y=0）：
 *   y= 0：只有核心
 *   y=-1：四斜角(±3,±3) MANA_BLOCK / ALTAR_PILLAR，核心正下方空氣
 *   y=-2：四斜角(±3,±3) MANA_BLOCK / ALTAR_PILLAR
 *         + 核心正下(0,0)催化物底座
 *         + 東西南北(±3,0)/(0,±3)各一底座
 *         其餘 . 位置可選擇性放置底座（PEDESTAL_SCAN_RADIUS 內皆可）
 *
 * 成形時角落自動換成 ALTAR_PILLAR；解散時還原 MANA_BLOCK。
 */
public final class AltarGeometry {

    private AltarGeometry() {}

    public static final Predicate<BlockState> PILLAR_PRED =
            state -> state.is(ModBlocks.MANA_BLOCK.get()) || state.is(ModBlocks.ALTAR_PILLAR.get());

    // 必要底座位置（成形前需手動放置）
    public static final List<Vec3i> PEDESTAL_OFFSETS = List.of(
            new Vec3i( 0, -2,  0),  // 核心正下方（催化物）
            new Vec3i( 0, -2, -3),  // North
            new Vec3i( 0, -2,  3),  // South
            new Vec3i(-3, -2,  0),  // West
            new Vec3i( 3, -2,  0)   // East
    );

    // y=-2 = 底段（top=false），y=-1 = 頂段（top=true）；四斜角距中心 ±3
    public static final List<Vec3i> PILLAR_BOTTOM = List.of(
            new Vec3i(-3, -2, -3), new Vec3i(-3, -2, 3),
            new Vec3i( 3, -2, -3), new Vec3i( 3, -2, 3)
    );
    public static final List<Vec3i> PILLAR_TOP = List.of(
            new Vec3i(-3, -1, -3), new Vec3i(-3, -1, 3),
            new Vec3i( 3, -1, -3), new Vec3i( 3, -1, 3)
    );
    public static final List<Vec3i> PILLAR_OFFSETS;
    static {
        PILLAR_OFFSETS = new ArrayList<>();
        PILLAR_OFFSETS.addAll(PILLAR_BOTTOM);
        PILLAR_OFFSETS.addAll(PILLAR_TOP);
    }

    public static final MultiblockPattern PATTERN = MultiblockPattern.builder()
            // 催化物底座（核心正下方 y=-2）
            .requireBlock(new Vec3i( 0, -2,  0), ModBlocks.ASPECT_PEDESTAL.get())
            // 四方向底座 North/South/West/East
            .requireBlock(new Vec3i( 0, -2, -3), ModBlocks.ASPECT_PEDESTAL.get())
            .requireBlock(new Vec3i( 0, -2,  3), ModBlocks.ASPECT_PEDESTAL.get())
            .requireBlock(new Vec3i(-3, -2,  0), ModBlocks.ASPECT_PEDESTAL.get())
            .requireBlock(new Vec3i( 3, -2,  0), ModBlocks.ASPECT_PEDESTAL.get())
            // 四斜角柱子底段
            .require(new Vec3i(-3, -2, -3), PILLAR_PRED)
            .require(new Vec3i(-3, -2,  3), PILLAR_PRED)
            .require(new Vec3i( 3, -2, -3), PILLAR_PRED)
            .require(new Vec3i( 3, -2,  3), PILLAR_PRED)
            // 四斜角柱子頂段
            .require(new Vec3i(-3, -1, -3), PILLAR_PRED)
            .require(new Vec3i(-3, -1,  3), PILLAR_PRED)
            .require(new Vec3i( 3, -1, -3), PILLAR_PRED)
            .require(new Vec3i( 3, -1,  3), PILLAR_PRED)
            .build();

    // ── 升級環位置（T1–T12：半徑 7/9/11/13，各3個平面）───────────────────────────
    public static final List<Vec3i> RING_T1 = List.of(
            new Vec3i( -7,  0,  0), new Vec3i( -6,  0, -3), new Vec3i( -6,  0, -2), new Vec3i( -6,  0, -1),
            new Vec3i( -6,  0,  1), new Vec3i( -6,  0,  2), new Vec3i( -6,  0,  3), new Vec3i( -5,  0, -4),
            new Vec3i( -5,  0,  4), new Vec3i( -4,  0, -5), new Vec3i( -4,  0,  5), new Vec3i( -3,  0, -6),
            new Vec3i( -3,  0,  6), new Vec3i( -2,  0, -6), new Vec3i( -2,  0,  6), new Vec3i( -1,  0, -6),
            new Vec3i( -1,  0,  6), new Vec3i(  0,  0, -7), new Vec3i(  0,  0,  7), new Vec3i(  1,  0, -6),
            new Vec3i(  1,  0,  6), new Vec3i(  2,  0, -6), new Vec3i(  2,  0,  6), new Vec3i(  3,  0, -6),
            new Vec3i(  3,  0,  6), new Vec3i(  4,  0, -5), new Vec3i(  4,  0,  5), new Vec3i(  5,  0, -4),
            new Vec3i(  5,  0,  4), new Vec3i(  6,  0, -3), new Vec3i(  6,  0, -2), new Vec3i(  6,  0, -1),
            new Vec3i(  6,  0,  1), new Vec3i(  6,  0,  2), new Vec3i(  6,  0,  3), new Vec3i(  7,  0,  0)
    );
    public static final List<Vec3i> RING_T2 = List.of(
            new Vec3i( -7,  0,  0), new Vec3i( -6, -3,  0), new Vec3i( -6, -2,  0), new Vec3i( -6, -1,  0),
            new Vec3i( -6,  1,  0), new Vec3i( -6,  2,  0), new Vec3i( -6,  3,  0), new Vec3i( -5, -4,  0),
            new Vec3i( -5,  4,  0), new Vec3i( -4, -5,  0), new Vec3i( -4,  5,  0), new Vec3i( -3, -6,  0),
            new Vec3i( -3,  6,  0), new Vec3i( -2, -6,  0), new Vec3i( -2,  6,  0), new Vec3i( -1, -6,  0),
            new Vec3i( -1,  6,  0), new Vec3i(  0, -7,  0), new Vec3i(  0,  7,  0), new Vec3i(  1, -6,  0),
            new Vec3i(  1,  6,  0), new Vec3i(  2, -6,  0), new Vec3i(  2,  6,  0), new Vec3i(  3, -6,  0),
            new Vec3i(  3,  6,  0), new Vec3i(  4, -5,  0), new Vec3i(  4,  5,  0), new Vec3i(  5, -4,  0),
            new Vec3i(  5,  4,  0), new Vec3i(  6, -3,  0), new Vec3i(  6, -2,  0), new Vec3i(  6, -1,  0),
            new Vec3i(  6,  1,  0), new Vec3i(  6,  2,  0), new Vec3i(  6,  3,  0), new Vec3i(  7,  0,  0)
    );
    public static final List<Vec3i> RING_T3 = List.of(
            new Vec3i(  0, -7,  0), new Vec3i(  0, -6, -3), new Vec3i(  0, -6, -2), new Vec3i(  0, -6, -1),
            new Vec3i(  0, -6,  1), new Vec3i(  0, -6,  2), new Vec3i(  0, -6,  3), new Vec3i(  0, -5, -4),
            new Vec3i(  0, -5,  4), new Vec3i(  0, -4, -5), new Vec3i(  0, -4,  5), new Vec3i(  0, -3, -6),
            new Vec3i(  0, -3,  6), new Vec3i(  0, -2, -6), new Vec3i(  0, -2,  6), new Vec3i(  0, -1, -6),
            new Vec3i(  0, -1,  6), new Vec3i(  0,  0, -7), new Vec3i(  0,  0,  7), new Vec3i(  0,  1, -6),
            new Vec3i(  0,  1,  6), new Vec3i(  0,  2, -6), new Vec3i(  0,  2,  6), new Vec3i(  0,  3, -6),
            new Vec3i(  0,  3,  6), new Vec3i(  0,  4, -5), new Vec3i(  0,  4,  5), new Vec3i(  0,  5, -4),
            new Vec3i(  0,  5,  4), new Vec3i(  0,  6, -3), new Vec3i(  0,  6, -2), new Vec3i(  0,  6, -1),
            new Vec3i(  0,  6,  1), new Vec3i(  0,  6,  2), new Vec3i(  0,  6,  3), new Vec3i(  0,  7,  0)
    );
    public static final List<Vec3i> RING_T4 = List.of(
            new Vec3i( -9,  0,  0), new Vec3i( -8,  0, -4), new Vec3i( -8,  0, -3), new Vec3i( -8,  0, -2),
            new Vec3i( -8,  0, -1), new Vec3i( -8,  0,  1), new Vec3i( -8,  0,  2), new Vec3i( -8,  0,  3),
            new Vec3i( -8,  0,  4), new Vec3i( -7,  0, -5), new Vec3i( -7,  0,  5), new Vec3i( -6,  0, -6),
            new Vec3i( -6,  0,  6), new Vec3i( -5,  0, -7), new Vec3i( -5,  0,  7), new Vec3i( -4,  0, -8),
            new Vec3i( -4,  0,  8), new Vec3i( -3,  0, -8), new Vec3i( -3,  0,  8), new Vec3i( -2,  0, -8),
            new Vec3i( -2,  0,  8), new Vec3i( -1,  0, -8), new Vec3i( -1,  0,  8), new Vec3i(  0,  0, -9),
            new Vec3i(  0,  0,  9), new Vec3i(  1,  0, -8), new Vec3i(  1,  0,  8), new Vec3i(  2,  0, -8),
            new Vec3i(  2,  0,  8), new Vec3i(  3,  0, -8), new Vec3i(  3,  0,  8), new Vec3i(  4,  0, -8),
            new Vec3i(  4,  0,  8), new Vec3i(  5,  0, -7), new Vec3i(  5,  0,  7), new Vec3i(  6,  0, -6),
            new Vec3i(  6,  0,  6), new Vec3i(  7,  0, -5), new Vec3i(  7,  0,  5), new Vec3i(  8,  0, -4),
            new Vec3i(  8,  0, -3), new Vec3i(  8,  0, -2), new Vec3i(  8,  0, -1), new Vec3i(  8,  0,  1),
            new Vec3i(  8,  0,  2), new Vec3i(  8,  0,  3), new Vec3i(  8,  0,  4), new Vec3i(  9,  0,  0)
    );
    public static final List<Vec3i> RING_T5 = List.of(
            new Vec3i( -9,  0,  0), new Vec3i( -8, -4,  0), new Vec3i( -8, -3,  0), new Vec3i( -8, -2,  0),
            new Vec3i( -8, -1,  0), new Vec3i( -8,  1,  0), new Vec3i( -8,  2,  0), new Vec3i( -8,  3,  0),
            new Vec3i( -8,  4,  0), new Vec3i( -7, -5,  0), new Vec3i( -7,  5,  0), new Vec3i( -6, -6,  0),
            new Vec3i( -6,  6,  0), new Vec3i( -5, -7,  0), new Vec3i( -5,  7,  0), new Vec3i( -4, -8,  0),
            new Vec3i( -4,  8,  0), new Vec3i( -3, -8,  0), new Vec3i( -3,  8,  0), new Vec3i( -2, -8,  0),
            new Vec3i( -2,  8,  0), new Vec3i( -1, -8,  0), new Vec3i( -1,  8,  0), new Vec3i(  0, -9,  0),
            new Vec3i(  0,  9,  0), new Vec3i(  1, -8,  0), new Vec3i(  1,  8,  0), new Vec3i(  2, -8,  0),
            new Vec3i(  2,  8,  0), new Vec3i(  3, -8,  0), new Vec3i(  3,  8,  0), new Vec3i(  4, -8,  0),
            new Vec3i(  4,  8,  0), new Vec3i(  5, -7,  0), new Vec3i(  5,  7,  0), new Vec3i(  6, -6,  0),
            new Vec3i(  6,  6,  0), new Vec3i(  7, -5,  0), new Vec3i(  7,  5,  0), new Vec3i(  8, -4,  0),
            new Vec3i(  8, -3,  0), new Vec3i(  8, -2,  0), new Vec3i(  8, -1,  0), new Vec3i(  8,  1,  0),
            new Vec3i(  8,  2,  0), new Vec3i(  8,  3,  0), new Vec3i(  8,  4,  0), new Vec3i(  9,  0,  0)
    );
    public static final List<Vec3i> RING_T6 = List.of(
            new Vec3i(  0, -9,  0), new Vec3i(  0, -8, -4), new Vec3i(  0, -8, -3), new Vec3i(  0, -8, -2),
            new Vec3i(  0, -8, -1), new Vec3i(  0, -8,  1), new Vec3i(  0, -8,  2), new Vec3i(  0, -8,  3),
            new Vec3i(  0, -8,  4), new Vec3i(  0, -7, -5), new Vec3i(  0, -7,  5), new Vec3i(  0, -6, -6),
            new Vec3i(  0, -6,  6), new Vec3i(  0, -5, -7), new Vec3i(  0, -5,  7), new Vec3i(  0, -4, -8),
            new Vec3i(  0, -4,  8), new Vec3i(  0, -3, -8), new Vec3i(  0, -3,  8), new Vec3i(  0, -2, -8),
            new Vec3i(  0, -2,  8), new Vec3i(  0, -1, -8), new Vec3i(  0, -1,  8), new Vec3i(  0,  0, -9),
            new Vec3i(  0,  0,  9), new Vec3i(  0,  1, -8), new Vec3i(  0,  1,  8), new Vec3i(  0,  2, -8),
            new Vec3i(  0,  2,  8), new Vec3i(  0,  3, -8), new Vec3i(  0,  3,  8), new Vec3i(  0,  4, -8),
            new Vec3i(  0,  4,  8), new Vec3i(  0,  5, -7), new Vec3i(  0,  5,  7), new Vec3i(  0,  6, -6),
            new Vec3i(  0,  6,  6), new Vec3i(  0,  7, -5), new Vec3i(  0,  7,  5), new Vec3i(  0,  8, -4),
            new Vec3i(  0,  8, -3), new Vec3i(  0,  8, -2), new Vec3i(  0,  8, -1), new Vec3i(  0,  8,  1),
            new Vec3i(  0,  8,  2), new Vec3i(  0,  8,  3), new Vec3i(  0,  8,  4), new Vec3i(  0,  9,  0)
    );
    public static final List<Vec3i> RING_T7 = List.of(
            new Vec3i(-11,  0,  0), new Vec3i(-10,  0, -4), new Vec3i(-10,  0, -3), new Vec3i(-10,  0, -2),
            new Vec3i(-10,  0, -1), new Vec3i(-10,  0,  1), new Vec3i(-10,  0,  2), new Vec3i(-10,  0,  3),
            new Vec3i(-10,  0,  4), new Vec3i( -9,  0, -6), new Vec3i( -9,  0, -5), new Vec3i( -9,  0,  5),
            new Vec3i( -9,  0,  6), new Vec3i( -8,  0, -7), new Vec3i( -8,  0,  7), new Vec3i( -7,  0, -8),
            new Vec3i( -7,  0,  8), new Vec3i( -6,  0, -9), new Vec3i( -6,  0,  9), new Vec3i( -5,  0, -9),
            new Vec3i( -5,  0,  9), new Vec3i( -4,  0,-10), new Vec3i( -4,  0, 10), new Vec3i( -3,  0,-10),
            new Vec3i( -3,  0, 10), new Vec3i( -2,  0,-10), new Vec3i( -2,  0, 10), new Vec3i( -1,  0,-10),
            new Vec3i( -1,  0, 10), new Vec3i(  0,  0,-11), new Vec3i(  0,  0, 11), new Vec3i(  1,  0,-10),
            new Vec3i(  1,  0, 10), new Vec3i(  2,  0,-10), new Vec3i(  2,  0, 10), new Vec3i(  3,  0,-10),
            new Vec3i(  3,  0, 10), new Vec3i(  4,  0,-10), new Vec3i(  4,  0, 10), new Vec3i(  5,  0, -9),
            new Vec3i(  5,  0,  9), new Vec3i(  6,  0, -9), new Vec3i(  6,  0,  9), new Vec3i(  7,  0, -8),
            new Vec3i(  7,  0,  8), new Vec3i(  8,  0, -7), new Vec3i(  8,  0,  7), new Vec3i(  9,  0, -6),
            new Vec3i(  9,  0, -5), new Vec3i(  9,  0,  5), new Vec3i(  9,  0,  6), new Vec3i( 10,  0, -4),
            new Vec3i( 10,  0, -3), new Vec3i( 10,  0, -2), new Vec3i( 10,  0, -1), new Vec3i( 10,  0,  1),
            new Vec3i( 10,  0,  2), new Vec3i( 10,  0,  3), new Vec3i( 10,  0,  4), new Vec3i( 11,  0,  0)
    );
    public static final List<Vec3i> RING_T8 = List.of(
            new Vec3i(-11,  0,  0), new Vec3i(-10, -4,  0), new Vec3i(-10, -3,  0), new Vec3i(-10, -2,  0),
            new Vec3i(-10, -1,  0), new Vec3i(-10,  1,  0), new Vec3i(-10,  2,  0), new Vec3i(-10,  3,  0),
            new Vec3i(-10,  4,  0), new Vec3i( -9, -6,  0), new Vec3i( -9, -5,  0), new Vec3i( -9,  5,  0),
            new Vec3i( -9,  6,  0), new Vec3i( -8, -7,  0), new Vec3i( -8,  7,  0), new Vec3i( -7, -8,  0),
            new Vec3i( -7,  8,  0), new Vec3i( -6, -9,  0), new Vec3i( -6,  9,  0), new Vec3i( -5, -9,  0),
            new Vec3i( -5,  9,  0), new Vec3i( -4,-10,  0), new Vec3i( -4, 10,  0), new Vec3i( -3,-10,  0),
            new Vec3i( -3, 10,  0), new Vec3i( -2,-10,  0), new Vec3i( -2, 10,  0), new Vec3i( -1,-10,  0),
            new Vec3i( -1, 10,  0), new Vec3i(  0,-11,  0), new Vec3i(  0, 11,  0), new Vec3i(  1,-10,  0),
            new Vec3i(  1, 10,  0), new Vec3i(  2,-10,  0), new Vec3i(  2, 10,  0), new Vec3i(  3,-10,  0),
            new Vec3i(  3, 10,  0), new Vec3i(  4,-10,  0), new Vec3i(  4, 10,  0), new Vec3i(  5, -9,  0),
            new Vec3i(  5,  9,  0), new Vec3i(  6, -9,  0), new Vec3i(  6,  9,  0), new Vec3i(  7, -8,  0),
            new Vec3i(  7,  8,  0), new Vec3i(  8, -7,  0), new Vec3i(  8,  7,  0), new Vec3i(  9, -6,  0),
            new Vec3i(  9, -5,  0), new Vec3i(  9,  5,  0), new Vec3i(  9,  6,  0), new Vec3i( 10, -4,  0),
            new Vec3i( 10, -3,  0), new Vec3i( 10, -2,  0), new Vec3i( 10, -1,  0), new Vec3i( 10,  1,  0),
            new Vec3i( 10,  2,  0), new Vec3i( 10,  3,  0), new Vec3i( 10,  4,  0), new Vec3i( 11,  0,  0)
    );
    public static final List<Vec3i> RING_T9 = List.of(
            new Vec3i(  0,-11,  0), new Vec3i(  0,-10, -4), new Vec3i(  0,-10, -3), new Vec3i(  0,-10, -2),
            new Vec3i(  0,-10, -1), new Vec3i(  0,-10,  1), new Vec3i(  0,-10,  2), new Vec3i(  0,-10,  3),
            new Vec3i(  0,-10,  4), new Vec3i(  0, -9, -6), new Vec3i(  0, -9, -5), new Vec3i(  0, -9,  5),
            new Vec3i(  0, -9,  6), new Vec3i(  0, -8, -7), new Vec3i(  0, -8,  7), new Vec3i(  0, -7, -8),
            new Vec3i(  0, -7,  8), new Vec3i(  0, -6, -9), new Vec3i(  0, -6,  9), new Vec3i(  0, -5, -9),
            new Vec3i(  0, -5,  9), new Vec3i(  0, -4,-10), new Vec3i(  0, -4, 10), new Vec3i(  0, -3,-10),
            new Vec3i(  0, -3, 10), new Vec3i(  0, -2,-10), new Vec3i(  0, -2, 10), new Vec3i(  0, -1,-10),
            new Vec3i(  0, -1, 10), new Vec3i(  0,  0,-11), new Vec3i(  0,  0, 11), new Vec3i(  0,  1,-10),
            new Vec3i(  0,  1, 10), new Vec3i(  0,  2,-10), new Vec3i(  0,  2, 10), new Vec3i(  0,  3,-10),
            new Vec3i(  0,  3, 10), new Vec3i(  0,  4,-10), new Vec3i(  0,  4, 10), new Vec3i(  0,  5, -9),
            new Vec3i(  0,  5,  9), new Vec3i(  0,  6, -9), new Vec3i(  0,  6,  9), new Vec3i(  0,  7, -8),
            new Vec3i(  0,  7,  8), new Vec3i(  0,  8, -7), new Vec3i(  0,  8,  7), new Vec3i(  0,  9, -6),
            new Vec3i(  0,  9, -5), new Vec3i(  0,  9,  5), new Vec3i(  0,  9,  6), new Vec3i(  0, 10, -4),
            new Vec3i(  0, 10, -3), new Vec3i(  0, 10, -2), new Vec3i(  0, 10, -1), new Vec3i(  0, 10,  1),
            new Vec3i(  0, 10,  2), new Vec3i(  0, 10,  3), new Vec3i(  0, 10,  4), new Vec3i(  0, 11,  0)
    );
    public static final List<Vec3i> RING_T10 = List.of(
            new Vec3i(-13,  0,  0), new Vec3i(-12,  0, -5), new Vec3i(-12,  0, -4), new Vec3i(-12,  0, -3),
            new Vec3i(-12,  0, -2), new Vec3i(-12,  0, -1), new Vec3i(-12,  0,  1), new Vec3i(-12,  0,  2),
            new Vec3i(-12,  0,  3), new Vec3i(-12,  0,  4), new Vec3i(-12,  0,  5), new Vec3i(-11,  0, -6),
            new Vec3i(-11,  0,  6), new Vec3i(-10,  0, -8), new Vec3i(-10,  0, -7), new Vec3i(-10,  0,  7),
            new Vec3i(-10,  0,  8), new Vec3i( -9,  0, -9), new Vec3i( -9,  0,  9), new Vec3i( -8,  0,-10),
            new Vec3i( -8,  0, 10), new Vec3i( -7,  0,-10), new Vec3i( -7,  0, 10), new Vec3i( -6,  0,-11),
            new Vec3i( -6,  0, 11), new Vec3i( -5,  0,-12), new Vec3i( -5,  0, 12), new Vec3i( -4,  0,-12),
            new Vec3i( -4,  0, 12), new Vec3i( -3,  0,-12), new Vec3i( -3,  0, 12), new Vec3i( -2,  0,-12),
            new Vec3i( -2,  0, 12), new Vec3i( -1,  0,-12), new Vec3i( -1,  0, 12), new Vec3i(  0,  0,-13),
            new Vec3i(  0,  0, 13), new Vec3i(  1,  0,-12), new Vec3i(  1,  0, 12), new Vec3i(  2,  0,-12),
            new Vec3i(  2,  0, 12), new Vec3i(  3,  0,-12), new Vec3i(  3,  0, 12), new Vec3i(  4,  0,-12),
            new Vec3i(  4,  0, 12), new Vec3i(  5,  0,-12), new Vec3i(  5,  0, 12), new Vec3i(  6,  0,-11),
            new Vec3i(  6,  0, 11), new Vec3i(  7,  0,-10), new Vec3i(  7,  0, 10), new Vec3i(  8,  0,-10),
            new Vec3i(  8,  0, 10), new Vec3i(  9,  0, -9), new Vec3i(  9,  0,  9), new Vec3i( 10,  0, -8),
            new Vec3i( 10,  0, -7), new Vec3i( 10,  0,  7), new Vec3i( 10,  0,  8), new Vec3i( 11,  0, -6),
            new Vec3i( 11,  0,  6), new Vec3i( 12,  0, -5), new Vec3i( 12,  0, -4), new Vec3i( 12,  0, -3),
            new Vec3i( 12,  0, -2), new Vec3i( 12,  0, -1), new Vec3i( 12,  0,  1), new Vec3i( 12,  0,  2),
            new Vec3i( 12,  0,  3), new Vec3i( 12,  0,  4), new Vec3i( 12,  0,  5), new Vec3i( 13,  0,  0)
    );
    public static final List<Vec3i> RING_T11 = List.of(
            new Vec3i(-13,  0,  0), new Vec3i(-12, -5,  0), new Vec3i(-12, -4,  0), new Vec3i(-12, -3,  0),
            new Vec3i(-12, -2,  0), new Vec3i(-12, -1,  0), new Vec3i(-12,  1,  0), new Vec3i(-12,  2,  0),
            new Vec3i(-12,  3,  0), new Vec3i(-12,  4,  0), new Vec3i(-12,  5,  0), new Vec3i(-11, -6,  0),
            new Vec3i(-11,  6,  0), new Vec3i(-10, -8,  0), new Vec3i(-10, -7,  0), new Vec3i(-10,  7,  0),
            new Vec3i(-10,  8,  0), new Vec3i( -9, -9,  0), new Vec3i( -9,  9,  0), new Vec3i( -8,-10,  0),
            new Vec3i( -8, 10,  0), new Vec3i( -7,-10,  0), new Vec3i( -7, 10,  0), new Vec3i( -6,-11,  0),
            new Vec3i( -6, 11,  0), new Vec3i( -5,-12,  0), new Vec3i( -5, 12,  0), new Vec3i( -4,-12,  0),
            new Vec3i( -4, 12,  0), new Vec3i( -3,-12,  0), new Vec3i( -3, 12,  0), new Vec3i( -2,-12,  0),
            new Vec3i( -2, 12,  0), new Vec3i( -1,-12,  0), new Vec3i( -1, 12,  0), new Vec3i(  0,-13,  0),
            new Vec3i(  0, 13,  0), new Vec3i(  1,-12,  0), new Vec3i(  1, 12,  0), new Vec3i(  2,-12,  0),
            new Vec3i(  2, 12,  0), new Vec3i(  3,-12,  0), new Vec3i(  3, 12,  0), new Vec3i(  4,-12,  0),
            new Vec3i(  4, 12,  0), new Vec3i(  5,-12,  0), new Vec3i(  5, 12,  0), new Vec3i(  6,-11,  0),
            new Vec3i(  6, 11,  0), new Vec3i(  7,-10,  0), new Vec3i(  7, 10,  0), new Vec3i(  8,-10,  0),
            new Vec3i(  8, 10,  0), new Vec3i(  9, -9,  0), new Vec3i(  9,  9,  0), new Vec3i( 10, -8,  0),
            new Vec3i( 10, -7,  0), new Vec3i( 10,  7,  0), new Vec3i( 10,  8,  0), new Vec3i( 11, -6,  0),
            new Vec3i( 11,  6,  0), new Vec3i( 12, -5,  0), new Vec3i( 12, -4,  0), new Vec3i( 12, -3,  0),
            new Vec3i( 12, -2,  0), new Vec3i( 12, -1,  0), new Vec3i( 12,  1,  0), new Vec3i( 12,  2,  0),
            new Vec3i( 12,  3,  0), new Vec3i( 12,  4,  0), new Vec3i( 12,  5,  0), new Vec3i( 13,  0,  0)
    );
    public static final List<Vec3i> RING_T12 = List.of(
            new Vec3i(  0,-13,  0), new Vec3i(  0,-12, -5), new Vec3i(  0,-12, -4), new Vec3i(  0,-12, -3),
            new Vec3i(  0,-12, -2), new Vec3i(  0,-12, -1), new Vec3i(  0,-12,  1), new Vec3i(  0,-12,  2),
            new Vec3i(  0,-12,  3), new Vec3i(  0,-12,  4), new Vec3i(  0,-12,  5), new Vec3i(  0,-11, -6),
            new Vec3i(  0,-11,  6), new Vec3i(  0,-10, -8), new Vec3i(  0,-10, -7), new Vec3i(  0,-10,  7),
            new Vec3i(  0,-10,  8), new Vec3i(  0, -9, -9), new Vec3i(  0, -9,  9), new Vec3i(  0, -8,-10),
            new Vec3i(  0, -8, 10), new Vec3i(  0, -7,-10), new Vec3i(  0, -7, 10), new Vec3i(  0, -6,-11),
            new Vec3i(  0, -6, 11), new Vec3i(  0, -5,-12), new Vec3i(  0, -5, 12), new Vec3i(  0, -4,-12),
            new Vec3i(  0, -4, 12), new Vec3i(  0, -3,-12), new Vec3i(  0, -3, 12), new Vec3i(  0, -2,-12),
            new Vec3i(  0, -2, 12), new Vec3i(  0, -1,-12), new Vec3i(  0, -1, 12), new Vec3i(  0,  0,-13),
            new Vec3i(  0,  0, 13), new Vec3i(  0,  1,-12), new Vec3i(  0,  1, 12), new Vec3i(  0,  2,-12),
            new Vec3i(  0,  2, 12), new Vec3i(  0,  3,-12), new Vec3i(  0,  3, 12), new Vec3i(  0,  4,-12),
            new Vec3i(  0,  4, 12), new Vec3i(  0,  5,-12), new Vec3i(  0,  5, 12), new Vec3i(  0,  6,-11),
            new Vec3i(  0,  6, 11), new Vec3i(  0,  7,-10), new Vec3i(  0,  7, 10), new Vec3i(  0,  8,-10),
            new Vec3i(  0,  8, 10), new Vec3i(  0,  9, -9), new Vec3i(  0,  9,  9), new Vec3i(  0, 10, -8),
            new Vec3i(  0, 10, -7), new Vec3i(  0, 10,  7), new Vec3i(  0, 10,  8), new Vec3i(  0, 11, -6),
            new Vec3i(  0, 11,  6), new Vec3i(  0, 12, -5), new Vec3i(  0, 12, -4), new Vec3i(  0, 12, -3),
            new Vec3i(  0, 12, -2), new Vec3i(  0, 12, -1), new Vec3i(  0, 12,  1), new Vec3i(  0, 12,  2),
            new Vec3i(  0, 12,  3), new Vec3i(  0, 12,  4), new Vec3i(  0, 12,  5), new Vec3i(  0, 13,  0)
    );
    // Only T1-T6 are active. RING_T7-T12 are defined above but not yet included here.
    public static final List<List<Vec3i>> ALL_RINGS = List.of(
            RING_T1, RING_T2, RING_T3, RING_T4, RING_T5, RING_T6
    );
}
