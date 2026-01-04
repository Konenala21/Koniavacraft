package com.github.nalamodikk.common.block.blockentity.mana_generator.sync;

import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * 管理 ManaGeneratorBlockEntity 的同步欄位。
 * 簡化版：使用 MachineSyncManager 自動管理 index 和類型。
 */
public class ManaGeneratorSyncHelper implements ISyncHelper {
    
    private final MachineSyncManager syncManager;

    // 本地緩存變數 (Client/Server 雙向綁定)
    private int mana;
    private int energy;
    private int mode;
    private int burnTime;
    private int currentBurnTime;
    private boolean isWorking;
    private boolean isPaused;
    private boolean hasDiagnosticDisplay;
    private int manaRate;
    private int energyRate;

    public ManaGeneratorSyncHelper() {
        this.syncManager = new MachineSyncManager();

        // 註冊追蹤
        syncManager.trackInt(() -> mana, v -> mana = v);
        syncManager.trackInt(() -> energy, v -> energy = v);
        syncManager.trackInt(() -> mode, v -> mode = v);
        syncManager.trackInt(() -> burnTime, v -> burnTime = v);
        syncManager.trackInt(() -> currentBurnTime, v -> currentBurnTime = v);
        syncManager.trackBoolean(() -> isWorking, v -> isWorking = v);
        syncManager.trackBoolean(() -> isPaused, v -> isPaused = v);
        syncManager.trackBoolean(() -> hasDiagnosticDisplay, v -> hasDiagnosticDisplay = v);
        syncManager.trackInt(() -> manaRate, v -> manaRate = v);
        syncManager.trackInt(() -> energyRate, v -> energyRate = v);
    }

    public void syncFrom(ManaGeneratorBlockEntity be) {
        this.mana = be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0;
        this.energy = be.getEnergyStorage() != null ? be.getEnergyStorage().getEnergyStored() : 0;
        this.mode = be.getCurrentMode();
        this.burnTime = be.getBurnTime();
        this.currentBurnTime = be.getCurrentBurnTime();
        this.isWorking = be.isWorking();
        this.isPaused = be.getFuelLogic().isPaused();
        
        this.hasDiagnosticDisplay = be.getUpgradeHandler().hasDiagnosticDisplay();
        
        Optional<ManaGenFuelRateLoader.FuelRate> currentRate = be.getCurrentFuelRate();
        this.manaRate = currentRate.map(ManaGenFuelRateLoader.FuelRate::getManaRate).orElse(0);
        this.energyRate = currentRate.map(ManaGenFuelRateLoader.FuelRate::getEnergyRate).orElse(0);
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof ManaGeneratorBlockEntity generator) {
            syncFrom(generator);
        }
    }

    @Override
    public ContainerData getContainerData() {
        return syncManager;
    }

    public void setModeIndex(int modeIndex) {
        this.mode = modeIndex;
    }

    public MachineSyncManager getRawSyncManager() {
        return syncManager;
    }

    public boolean hasDiagnosticDisplay() {
        return hasDiagnosticDisplay;
    }

    public int getManaRate() {
        return manaRate;
    }

    public int getEnergyRate() {
        return energyRate;
    }
    
    // --- 相容性方法：委派給 MachineSyncManager ---
    
    public boolean hasDirty() {
        return syncManager.isDirty();
    }
    
    public void flushSyncState(BlockEntity be) {
        syncManager.markDirty(false);
    }
}
