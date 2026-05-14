package com.github.nalamodikk.common.multiblock.api;

import net.minecraft.core.BlockPos;

public interface IMultiblockPart {
    void addedToController(IMultiblockController controller);
    void removedFromController();
    boolean isPartFormed();
    BlockPos getPartPos();
}
