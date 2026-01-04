package com.github.nalamodikk.common.coreapi.machine.logic.gen;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.capability.mana.ManaAction;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FuelManaGenHelper {
    private final IUnifiedManaHandler manaStorage;
    private final Supplier<Optional<ManaGenFuelRateLoader.FuelRate>> fuelRateSupplier;
    private int tickCounter = 0;
    private final Consumer<Integer> onGenerate;

    public FuelManaGenHelper(IUnifiedManaHandler manaStorage, Supplier<Optional<ManaGenFuelRateLoader.FuelRate>> fuelRateSupplier, Consumer<Integer> onGenerate) {
        this.manaStorage = manaStorage;
        this.fuelRateSupplier = fuelRateSupplier;
        this.onGenerate = onGenerate;

    }

    public void resetTickCounter() {
        this.tickCounter = 0;
    }


    public boolean generate() {
        Optional<ManaGenFuelRateLoader.FuelRate> opt = fuelRateSupplier.get();
        if (opt.isEmpty() || manaStorage == null || !manaStorage.canReceive()) return false;

        ManaGenFuelRateLoader.FuelRate rate = opt.get();
        tickCounter++;

        if (tickCounter >= rate.getIntervalTick()) {
            tickCounter = 0;

            int remaining = manaStorage.insertMana(rate.getManaRate(), ManaAction.EXECUTE);
            if (remaining > 0) {
                // 呼叫外部回傳「剩下來的」魔力量（丟給 OutputHandler）
                onGenerate.accept(remaining);
            }

            return true;
        }

        return false;
    }


}

