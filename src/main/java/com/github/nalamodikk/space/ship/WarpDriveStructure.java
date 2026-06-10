package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 曲速引擎 3×4×3 沙漏結構的圖案 + 偵測(在 contraption 的 local 座標、軸對齊、垂直)。
 * 殼 = 20 格:上下兩片 3×3 盤 + 中間 2 格頸。其中恰好 1 格是曲速核心、1 格是進料口、其餘 18 格是魔力合金，
 * 核心/進料口可放在 20 格殼位的任一格。頸層非中心的 16 格必須空(沙漏腰)。
 */
public final class WarpDriveStructure {
    private WarpDriveStructure() {}

    /** 殼的 20 格 offset(box 原點 = 底前左角 (0,0,0))。 */
    public static final List<Vec3i> SHELL = new ArrayList<>();
    /** 頸層非中心、必須空的 16 格。 */
    public static final List<Vec3i> NECK_EMPTY = new ArrayList<>();
    static {
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            SHELL.add(new Vec3i(x, 0, z)); // 底盤
            SHELL.add(new Vec3i(x, 3, z)); // 頂盤
        }
        SHELL.add(new Vec3i(1, 1, 1)); // 頸(下)
        SHELL.add(new Vec3i(1, 2, 1)); // 頸(上)
        for (int y = 1; y <= 2; y++) for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++)
            if (!(x == 1 && z == 1)) NECK_EMPTY.add(new Vec3i(x, y, z));
    }

    /** 一座完整結構:記錄它的進料口 local(曲速燃料從這抽)。 */
    public record Found(BlockPos intakeLocal) {}

    /**
     * 掃所有核心、找完整結構。每個核心試 20 個 box 擺位(核心可在殼的任一格),命中即算一座。
     * @param cores   所有曲速核心的 local
     * @param stateAt local → BlockState(沒方塊回 null)
     */
    public static List<Found> detect(Iterable<BlockPos> cores, Function<BlockPos, BlockState> stateAt) {
        List<Found> result = new ArrayList<>();
        Block coreB = ModBlocks.MANA_WARP_ENGINE.get();
        Block alloyB = ModBlocks.MANA_ALLOY_BLOCK.get();
        Block intakeB = ModBlocks.MANA_WARP_INPUT.get();
        for (BlockPos core : cores) {
            for (Vec3i slot : SHELL) {
                Found f = check(core.subtract(slot), stateAt, coreB, alloyB, intakeB);
                if (f != null) { result.add(f); break; } // 這核心找到一座就停
            }
        }
        return result;
    }

    private static Found check(BlockPos origin, Function<BlockPos, BlockState> stateAt,
                               Block coreB, Block alloyB, Block intakeB) {
        int cores = 0, intakes = 0;
        BlockPos intakeLocal = null;
        for (Vec3i s : SHELL) {
            BlockState st = stateAt.apply(origin.offset(s));
            if (st == null) return null;
            Block b = st.getBlock();
            if (b == coreB) cores++;
            else if (b == intakeB) { intakes++; intakeLocal = origin.offset(s); }
            else if (b != alloyB) return null;
        }
        if (cores != 1 || intakes != 1) return null;
        for (Vec3i e : NECK_EMPTY) {
            BlockState st = stateAt.apply(origin.offset(e));
            if (st != null && !st.isAir()) return null; // 沙漏腰要空
        }
        return new Found(intakeLocal);
    }
}
