package com.github.nalamodikk.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 飛船的「contraption」資料：一組相對於核心(anchor)的方塊 + 各自的 BlockEntity NBT。
 * 參考 Create 的 Contraption（組裝 BFS / capture / 序列化），砍掉旋轉、黏性、動作方塊。
 * 對照筆記：AI-context/.../系統設計/飛船Contraption-Create對照筆記.md
 *
 * M1 範圍：只做 assemble（flood-fill 掃相連方塊）+ 序列化。還沒有實體 / 渲染 / 移除世界。
 */
public class ShipContraption {

    public static final int MAX_BLOCKS = 256;

    private final Map<BlockPos, StructureBlockInfo> blocks = new HashMap<>();
    private final Map<BlockPos, CompoundTag> updateTags = new HashMap<>();
    private BlockPos anchor = BlockPos.ZERO;
    private AABB bounds = new AABB(BlockPos.ZERO);

    /**
     * 從核心位置 BFS flood-fill 抓所有相連的可移動方塊（含核心本身）。
     * 鄰居規則簡化版：6 面相連、非空氣、非液體方塊、非不可破壞，就納入。
     * 空氣/液體會被 poll 到但不通過 isMovable，因此不再往外擴散，自然成為邊界。
     *
     * @return true 組裝成功；false 超過上限或沒抓到任何方塊
     */
    public boolean assemble(Level world, BlockPos anchorPos) {
        this.anchor = anchorPos;
        this.bounds = new AABB(BlockPos.ZERO);
        blocks.clear();
        updateTags.clear();

        Queue<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(anchorPos);

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            if (!visited.add(pos)) continue;
            if (!world.isLoaded(pos)) return false;

            BlockState state = world.getBlockState(pos);
            if (!isMovable(state, world, pos)) continue;

            addBlock(world, pos);
            if (blocks.size() > MAX_BLOCKS) return false;

            for (Direction d : Direction.values()) {
                BlockPos np = pos.relative(d);
                if (!visited.contains(np)) frontier.add(np);
            }
        }
        return !blocks.isEmpty();
    }

    private static boolean isMovable(BlockState state, Level world, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof LiquidBlock) return false;   // 水/熔岩方塊本身（waterlogged 固體仍可移動）
        if (state.getDestroySpeed(world, pos) < 0) return false;     // 不可破壞（基岩等）
        return true;
    }

    private void addBlock(Level world, BlockPos pos) {
        BlockPos localPos = pos.subtract(anchor);
        BlockState state = world.getBlockState(pos);

        CompoundTag beNbt = null;
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null) {
            beNbt = be.saveWithFullMetadata(world.registryAccess());
            beNbt.remove("x");
            beNbt.remove("y");
            beNbt.remove("z");
            updateTags.put(localPos, be.getUpdateTag(world.registryAccess()));
        }

        blocks.put(localPos, new StructureBlockInfo(localPos, state, beNbt));
        bounds = bounds.minmax(new AABB(localPos));
    }

    // ── 序列化（M1 先用簡單 list；之後大船再換 Create 的 paletted 寫法）──────────

    public CompoundTag writeNbt(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Anchor", NbtUtils.writeBlockPos(anchor));
        ListTag list = new ListTag();
        for (StructureBlockInfo info : blocks.values()) {
            CompoundTag e = new CompoundTag();
            e.put("Pos", NbtUtils.writeBlockPos(info.pos()));
            e.put("State", NbtUtils.writeBlockState(info.state()));
            if (info.nbt() != null) e.put("Nbt", info.nbt());
            list.add(e);
        }
        tag.put("Blocks", list);
        return tag;
    }

    public void readNbt(Level world, CompoundTag tag) {
        blocks.clear();
        bounds = new AABB(BlockPos.ZERO);
        anchor = NbtUtils.readBlockPos(tag, "Anchor").orElse(BlockPos.ZERO);

        HolderGetter<Block> holder = world.holderLookup(Registries.BLOCK);
        ListTag list = tag.getList("Blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(e, "Pos").orElse(BlockPos.ZERO);
            BlockState state = NbtUtils.readBlockState(holder, e.getCompound("State"));
            CompoundTag nbt = e.contains("Nbt") ? e.getCompound("Nbt") : null;
            blocks.put(pos, new StructureBlockInfo(pos, state, nbt));
            bounds = bounds.minmax(new AABB(pos));
        }
    }

    // ── getters ──────────────────────────────────────────────────────────────

    public Map<BlockPos, StructureBlockInfo> getBlocks() { return blocks; }
    public int size() { return blocks.size(); }
    public BlockPos anchor() { return anchor; }
    public AABB bounds() { return bounds; }
}
