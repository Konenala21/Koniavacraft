package com.github.nalamodikk.common.multiblock;

import com.github.nalamodikk.common.multiblock.api.IMultiblockController;
import com.github.nalamodikk.common.multiblock.api.IMultiblockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMultiblockPartBlockEntity extends BlockEntity
        implements IMultiblockPart {

    @Nullable
    private BlockPos controllerPos = null;

    protected AbstractMultiblockPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ── IMultiblockPart ───────────────────────────────────────────────────────

    @Override
    public void addedToController(IMultiblockController controller) {
        this.controllerPos = controller.getControllerPos();
        setChanged();
        onAddedToController(controller);
    }

    @Override
    public void removedFromController() {
        this.controllerPos = null;
        setChanged();
        onRemovedFromController();
    }

    @Override
    public boolean isPartFormed() {
        return controllerPos != null;
    }

    @Override
    public BlockPos getPartPos() {
        return worldPosition;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    // ── 子類可覆寫的鉤子 ──────────────────────────────────────────────────────

    protected void onAddedToController(IMultiblockController controller) {}
    protected void onRemovedFromController() {}

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ControllerPos")) {
            controllerPos = NbtUtils.readBlockPos(tag, "ControllerPos").orElse(null);
        }
    }
}
