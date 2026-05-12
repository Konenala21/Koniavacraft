package com.github.nalamodikk.common.block.blockentity.mana_generator.logic;

import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader.FuelRate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Optional;

public class ManaFuelHandler {
    private final ManaGeneratorStateManager  stateManager;
    private ManaGeneratorUpgradeHandler upgradeHandler;
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaFuelHandler.class);

    private final ItemStackHandler fuelHandler;
    private ResourceLocation currentFuelId;
    private int burnTime;
    private int currentBurnTime;
    private int failedFuelCooldown;
    private boolean isPaused;
    private boolean needsFuel = true;
    private boolean recoveryAttempted = false;
    private int pauseLogCounter = 0;
    private int resumeLogCounter = 0;
    private static final int LOG_EVERY = 5;


    public ManaFuelHandler( ItemStackHandler fuelHandler,ManaGeneratorStateManager stateManager) {
        this.fuelHandler = fuelHandler;
        this.stateManager = stateManager;
    }

    /**
     */
    public void setUpgradeHandler(ManaGeneratorUpgradeHandler upgradeHandler) {
        this.upgradeHandler = upgradeHandler;
    }


    public boolean hasAttemptedRecovery() {
        return recoveryAttempted;
    }

    public void markRecoveryAttempted() {
        this.recoveryAttempted = true;
    }

    public void resetRecoveryFlag() {
        this.recoveryAttempted = false;
    }


    public void tickCooldown() {
        if (failedFuelCooldown > 0) {
            failedFuelCooldown--;
            if (failedFuelCooldown == 0) {
                markNeedsFuel();
            }
        }
    }

    public boolean tryConsumeFuel() {
        if (!needsFuel || burnTime > 0 || failedFuelCooldown > 0) {
            return false;
        }

        ItemStack fuel = fuelHandler.getStackInSlot(0);
        if (fuel.isEmpty() || fuel.getItem() == null) return false;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(fuel.getItem());
        FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(id);

        if (rate == null || rate.getBurnTime() <= 0) {
            failedFuelCooldown = 20; // cooldown 20 tick ?岫
            needsFuel = false;
            return false;
        }

        switch (stateManager.getCurrentMode()) {
            case MANA -> {
                if (rate.getManaRate() <= 0) {
                    failedFuelCooldown = 20;
                    needsFuel = false;
                    return false;
                }
            }
            case ENERGY -> {
                if (rate.getEnergyRate() <= 0) {
                    failedFuelCooldown = 20;
                    needsFuel = false;
                    return false;
                }
            }
        }

        currentFuelId = id;
        int baseBurnTime = rate.getBurnTime();
        currentBurnTime = upgradeHandler != null ? upgradeHandler.getModifiedBurnTime(baseBurnTime) : baseBurnTime;
        burnTime = currentBurnTime;
        fuelHandler.extractItem(0, 1, false);
        needsFuel = false;
        recoveryAttempted = false;

        return true;


    }

    public void markNeedsFuel() {
        this.needsFuel = true;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getCurrentBurnTime() {
        return currentBurnTime;
    }

    public ResourceLocation getCurrentFuelId() {
        return currentFuelId;
    }

    public boolean isCoolingDown() {
        return failedFuelCooldown > 0;
    }


    public void setCooldown() {
        this.failedFuelCooldown = 20;
    }



    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }


    public void setCurrentFuelId(@Nullable ResourceLocation id) {
        this.currentFuelId = id;
    }


    public void resetBurnTime() {
        this.burnTime = 0;
        this.currentBurnTime = 0;
    }

    public void tickBurn(boolean allowBurn) {
        if (!allowBurn || isPaused) return;

        if (burnTime > 0) {
            burnTime--;
            if (burnTime == 0) {
                currentBurnTime = 0;
                markNeedsFuel();
            }
        }
    }


    public void pauseBurn() {
        if (!isPaused) {
            isPaused = true;
            failedFuelCooldown = Math.max(failedFuelCooldown, 20);
            if (++pauseLogCounter >= LOG_EVERY) {
                LOGGER.debug("pauseBurn(): machine paused, cooldown = {}", failedFuelCooldown);
                pauseLogCounter = 0;
            }
        }
    }

    public void resumeBurn() {
        if (isPaused) {
            this.isPaused = false;
            if (++resumeLogCounter >= LOG_EVERY) {
                LOGGER.debug("resumeBurn(): machine resumed from pause");
                resumeLogCounter = 0;
            }
        }
    }

    public boolean isPaused() {
        return isPaused;
    }
    public float getFuelProgress() {
        if (currentBurnTime == 0) return 0f;
        return burnTime / (float) currentBurnTime;
    }


    public boolean isBurning() {
        return burnTime > 0;
    }

    public Optional<ManaGenFuelRateLoader.FuelRate> getCurrentFuelRate() {
        if (currentFuelId == null) return Optional.empty();

        ManaGenFuelRateLoader.FuelRate baseRate = ManaGenFuelRateLoader.getFuelRateForItem(currentFuelId);
        if (baseRate == null) return Optional.empty();

        switch (stateManager.getCurrentMode()) {
            case MANA -> {
                if (baseRate.getManaRate() <= 0) return Optional.empty();
            }
            case ENERGY -> {
                if (baseRate.getEnergyRate() <= 0) return Optional.empty();
            }
        }

        if (upgradeHandler != null) {
            int modifiedMana = upgradeHandler.getModifiedOutput(baseRate.getManaRate());
            int modifiedEnergy = upgradeHandler.getModifiedOutput(baseRate.getEnergyRate());

            return Optional.of(new ManaGenFuelRateLoader.FuelRate(
                modifiedMana,
                currentBurnTime > 0 ? currentBurnTime : baseRate.getBurnTime(),
                modifiedEnergy,
                baseRate.getIntervalTick()
            ));
        }

        return Optional.of(baseRate);
    }

    public boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ManaGenFuelRateLoader.FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(itemId);

        if (rate == null || rate.getBurnTime() <= 0) {
            return false;
        }

        return switch (stateManager.getCurrentMode()) {
            case MANA -> rate.getManaRate() > 0;
            case ENERGY -> rate.getEnergyRate() > 0;
        };
    }

    public boolean canAcceptFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ItemStack currentFuel = fuelHandler.getStackInSlot(0);
        if (!currentFuel.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(currentFuel, stack)) {
                return false;
            }
            if (currentFuel.getCount() >= currentFuel.getMaxStackSize()) {
                return false;
            }
        }

        return isValidFuel(stack);
    }

    // Setters for NBT loading
    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public void setCurrentBurnTime(int currentBurnTime) {
        this.currentBurnTime = currentBurnTime;
    }
}
