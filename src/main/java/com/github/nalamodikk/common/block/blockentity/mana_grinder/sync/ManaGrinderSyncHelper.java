package com.github.nalamodikk.common.block.blockentity.mana_grinder.sync;

import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderBlockEntity;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ManaGrinderSyncHelper implements ISyncHelper {
    private final MachineSyncManager syncManager;
    
    // 緩存變數
    private int mana;
    private int progress;
    private int maxProgress;

    public ManaGrinderSyncHelper() {
        this.syncManager = new MachineSyncManager();
        syncManager.trackInt(() -> mana, v -> mana = v);
        syncManager.trackInt(() -> progress, v -> progress = v);
        syncManager.trackInt(() -> maxProgress, v -> maxProgress = v);
    }

    public void syncFrom(ManaGrinderBlockEntity be) {
        int newMana = be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0;
        int newProgress = be.getProgress();
        int newMaxProgress = be.getMaxProgress();

        if (this.mana != newMana) {
            this.mana = newMana;
        }
        if (this.progress != newProgress) {
            this.progress = newProgress;
        }
        if (this.maxProgress != newMaxProgress) {
            this.maxProgress = newMaxProgress;
        }
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof ManaGrinderBlockEntity grinder) {
            syncFrom(grinder);
        }
    }

    @Override
    public ContainerData getContainerData() {
        return syncManager;
    }

    public int getMana() { return mana; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
}
