package com.github.nalamodikk.common.sync;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ISyncHelper {
    void syncFrom(BlockEntity be);
    ContainerData getContainerData();
}
