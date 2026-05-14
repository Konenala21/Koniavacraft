package com.github.nalamodikk.common.multiblock.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 定義多方塊結構的位置條件。
 *
 * <p>目前使用簡單的 {@code Predicate<BlockState>} 匹配，足夠應付固定結構。
 *
 * <p>未來可擴充方向：
 * <ul>
 *   <li><b>複雜謂詞系統</b>（參考 GT TraceabilityPredicate）：讓每個位置在匹配時
 *       執行額外邏輯，例如計數特定等級的方塊、累加能量上限、記錄匹配的 part 類型。</li>
 *   <li><b>可選位置</b>：部分位置允許空或多種選擇（hatch 系統）。</li>
 * </ul>
 */
public class MultiblockPattern {

    private final List<StructureEntry> entries;

    private MultiblockPattern(List<StructureEntry> entries) {
        this.entries = entries;
    }

    public boolean checkAt(Level level, BlockPos controllerPos) {
        for (StructureEntry entry : entries) {
            BlockPos worldPos = controllerPos.offset(entry.offset());
            if (!entry.predicate().test(level.getBlockState(worldPos))) {
                return false;
            }
        }
        return true;
    }

    public List<BlockPos> getPartPositions(BlockPos controllerPos) {
        List<BlockPos> positions = new ArrayList<>();
        for (StructureEntry entry : entries) {
            positions.add(controllerPos.offset(entry.offset()));
        }
        return positions;
    }

    public record StructureEntry(Vec3i offset, Predicate<BlockState> predicate) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<StructureEntry> entries = new ArrayList<>();

        public Builder require(Vec3i offset, Predicate<BlockState> predicate) {
            entries.add(new StructureEntry(offset, predicate));
            return this;
        }

        public Builder requireBlock(Vec3i offset, Block block) {
            return require(offset, state -> state.is(block));
        }

        public Builder requireBlock(int x, int y, int z, Block block) {
            return requireBlock(new Vec3i(x, y, z), block);
        }

        public MultiblockPattern build() {
            return new MultiblockPattern(List.copyOf(entries));
        }
    }
}
