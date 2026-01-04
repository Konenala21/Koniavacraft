package com.github.nalamodikk.common.coreapi.machine.logic.gen;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractGenerationHandler<T> {
    protected final Supplier<Optional<T>> rateSupplier;
    protected final int tickInterval;
    protected int tickCounter = 0;
    protected int lastHash = -1;
    protected T cachedRate = null;

    public AbstractGenerationHandler(Supplier<Optional<T>> rateSupplier, int tickInterval) {
        this.rateSupplier = rateSupplier;
        this.tickInterval = tickInterval;
    }

    protected int getIntervalTick(T rate) {
        return tickInterval; // 預設回傳固定值，如需動態覆寫
    }


    public boolean tickGenerate() {
        Optional<T> optional = rateSupplier.get();
        if (optional.isEmpty() || !canAccept()) return false;

        T rate = optional.get();
        int hash = hash(rate);

        if (hash != lastHash) {
            lastHash = hash;
            cachedRate = rate;
            tickCounter = 0;
        }

        if (cachedRate == null) return false;

        tickCounter++;
        if (tickCounter >= getIntervalTick(cachedRate)) {
            tickCounter = 0;
            return doGenerate(cachedRate);
        }
        return false;
    }

    protected abstract int hash(T rate);

    protected abstract boolean canAccept();

    protected abstract boolean doGenerate(T rate);
}
