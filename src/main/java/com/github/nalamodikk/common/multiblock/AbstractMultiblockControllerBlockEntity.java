package com.github.nalamodikk.common.multiblock;

import com.github.nalamodikk.common.multiblock.api.IMultiblockController;
import com.github.nalamodikk.common.multiblock.api.IMultiblockPart;
import com.github.nalamodikk.common.multiblock.api.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有多方塊 Controller BlockEntity 的抽象基類。
 *
 * <p>目前驗證策略：每 {@code CHECK_INTERVAL} tick 同步掃描，對小型結構（&lt;10 方塊）開銷極低。
 *
 * <p>未來可擴充方向：
 * <ul>
 *   <li><b>跨 tick 異步驗證</b>（參考 GT MultiblockWorldSavedData）：若未來有大型結構
 *       （幾十個方塊），可建立全域驗證佇列，每 tick 只取幾台機器驗證，避免集中卡頓。</li>
 * </ul>
 */
public abstract class AbstractMultiblockControllerBlockEntity extends BlockEntity
        implements IMultiblockController {

    private boolean isFormed = false;
    private final List<IMultiblockPart> activeParts = new ArrayList<>();

    protected AbstractMultiblockControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ── 結構驗證 ─────────────────────────────────────────────────────────────

    public void checkStructure() {
        if (level == null || level.isClientSide()) return;

        boolean valid = getPattern().checkAt(level, worldPosition);

        if (valid && !isFormed) {
            isFormed = true;
            collectParts();
            onStructureFormed();
            setChanged();
        } else if (!valid && isFormed) {
            isFormed = false;
            disbandParts();
            onStructureInvalid();
            setChanged();
        }
    }

    private void collectParts() {
        activeParts.clear();
        for (BlockPos partPos : getPattern().getPartPositions(worldPosition)) {
            if (level != null && level.getBlockEntity(partPos) instanceof IMultiblockPart part) {
                activeParts.add(part);
                part.addedToController(this);
            }
        }
    }

    private void disbandParts() {
        for (IMultiblockPart part : activeParts) {
            part.removedFromController();
        }
        activeParts.clear();
    }

    // ── IMultiblockController ─────────────────────────────────────────────────

    @Override
    public boolean isFormed() {
        return isFormed;
    }

    @Override
    public List<IMultiblockPart> getActiveParts() {
        return activeParts;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public Level getControllerLevel() {
        return level;
    }

    // ── 生命週期鉤子（子類覆寫）──────────────────────────────────────────────

    @Override
    public void onStructureFormed() {}

    @Override
    public void onStructureInvalid() {}

    // ── onLoad / setRemoved ──────────────────────────────────────────────────

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
        }
    }

    @Override
    public void setRemoved() {
        if (isFormed) {
            disbandParts();
        }
        super.setRemoved();
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsFormed", isFormed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isFormed = tag.getBoolean("IsFormed");
    }
}
