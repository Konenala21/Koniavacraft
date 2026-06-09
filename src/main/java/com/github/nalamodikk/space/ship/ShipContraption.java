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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

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

    public static final int MAX_BLOCKS = 2048; // VBO 烤好後放大（原 512 是 naive 渲染暫限）；再大要顧碰撞 VoxelShape/鏡射成本

    private final Map<BlockPos, StructureBlockInfo> blocks = new HashMap<>();
    private final Map<BlockPos, CompoundTag> updateTags = new HashMap<>();
    private BlockPos anchor = BlockPos.ZERO;
    private AABB bounds = new AABB(BlockPos.ZERO);

    /**
     * 從核心位置 BFS flood-fill 抓所有相連的可移動方塊（含核心本身），但**只在建造盒內擴散**。
     * 盒由組裝台投射（min/max 含端點），盒外的地面/牆碰到也不抓；組裝台在盒下方，自然排除。
     * 鄰居規則簡化版：6 面相連、在盒內、非空氣、非液體方塊、非不可破壞，就納入。
     *
     * @return true 組裝成功；false 超過上限或沒抓到任何方塊
     */
    public boolean assemble(Level world, BlockPos anchorPos, BlockPos boxMin, BlockPos boxMax) {
        this.anchor = anchorPos;
        this.bounds = new AABB(BlockPos.ZERO);
        blocks.clear();
        updateTags.clear();

        if (!inBox(anchorPos, boxMin, boxMax)) return false;

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

            // 26 連通(面+邊+角):斜對角接的方塊也算連通，組裝不會在斜接處斷開。
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos np = pos.offset(dx, dy, dz);
                        if (!visited.contains(np) && inBox(np, boxMin, boxMax)) frontier.add(np);
                    }
        }
        return !blocks.isEmpty();
    }

    private static boolean inBox(BlockPos p, BlockPos min, BlockPos max) {
        return p.getX() >= min.getX() && p.getX() <= max.getX()
                && p.getY() >= min.getY() && p.getY() <= max.getY()
                && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    private static boolean isMovable(BlockState state, Level world, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof LiquidBlock) return false;   // 水/熔岩方塊本身（waterlogged 固體仍可移動）
        if (state.getDestroySpeed(world, pos) < 0) return false;     // 不可破壞（基岩等）
        // 發射台結構（底座/組裝架/組裝台）是鷹架，不算飛船的一部分，當邊界
        if (state.getBlock() instanceof ShipAssemblyBaseBlock
                || state.getBlock() instanceof ShipAssemblyGantryBlock
                || state.getBlock() instanceof ShipAssemblyPadBlock) return false;
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

    /**
     * spawn 同步用的直接 buffer 編碼(網路熱路徑)：每方塊用全域 blockstate ID(一個 varint)而非整顆
     * blockstate NBT compound。比 writeNbt+writeNbt 快十幾倍、體積小一個量級(2000 方塊 41ms/110KB → 數 ms/~10KB)。
     * NBT 版(writeNbt/readNbt)留給存檔。只有 BE 方塊(機器/箱子)才額外帶 NBT。
     */
    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeBlockPos(anchor);
        buf.writeVarInt(blocks.size());
        for (StructureBlockInfo info : blocks.values()) {
            buf.writeBlockPos(info.pos());
            buf.writeVarInt(Block.getId(info.state()));
            boolean hasNbt = info.nbt() != null;
            buf.writeBoolean(hasNbt);
            if (hasNbt) buf.writeNbt(info.nbt());
        }
    }

    public void readFromBuf(FriendlyByteBuf buf) {
        blocks.clear();
        bounds = new AABB(BlockPos.ZERO);
        anchor = buf.readBlockPos();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            BlockPos pos = buf.readBlockPos();
            BlockState state = Block.stateById(buf.readVarInt());
            CompoundTag nbt = buf.readBoolean() ? buf.readNbt() : null;
            blocks.put(pos, new StructureBlockInfo(pos, state, nbt));
            bounds = bounds.minmax(new AABB(pos));
        }
    }

    /**
     * 把組裝到的方塊從世界移除（換成空氣），改由 ShipEntity 承載。
     * 用 UPDATE_SUPPRESS_DROPS 避免箱子/機器把內容物噴一地（內容物已存在 contraption NBT 裡）。
     */
    public void removeFromWorld(Level world) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE;
        for (BlockPos localPos : blocks.keySet()) {
            BlockPos wp = localPos.offset(anchor);
            world.removeBlockEntity(wp);
            world.setBlock(wp, Blocks.AIR.defaultBlockState(), flags);
        }
    }

    /**
     * 把飛船方塊寫回世界（收船），套用 rotation（船會轉，yaw snap 到 90° 後的 Rotation）。
     * 位置與 blockstate 一起旋轉。先檢查所有目標格都可放，有一格被擋就整批不寫、回傳 false。
     * 箱子/機器用 loadWithComponents 還原內容物（NBT 補回 x/y/z）。
     */
    public boolean addToWorld(Level world, BlockPos targetAnchor, Rotation rotation) {
        for (BlockPos localPos : blocks.keySet()) {
            BlockPos wp = rotatedTarget(localPos, targetAnchor, rotation);
            BlockState existing = world.getBlockState(wp);
            if (!existing.isAir() && !existing.canBeReplaced()) return false;
        }
        for (Map.Entry<BlockPos, StructureBlockInfo> e : blocks.entrySet()) {
            BlockPos wp = rotatedTarget(e.getKey(), targetAnchor, rotation);
            StructureBlockInfo info = e.getValue();
            BlockState placed = info.state().rotate(rotation);
            // 結構方塊(絕大多數)用快 flag：不發鄰居連鎖、不依鄰居重算形狀(contraption 已存好連接狀態)，
            // 大幅省組裝/拆船的 setBlock 成本(2000 塊 ×UPDATE_ALL 的鄰居+光照連鎖會凍住)。
            // 機器/管線(EntityBlock，數量少)維持 UPDATE_ALL → 網路照常成形、行為不變。
            int flags = placed.getBlock() instanceof EntityBlock
                    ? Block.UPDATE_ALL
                    : (Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            world.setBlock(wp, placed, flags);
            if (info.nbt() != null) {
                BlockEntity be = world.getBlockEntity(wp);
                if (be != null) {
                    CompoundTag tag = info.nbt().copy();
                    tag.putInt("x", wp.getX());
                    tag.putInt("y", wp.getY());
                    tag.putInt("z", wp.getZ());
                    be.loadWithComponents(tag, world.registryAccess());
                }
            }
        }
        return true;
    }

    /** local 座標依 Rotation 旋轉後 + 錨點 = 目標世界座標（與 ShipEntity.rotatedWorldCorner 的方向一致）。 */
    private static BlockPos rotatedTarget(BlockPos local, BlockPos anchor, Rotation rotation) {
        int lx = local.getX(), ly = local.getY(), lz = local.getZ();
        int rx, rz;
        switch (rotation) {
            case CLOCKWISE_90 -> { rx = -lz; rz = lx; }
            case CLOCKWISE_180 -> { rx = -lx; rz = -lz; }
            case COUNTERCLOCKWISE_90 -> { rx = lz; rz = -lx; }
            default -> { rx = lx; rz = lz; }
        }
        return anchor.offset(rx, ly, rz);
    }

    // ── getters ──────────────────────────────────────────────────────────────

    public Map<BlockPos, StructureBlockInfo> getBlocks() { return blocks; }

    /** 換掉某 local 方塊的 blockstate（保留位置與 BE NBT）。互動方塊切狀態(門開關等)用。 */
    public void setBlockState(BlockPos local, BlockState state) {
        StructureBlockInfo old = blocks.get(local);
        if (old == null) return;
        blocks.put(local, new StructureBlockInfo(local, state, old.nbt()));
    }

    /** 換掉某 local 方塊的 BE NBT（保留位置與 blockstate）。箱子改內容物寫回用，拆解才保留。 */
    public void setBlockNbt(BlockPos local, CompoundTag nbt) {
        StructureBlockInfo old = blocks.get(local);
        if (old == null) return;
        blocks.put(local, new StructureBlockInfo(local, old.state(), nbt));
    }

    /** 加一個新方塊（停船編輯：放方塊）。會擴大 bounds。 */
    public void addBlock(BlockPos local, BlockState state, @Nullable CompoundTag nbt) {
        blocks.put(local, new StructureBlockInfo(local, state, nbt));
        bounds = bounds.minmax(new AABB(local));
    }

    /** 移除一個方塊（停船編輯：挖方塊）。bounds 重算（可能縮小）。 */
    public void removeBlock(BlockPos local) {
        if (blocks.remove(local) != null) recomputeBounds();
    }

    private void recomputeBounds() {
        AABB b = new AABB(BlockPos.ZERO);
        for (BlockPos p : blocks.keySet()) b = b.minmax(new AABB(p));
        bounds = b;
    }

    public int size() { return blocks.size(); }
    public BlockPos anchor() { return anchor; }
    public AABB bounds() { return bounds; }
}
