package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 飛船的燃料/引擎/曲速子系統（server 權威 + client 算 tier）。從 ShipEntity 抽出的純結構搬移：
 * 引擎數、燃料槽/曲速核心/進料口的 local 清單、以及讀/抽燃料、tier 計算都集中在這。
 *
 * <p>state 跟著 contraption 走：組裝/載入時 {@link #recompute()} 全船掃一次；停船編輯加/挖單格時
 * {@link #onBlockChanged} 做 O(1) 增量維護（曲速結構完整性靠多方塊，不能增量 → 動到就重掃）。
 *
 * <p>燃料的實際儲存在影子維度的燃料槽/進料口 BE 裡，所以讀/抽都透過 {@code ship.getShadow()} +
 * {@code ship.getShadowAnchor()}。沒影子（client / gametest）時讀回 0。
 */
public final class ShipFuelSystem {

    // ── 速度/燃料/魔焓常數（movement 與 tier 計算共用）────────────────────────────
    /** 每引擎貢獻的每 tick 速度上限(20 b/s=1.0/tick;~50 引擎到頂 200)。 */
    public static final double SPEED_PER_ENGINE = 0.2;
    /** 一般引擎天花板 10.0/tick=200 b/s。碰撞已子步進防穿牆;這麼快只適合高空/太空。 */
    public static final double SPEED_CAP = 10.0;
    /** 移動時「每引擎」每 tick 耗魔力(滿油門);加速 ×2;隨油門縮放。引擎越多越快也越耗。 */
    public static final int FUEL_PER_ENGINE_MOVE = 12;

    /** 基礎魔力燃料的魔焓等級。 */
    public static final int ENTHALPY_BASIC_MANA = 5;
    /** 魔力引擎能燒的最高魔焓。 */
    public static final int MANA_ENGINE_ENTHALPY_CAP = 9;

    /** 每「座」完整曲速結構貢獻(15/tick=300 b/s 一座;2 座到頂 600)。比引擎每格效率高 ~4 倍,名副其實的高階科技。 */
    public static final double SPEED_PER_WARP = 15.0;
    /** 有曲速結構時的天花板 30.0/tick=600 b/s。 */
    public static final double WARP_CAP = 30.0;
    /** 每座曲速結構每 tick 從進料口抽的能量(很兇,要高密度燃料/魔力網路撐)。 */
    public static final int FUEL_PER_WARP_MOVE = 200;

    private final ShipEntity ship;

    // 引擎數=速度上限、燃料槽 local=飛行時抽魔力的對象。contraption 變動時重算。
    private int engineCount = 0;
    private final List<BlockPos> fuelTankLocals = new ArrayList<>();
    // 曲速：核心 local（掃結構用）、完整結構數、各進料口 local（抽曲速燃料）
    private final List<BlockPos> warpCoreLocals = new ArrayList<>();
    private int warpDriveCount = 0;
    private final List<BlockPos> warpIntakeLocals = new ArrayList<>();

    public ShipFuelSystem(ShipEntity ship) {
        this.ship = ship;
    }

    /** contraption 變動時重算引擎數 + 燃料槽/曲速核心 local，再掃完整曲速結構。只在組裝/載入跑一次。 */
    public void recompute() {
        engineCount = 0;
        fuelTankLocals.clear();
        warpCoreLocals.clear();
        ShipContraption contraption = ship.getContraption();
        if (contraption == null) { warpDriveCount = 0; warpIntakeLocals.clear(); return; }
        for (var e : contraption.getBlocks().entrySet()) {
            Block b = e.getValue().state().getBlock();
            if (b instanceof ManaWarpEngineBlock) warpCoreLocals.add(e.getKey().immutable());
            else if (b instanceof ManaEngineBlock) engineCount++;
            else if (b instanceof ManaFuelTankBlock) fuelTankLocals.add(e.getKey().immutable());
        }
        recomputeWarpDrives();
    }

    /** 用 warpCoreLocals 掃完整 3×4×3 沙漏結構，更新 warpDriveCount + warpIntakeLocals(曲速燃料從這些進料口抽)。 */
    private void recomputeWarpDrives() {
        warpIntakeLocals.clear();
        ShipContraption contraption = ship.getContraption();
        if (contraption == null || warpCoreLocals.isEmpty()) { warpDriveCount = 0; return; }
        var blocks = contraption.getBlocks();
        var found = WarpDriveStructure.detect(warpCoreLocals,
                lp -> { var i = blocks.get(lp); return i != null ? i.state() : null; });
        warpDriveCount = found.size();
        for (var f : found) warpIntakeLocals.add(f.intakeLocal());
    }

    /**
     * 停船編輯加/挖單格時的 O(1) 增量維護（不掃全船 → 逐個放引擎/燃料槽也不卡）。
     * 動到曲速相關方塊（核心/外殼魔力合金/進料口）才重掃完整結構（多方塊完整性不能增量）。
     */
    public void onBlockChanged(BlockPos local, Block oldBlock, Block newBlock) {
        boolean oldEngine = oldBlock instanceof ManaEngineBlock, newEngine = newBlock instanceof ManaEngineBlock;
        if (oldEngine != newEngine) engineCount += newEngine ? 1 : -1;
        boolean oldTank = oldBlock instanceof ManaFuelTankBlock, newTank = newBlock instanceof ManaFuelTankBlock;
        if (oldTank && !newTank) fuelTankLocals.remove(local);
        else if (newTank && !oldTank) fuelTankLocals.add(local.immutable());
        // 曲速核心 list 增量維護
        boolean oldWarp = oldBlock instanceof ManaWarpEngineBlock, newWarp = newBlock instanceof ManaWarpEngineBlock;
        if (oldWarp && !newWarp) warpCoreLocals.remove(local);
        else if (newWarp && !oldWarp) warpCoreLocals.add(local.immutable());
        Block alloy = ModBlocks.MANA_ALLOY_BLOCK.get();
        if (oldWarp || newWarp || oldBlock == alloy || newBlock == alloy
                || oldBlock instanceof ManaWarpInputBlock || newBlock instanceof ManaWarpInputBlock) {
            recomputeWarpDrives();
        }
    }

    /** 目前總燃料 = 影子裡各燃料槽的魔力總和。沒影子(gametest)回 0。 */
    public int currentFuel() {
        ServerLevel shadow = ship.getShadow();
        BlockPos anchor = ship.getShadowAnchor();
        if (shadow == null || anchor == null) return 0;
        int total = 0;
        for (BlockPos lp : fuelTankLocals) {
            if (shadow.getBlockEntity(anchor.offset(lp)) instanceof ManaFuelTankBlockEntity tank)
                total += tank.getManaStorage().getManaStored();
        }
        return total;
    }

    /** 從影子的燃料槽抽 amount 魔力（飛行消耗）。逐槽抽到夠為止。 */
    public void drainFuel(int amount) {
        ServerLevel shadow = ship.getShadow();
        BlockPos anchor = ship.getShadowAnchor();
        if (shadow == null || anchor == null || amount <= 0) return;
        for (BlockPos lp : fuelTankLocals) {
            if (amount <= 0) break;
            if (shadow.getBlockEntity(anchor.offset(lp)) instanceof ManaFuelTankBlockEntity tank)
                amount -= tank.getManaStorage().extractMana(amount, ManaAction.EXECUTE);
        }
    }

    /** 曲速燃料 = 完整結構各進料口的能量總和(影子)。 */
    public int currentWarpFuel() {
        ServerLevel shadow = ship.getShadow();
        BlockPos anchor = ship.getShadowAnchor();
        if (shadow == null || anchor == null) return 0;
        int total = 0;
        for (BlockPos lp : warpIntakeLocals) {
            if (shadow.getBlockEntity(anchor.offset(lp)) instanceof ManaWarpInputBlockEntity in)
                total += in.getEnergy().getManaStored();
        }
        return total;
    }

    /** 從各進料口抽 amount 曲速能量。 */
    public void drainWarpFuel(int amount) {
        ServerLevel shadow = ship.getShadow();
        BlockPos anchor = ship.getShadowAnchor();
        if (shadow == null || anchor == null || amount <= 0) return;
        for (BlockPos lp : warpIntakeLocals) {
            if (amount <= 0) break;
            if (shadow.getBlockEntity(anchor.offset(lp)) instanceof ManaWarpInputBlockEntity in)
                amount -= in.getEnergy().extractMana(amount, ManaAction.EXECUTE);
        }
    }

    /**
     * 飛船 tier = min(裝著的最高燃料魔焓(引擎燒得動的), 引擎魔焓上限)。0 = 不能飛(沒引擎或沒燃料)。
     * 用同步的 DATA_FUEL（ship.getDisplayFuel）+ engineCount(兩者 client 都有) → client HUD 也算得出來。
     */
    public int tier() {
        int engineCap = engineCount > 0 ? MANA_ENGINE_ENTHALPY_CAP : 0;
        if (engineCap == 0) return 0;
        int fuelEnthalpy = ship.getDisplayFuel() > 0 ? Math.min(ENTHALPY_BASIC_MANA, engineCap) : 0;
        return Math.min(fuelEnthalpy, engineCap);
    }

    public int engineCount() { return engineCount; }
    public int warpDriveCount() { return warpDriveCount; }       // 完整成形的曲速結構數
    public int warpCoreCount() { return warpCoreLocals.size(); } // 放下的曲速核心數(成形回饋:成形數 < 核心數 = 有蓋錯)
    public int tankCount() { return fuelTankLocals.size(); }
    public int warpIntakeCount() { return warpIntakeLocals.size(); }
}
