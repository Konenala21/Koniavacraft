package com.github.nalamodikk.common.block.blockentity.mana_plate_press.sync;

import com.github.nalamodikk.common.block.blockentity.mana_plate_press.ManaPlatePressBlockEntity;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ManaPlatePressSync implements ISyncHelper {

    private final MachineSyncManager syncManager;

    private int currentMana;
    private int maxMana;
    private int progress;
    private int maxProgress;
    private boolean isWorking;

    public ManaPlatePressSync() {
        this.syncManager = new MachineSyncManager();
        syncManager.trackInt(() -> currentMana, v -> currentMana = v);
        syncManager.trackInt(() -> maxMana, v -> maxMana = v);
        syncManager.trackInt(() -> progress, v -> progress = v);
        syncManager.trackInt(() -> maxProgress, v -> maxProgress = v);
        syncManager.trackBoolean(() -> isWorking, v -> isWorking = v);
    }

    public void syncFrom(ManaPlatePressBlockEntity be) {
        this.currentMana = be.getCurrentMana();
        this.maxMana = be.getMaxMana();
        this.progress = be.getPressingProgress();
        this.maxProgress = be.getMaxPressingTime();
        this.isWorking = be.isWorking();
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof ManaPlatePressBlockEntity press) syncFrom(press);
    }

    @Override
    public ContainerData getContainerData() { return syncManager; }

    public int getCurrentMana() { return currentMana; }
    public int getMaxMana() { return maxMana; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public boolean isWorking() { return isWorking; }
}
