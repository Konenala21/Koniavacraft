package com.github.nalamodikk.common.block.blockentity.conduit;

import com.github.nalamodikk.common.block.blockentity.conduit.manager.network.VirtualNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;
import java.util.Set;

/**
 * 導管的虛擬網路「成員資格」生命週期：加入鄰近網路 / 建立新網路 / 離開 / 從 NBT 還原網路魔力。
 * 實際的 {@code virtualNetwork} 參考仍存放在 {@link ArcaneConduitBlockEntity}（魔力 facade 與
 * NBT 序列化都直接讀它），本類別持有 BE reference 操作該欄位與還原暫存資料。
 */
final class ConduitNetworkMembership {

    private final ArcaneConduitBlockEntity conduit;

    private CompoundTag tempNetworkData = null;
    private boolean needsNetworkRestore = false;

    ConduitNetworkMembership(ArcaneConduitBlockEntity conduit) {
        this.conduit = conduit;
    }

    /** 嘗試加入鄰近導管的網路；找不到就建立新網路。 */
    void tryJoin() {
        if (conduit.virtualNetwork != null) return; // 已經在網路中

        Level level = conduit.getLevel();
        BlockPos worldPosition = conduit.getBlockPos();
        // 搜尋鄰近的導管
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE instanceof ArcaneConduitBlockEntity neighborConduit) {
                VirtualNetwork neighborNetwork = neighborConduit.getVirtualNetwork();

                if (neighborNetwork != null) {
                    // 加入鄰居的網路
                    join(neighborNetwork);
                    return;
                }
            }
        }

        // 沒有鄰近網路，創建新的
        createNew();
    }

    private void createNew() {
        conduit.virtualNetwork = new VirtualNetwork();
        conduit.virtualNetwork.addConduit(conduit);
    }

    private void join(VirtualNetwork network) {
        conduit.virtualNetwork = network;
        network.addConduit(conduit);
    }

    void leave() {
        if (conduit.virtualNetwork != null) {
            conduit.virtualNetwork.removeConduit(conduit.getBlockPos());
            conduit.virtualNetwork = null;
        }
    }

    // 判斷是否為網路主導管（位置最小的導管）
    boolean isNetworkMaster() {
        if (conduit.virtualNetwork == null) return false;

        Set<BlockPos> conduits = conduit.virtualNetwork.getConnectedConduits();
        if (conduits.isEmpty()) return true;

        // 找到位置最小的導管作為主導管
        BlockPos minPos = conduits.stream()
                .min(Comparator.comparingLong(BlockPos::asLong))
                .orElse(conduit.getBlockPos());

        return conduit.getBlockPos().equals(minPos);
    }

    /** loadAdditional 時把 NBT 內的網路魔力暫存起來，待 onLoad 網路建立後再還原。 */
    void queueRestoreFromNBT(CompoundTag tag) {
        if (!tag.contains("VirtualNetworkMana")) return;
        tempNetworkData = new CompoundTag();
        tempNetworkData.putInt("Mana", tag.getInt("VirtualNetworkMana"));
        tempNetworkData.putInt("MaxMana", tag.getInt("VirtualNetworkMaxMana"));

        if (tag.contains("VirtualNetworkConduits")) {
            tempNetworkData.put("Conduits", tag.get("VirtualNetworkConduits"));
        }

        needsNetworkRestore = true;
    }

    /** onLoad 時若有暫存的還原資料且網路已建立，套用之。 */
    void restoreIfNeeded() {
        if (!needsNetworkRestore || tempNetworkData == null || conduit.virtualNetwork == null) return;

        try {
            int savedMana = tempNetworkData.getInt("Mana");

            // 取最大值策略：若存檔魔力大於目前網路魔力則更新。
            // 解決分批載入時容量尚未完全展開導致首次恢復被截斷的問題。
            if (savedMana > conduit.virtualNetwork.getTotalManaStored()) {
                conduit.virtualNetwork.setTotalManaStored(savedMana);
            }

            needsNetworkRestore = false;
            tempNetworkData = null;

        } catch (Exception e) {
            ArcaneConduitBlockEntity.LOGGER.error("Failed to restore virtual network data: {}", e.getMessage());
            needsNetworkRestore = false;
            tempNetworkData = null;
        }
    }
}
