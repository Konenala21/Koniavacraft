package com.github.nalamodikk.common.block.blockentity.mana_generator.logic;

import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class ManaGeneratorTicker {
    private final ManaGeneratorBlockEntity machine;
    public static final Logger LOGGER = LogUtils.getLogger();
    private int tickCounter = 0;

    // Debounce for ACTIVE block state changes to prevent rapid light toggling.
    // The desired state must be stable for MIN_ACTIVE_HOLD_TICKS before the
    // block state (and thus light level) is actually updated.
    private static final int MIN_ACTIVE_HOLD_TICKS = 10;
    private boolean pendingActive = false;
    private int activeHoldTicks = 0;

    public ManaGeneratorTicker(ManaGeneratorBlockEntity machine) {
        this.machine = machine;
    }

    public void tick() {
        tickCounter++;

        ManaFuelHandler fuelHandler = machine.getFuelLogic();
        boolean changed = false;

        fuelHandler.tickCooldown();

        if (!fuelHandler.isBurning() && !fuelHandler.isCoolingDown() && fuelHandler.isPaused()) {
            if (!fuelHandler.hasAttemptedRecovery()) {
                LOGGER.debug("[FuelSafety] Recovering from paused-but-not-cooling state");
                fuelHandler.markNeedsFuel();
                fuelHandler.setPaused(false);
                fuelHandler.markRecoveryAttempted();
            }
        }

        if (!fuelHandler.isBurning()) {
            if (tickCounter % 4 == 0 && !fuelHandler.tryConsumeFuel()) {
                machine.getStateManager().setWorking(false);
            }
        }

        boolean success = false;

        if (fuelHandler.isBurning()) {
            success = switch (machine.getStateManager().getCurrentMode()) {
                case MANA -> machine.getManaGenHandler().generate();
                case ENERGY -> machine.getEnergyGenHandler().generate();
            };
        }

        // Determine desired working state from this tick's result.
        // setWorking() updates internal state immediately (for GUI sync etc.),
        // but the block-state light update is deferred via debounce below.
        final boolean wantsActive;
        if (!success) {
            fuelHandler.pauseBurn();
            machine.getStateManager().setWorking(false);
            wantsActive = false;
        } else {
            fuelHandler.resumeBurn();
            fuelHandler.tickBurn(true);
            boolean stillBurning = fuelHandler.isBurning();
            machine.getStateManager().setWorking(stillBurning);
            wantsActive = stillBurning;
        }

        // Debounced ACTIVE block-state update.
        // Prevents rapid light toggling when output buffer oscillates around full,
        // which would otherwise call level.setBlock() every few ticks and cause
        // chunk re-lighting recalculations and visible flicker.
        if (wantsActive != pendingActive) {
            // Desired state changed — reset hold counter and wait again.
            pendingActive = wantsActive;
            activeHoldTicks = 0;
        } else if (activeHoldTicks < MIN_ACTIVE_HOLD_TICKS) {
            activeHoldTicks++;
        } else {
            // State stable for MIN_ACTIVE_HOLD_TICKS — commit to block state.
            machine.updateBlockActiveState(pendingActive);
            changed = true;
        }

        // 輸出邏輯獨立於燃燒狀態——確保 buffer 中的殘餘能量/魔力在停止燃燒後仍能推送出去
        if (machine.getLevel() instanceof ServerLevel serverLevel) {
            if (machine.getOutputThrottle().shouldTryOutput()) {
                boolean outputSuccess = OutputHandler.tryOutput(
                        serverLevel,
                        machine.getBlockPos(),
                        machine.getManaStorage(),
                        machine.getEnergyStorage(),
                        machine.getIOMap(),
                        machine.getManaOutputCaches(),
                        machine.getEnergyOutputCaches()
                );
                machine.getOutputThrottle().recordOutputResult(outputSuccess);
            }
        }

        if (changed) {
            machine.sync();
        }
    }



}
