package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

    public static final int MAX_FOOTPRINT = 48;   // footprint 單邊上限
    public static final int MAX_HEIGHT = 48;       // 高度上限
    public static final int MAX_BASE_BLOCKS = 2500;// 底座 flood-fill 上限（覆蓋 48×48）
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
    public static final int STATUS_LAUNCHED = 7;    // 已組裝出航（方塊變成飛船實體）

    private final ContainerData data = new SimpleContainerData(6);

    // 最近一次掃描算出的盒（給 assemble 用；不持久化，掃描時重算）
    @Nullable private BlockPos boxMin;
    @Nullable private BlockPos boxMax;

    public ShipAssemblyPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIP_ASSEMBLY_PAD.get(), pos, state);
    }

    /**
     * 讀發射台結構算建造盒：底座 footprint + 組裝架高度。成功時設好 boxMin/boxMax 並回填盒尺寸，
     * 回傳 true；失敗（沒底座/太大）時設好狀態並回傳 false。
     */
    private boolean computeBox() {
        int baseY = worldPosition.getY();

        // 1) flood-fill 與組裝台同層、相連的底座（水平 4 鄰），取外接矩形
        Set<BlockPos> base = new HashSet<>();
        Queue<BlockPos> q = new ArrayDeque<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = worldPosition.relative(d);
            if (level.getBlockState(n).getBlock() instanceof ShipAssemblyBaseBlock) q.add(n);
        }
        if (q.isEmpty()) { setNoBox(STATUS_NO_BASE); return false; }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (!base.add(p)) continue;
            if (base.size() > MAX_BASE_BLOCKS) { setNoBox(STATUS_TOO_BIG); return false; }
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
            return false;
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
        data.set(DATA_BOX_W, maxX - minX + 1);
        data.set(DATA_BOX_H, height);
        data.set(DATA_BOX_D, maxZ - minZ + 1);
        return true;
    }

    /** 盒內飛船核心：回傳唯一核心位置，0 或多個回傳 null 並把數量寫進 coreCountOut[0]。 */
    @Nullable
    private BlockPos findSingleCore(int[] coreCountOut) {
        BlockPos core = null;
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(boxMin, boxMax)) {
            if (level.getBlockState(p).getBlock() instanceof ShipCoreBlock) {
                count++;
                core = p.immutable();
            }
        }
        coreCountOut[0] = count;
        return count == 1 ? core : null;
    }

    /** 掃描：讀結構算盒 → 找核心 → 試組裝，結果寫進 ContainerData（不改世界）。 */
    public void scan() {
        if (level == null || level.isClientSide) return;
        if (!computeBox()) return;

        int[] cc = new int[1];
        BlockPos core = findSingleCore(cc);
        if (cc[0] == 0) { data.set(DATA_COUNT, 0); data.set(DATA_CORES, 0); setStatus(STATUS_NO_CORE); return; }
        if (cc[0] > 1)  { data.set(DATA_COUNT, 0); data.set(DATA_CORES, cc[0]); setStatus(STATUS_MULTI_CORE); return; }

        ShipContraption ship = new ShipContraption();
        boolean ok = ship.assemble(level, core, boxMin, boxMax);
        data.set(DATA_COUNT, ok ? ship.size() : 0);
        data.set(DATA_CORES, 1);
        setStatus(ok ? STATUS_OK : STATUS_FAILED);
    }

    /** 組裝出航：算盒 → 組裝 → 移除世界方塊 → 生成 ShipEntity（server 端）。 */
    public void assembleShip() {
        if (!(level instanceof ServerLevel server)) return;
        if (!computeBox()) return;

        int[] cc = new int[1];
        BlockPos core = findSingleCore(cc);
        if (cc[0] == 0) { data.set(DATA_CORES, 0); setStatus(STATUS_NO_CORE); return; }
        if (cc[0] > 1)  { data.set(DATA_CORES, cc[0]); setStatus(STATUS_MULTI_CORE); return; }

        ShipContraption ship = new ShipContraption();
        if (!ship.assemble(server, core, boxMin, boxMax)) {
            data.set(DATA_CORES, 1);
            setStatus(STATUS_FAILED);
            return;
        }

        ship.removeFromWorld(server);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), server);
        entity.setContraption(ship);
        // 實體原點放在船中心（hitbox 才貼合），位置依 contraption 算
        entity.placeAtShipCenter(core);
        entity.setOldPosAndRot(); // 初始化 xOld 等 = 現位，否則第一 tick 前甲板碰撞用 xOld=0 在錯框算 → 剛組裝穿過去

        // VM1：把方塊複製進影子維度的固定區域並 force-load，機器/作物在那裡真正 tick。
        // 視覺船(entity 上的 contraption)是鏡像，影子是真相（VM2 做狀態鏡射回視覺船）。
        // 重要：anchor 要在 addFreshEntity 前設好，spawn data 才帶得到給 client（VM3b 機器 GUI 靠它反查 render BE）。
        ServerLevel shadow = ShipShadowManager.shadowLevel(server.getServer());
        if (shadow != null) {
            BlockPos anchor = ShipShadowManager.get(server.getServer()).allocate(entity.getUUID());
            entity.setShadowAnchor(anchor);
            server.addFreshEntity(entity);
            ShipShadowManager.setForceLoad(shadow, anchor, ship.bounds(), true);
            ShipShadowManager.placeInShadow(shadow, anchor, ship);
        } else {
            server.addFreshEntity(entity);
            com.github.nalamodikk.KoniavacraftMod.LOGGER.warn(
                    "[ship] ship_shadow dimension NOT loaded at assembly (run runData + restart); machines won't tick");
        }

        data.set(DATA_COUNT, ship.size());
        data.set(DATA_CORES, 1);
        setStatus(STATUS_LAUNCHED);
    }

    private void setNoBox(int status) {
        boxMin = null; boxMax = null;
        data.set(DATA_BOX_W, 0); data.set(DATA_BOX_H, 0); data.set(DATA_BOX_D, 0);
        data.set(DATA_COUNT, 0); data.set(DATA_CORES, 0);
        setStatus(status);
    }

    private void setStatus(int status) {
        data.set(DATA_STATUS, status);
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
