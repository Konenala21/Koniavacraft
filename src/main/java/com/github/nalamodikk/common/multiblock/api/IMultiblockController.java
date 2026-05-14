package com.github.nalamodikk.common.multiblock.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

public interface IMultiblockController {
    boolean isFormed();
    MultiblockPattern getPattern();
    void onStructureFormed();
    void onStructureInvalid();
    List<IMultiblockPart> getActiveParts();
    BlockPos getControllerPos();
    Level getControllerLevel();
}
