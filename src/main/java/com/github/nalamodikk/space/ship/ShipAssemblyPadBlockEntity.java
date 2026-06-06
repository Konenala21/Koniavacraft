package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * 飛船組裝台 BlockEntity（像火箭發射台的控制台）。
 *
 * 建造盒由玩家蓋的結構讀出來，不再是隱形固定盒：
 *   水平範圍(footprint) = 與組裝台同層、相連的「組裝底座」flood-fill 的外接矩形（底座可不規則）
 *   高度 = footprint 內最高「組裝架」柱頂到底座的高度（沒組裝架 → 預設低高度）
 *   盒 = footprint × [baseY+1, baseY+height]
 *
 * 兩個獨立上限：盒尺寸給寬鬆（MAX_FOOTPRINT/MAX_HEIGHT），方塊總數才是效能瓶頸
 * （ShipContraption.MAX_BLOCKS，naive 渲染階段壓低，之後烤 buffer + tier 再開大）。
 *
 * M1.5：只掃描回報，還沒生成飛船實體。
 */
public class ShipAssemblyPadBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MAX_FOOTPRINT = 32;   // footprint 單邊上限
    public static final int MAX_HEIGHT = 32;       // 高度上限
    public static final int MAX_BASE_BLOCKS = 2048;// 底座 flood-fill 上限
    public static final int DEFAULT_HEIGHT = 8;    // 沒組裝架時的預設高度

    // ContainerData：[0]方塊數 [1]核心數 [2]狀態 [3]盒寬X [4]盒高Y [5]盒深Z
    public static final int DATA_COUNT = 0;
    public static final int DATA_CORES = 1;
    public static final int DATA_STATUS = 2;
    public static final int DATA_BOX_W = 3;
    public static final int DATA_BOX_H = 4;
    public static final int DATA_BOX_D = 5;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_OK = 1;
    public static final int STATUS_NO_CORE = 2;
    public static final int STATUS_MULTI_CORE = 3;
    public static final int STATUS_FAILED = 4;
    public static final int STATUS_NO_BASE = 5;     // 旁邊沒有相連的組裝底座
    public static final int STATUS_TOO_BIG = 6;     // 底座超過 footprint 上限

    private final ContainerData data = new SimpleContainerData(6);

    // 最近一次掃描算出的盒（給 assemble 用；不持久化，掃描時重算）
    @Nullable private BlockPos boxMin;
    @Nullable private BlockPos boxMax;

    public ShipAssemblyPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIP_ASSEMBLY_PAD.get(), pos, state);
    }

    /** 掃描：讀結構算盒 → 找核心 → 組裝，結果寫進 ContainerData（server 端）。 */
    public void scan() {
        if (level == null || level.isClientSide) return;

        int baseY = worldPosition.getY();

        // 1) flood-fill 與組裝台同層、相連的底座（水平 4 鄰），取外接矩形
        Set<BlockPos> base = new HashSet<>();
        Queue<BlockPos> q = new ArrayDeque<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = worldPosition.relative(d);
            if (level.getBlockState(n).getBlock() instanceof ShipAssemblyBaseBlock) q.add(n);
        }
        if (q.isEmpty()) { setNoBox(STATUS_NO_BASE); return; }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (!base.add(p)) continue;
            if (base.size() > MAX_BASE_BLOCKS) { setNoBox(STATUS_TOO_BIG); return; }
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!base.contains(n) && level.getBlockState(n).getBlock() instanceof ShipAssemblyBaseBlock)
                    q.add(n);
            }
        }
        if (maxX - minX + 1 > MAX_FOOTPRINT || maxZ - minZ + 1 > MAX_FOOTPRINT) {
            setNoBox(STATUS_TOO_BIG);
            return;
        }

        // 2) 高度 = footprint 內最高組裝架柱（到底座的高度）；沒有就用預設
        int maxGantryY = baseY;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = baseY + 1; y <= baseY + MAX_HEIGHT; y++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof ShipAssemblyGantryBlock)
                        maxGantryY = Math.max(maxGantryY, y);
                }
            }
        }
        int height = maxGantryY > baseY ? Math.min(maxGantryY - baseY, MAX_HEIGHT) : DEFAULT_HEIGHT;

        boxMin = new BlockPos(minX, baseY + 1, minZ);
        boxMax = new BlockPos(maxX, baseY + height, maxZ);
        int boxW = maxX - minX + 1, boxD = maxZ - minZ + 1;

        // 3) 找盒內飛船核心
        BlockPos core = null;
        int coreCount = 0;
        for (BlockPos p : BlockPos.betweenClosed(boxMin, boxMax)) {
            if (level.getBlockState(p).getBlock() instanceof ShipCoreBlock) {
                coreCount++;
                core = p.immutable();
            }
        }
        if (coreCount == 0) { setResult(0, 0, STATUS_NO_CORE, boxW, height, boxD); return; }
        if (coreCount > 1)  { setResult(0, coreCount, STATUS_MULTI_CORE, boxW, height, boxD); return; }

        // 4) 在盒內組裝
        ShipContraption ship = new ShipContraption();
        boolean ok = ship.assemble(level, core, boxMin, boxMax);
        setResult(ok ? ship.size() : 0, 1, ok ? STATUS_OK : STATUS_FAILED, boxW, height, boxD);
    }

    private void setNoBox(int status) {
        boxMin = null; boxMax = null;
        setResult(0, 0, status, 0, 0, 0);
    }

    private void setResult(int count, int cores, int status, int boxW, int boxH, int boxD) {
        data.set(DATA_COUNT, count);
        data.set(DATA_CORES, cores);
        data.set(DATA_STATUS, status);
        data.set(DATA_BOX_W, boxW);
        data.set(DATA_BOX_H, boxH);
        data.set(DATA_BOX_D, boxD);
        setChanged();
    }

    public ContainerData getData() { return data; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.ship_assembly_pad");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ShipAssemblyPadMenu(id, inv, this);
    }
}
