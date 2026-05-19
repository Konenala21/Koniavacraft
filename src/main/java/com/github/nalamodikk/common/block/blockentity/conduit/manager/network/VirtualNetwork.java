package com.github.nalamodikk.common.block.blockentity.conduit.manager.network;

import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VirtualNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Shared pool — capacity = sum of all connected conduit tier capacities
    private final ManaStorage sharedManaPool = new ManaStorage(0);

    private final Set<BlockPos> connectedConduits = new HashSet<>();
    private final Map<BlockPos, ArcaneConduitBlockEntity> conduitMap = new HashMap<>();
    private final Map<BlockPos, Integer> conduitCapacities = new HashMap<>();

    public VirtualNetwork() {
        LOGGER.debug("Created virtual network");
    }

    public void setTotalManaStored(int amount) {
        sharedManaPool.setMana(Math.max(0, Math.min(amount, sharedManaPool.getMaxManaStored())));
        LOGGER.debug("Virtual network mana set to: {}", sharedManaPool.getManaStored());
    }

    public int getMaxManaStored() {
        return sharedManaPool.getMaxManaStored();
    }

    public Set<BlockPos> getConnectedConduits() {
        return new HashSet<>(connectedConduits);
    }

    public void logNetworkInfo() {
        LOGGER.debug("Virtual Network — Mana: {}/{}, Conduits: {}",
                sharedManaPool.getManaStored(),
                sharedManaPool.getMaxManaStored(),
                connectedConduits.size());
    }

    /**
     * Add a conduit to the network.
     * Scales the shared pool capacity up by the conduit's tier capacity,
     * then merges any mana the conduit was holding.
     */
    public void addConduit(ArcaneConduitBlockEntity conduit) {
        BlockPos pos = conduit.getBlockPos();
        if (!connectedConduits.add(pos)) return; // 已在網路中，防止容量重複疊加
        conduitMap.put(pos, conduit);

        int tierCapacity = conduit.getTier().getBufferCapacity();
        conduitCapacities.put(pos, tierCapacity);
        sharedManaPool.setCapacity(sharedManaPool.getMaxManaStored() + tierCapacity);

        int conduitMana = conduit.getBufferManaStored();
        if (conduitMana > 0) {
            sharedManaPool.receiveMana(conduitMana, ManaAction.EXECUTE);
            conduit.setBufferMana(0);
        }

        LOGGER.debug("Added conduit {} (tier cap: {}). Network: {}/{}",
                pos, tierCapacity, sharedManaPool.getManaStored(), sharedManaPool.getMaxManaStored());
    }

    /**
     * Remove a conduit from the network.
     * Shrinks the pool capacity by that conduit's tier contribution.
     * Excess mana (above the new cap) is returned to the leaving conduit's buffer
     * so players don't silently lose mana when reorganising their network.
     */
    public void removeConduit(BlockPos pos) {
        if (!connectedConduits.remove(pos)) return;

        ArcaneConduitBlockEntity leaving = conduitMap.remove(pos);
        Integer tierCap = conduitCapacities.remove(pos);
        if (tierCap != null) {
            int newMax = Math.max(0, sharedManaPool.getMaxManaStored() - tierCap);
            int excess = Math.max(0, sharedManaPool.getManaStored() - newMax);
            if (excess > 0 && leaving != null) {
                sharedManaPool.extractMana(excess, ManaAction.EXECUTE);
                leaving.setBufferMana(excess);
            }
            sharedManaPool.setCapacity(newMax);
        }

        LOGGER.debug("Removed conduit {} from network. Remaining: {}, cap: {}",
                pos, connectedConduits.size(), sharedManaPool.getMaxManaStored());
    }

    /**
     * Call this when a conduit upgrades its tier while already in the network.
     * Adjusts the pool capacity by the delta between old and new tier capacities.
     */
    public void updateConduitCapacity(BlockPos pos, int oldCapacity, int newCapacity) {
        if (!conduitCapacities.containsKey(pos)) return;
        conduitCapacities.put(pos, newCapacity);
        int newTotal = sharedManaPool.getMaxManaStored() - oldCapacity + newCapacity;
        sharedManaPool.setCapacity(Math.max(0, newTotal));
        LOGGER.debug("Conduit {} tier upgraded: cap {} -> {}. Network max: {}",
                pos, oldCapacity, newCapacity, sharedManaPool.getMaxManaStored());
    }

    public int extractManaFromNetwork(int maxExtract, ManaAction action) {
        return sharedManaPool.extractMana(maxExtract, action);
    }

    public int receiveManaToNetwork(int maxReceive, ManaAction action) {
        return sharedManaPool.receiveMana(maxReceive, action);
    }

    public int getTotalManaStored() {
        return sharedManaPool.getManaStored();
    }

    public int getTotalManaCapacity() {
        return sharedManaPool.getMaxManaStored();
    }

    public boolean contains(BlockPos pos) {
        return connectedConduits.contains(pos);
    }

    public int getNetworkSize() {
        return connectedConduits.size();
    }
}
