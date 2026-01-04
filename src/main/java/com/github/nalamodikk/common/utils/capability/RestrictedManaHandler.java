package com.github.nalamodikk.common.utils.capability;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;

/**
 * 受限制的魔力處理器 - 根據 IO 配置限制功能
 */
public class RestrictedManaHandler implements IUnifiedManaHandler {
    private final IUnifiedManaHandler delegate;
    private final boolean canReceive;
    private final boolean canExtract;

    public RestrictedManaHandler(IUnifiedManaHandler delegate, boolean canReceive, boolean canExtract) {
        this.delegate = delegate;
        this.canReceive = canReceive;
        this.canExtract = canExtract;
    }

    @Override
    public boolean canReceive() {
        return canReceive && delegate.canReceive();
    }

    @Override
    public boolean canExtract() {
        return canExtract && delegate.canExtract();
    }

    @Override
    public int receiveMana(int maxReceive, com.github.nalamodikk.common.capability.mana.ManaAction action) {
        return canReceive ? delegate.receiveMana(maxReceive, action) : 0;
    }

    @Override
    public int extractMana(int maxExtract, com.github.nalamodikk.common.capability.mana.ManaAction action) {
        return canExtract ? delegate.extractMana(maxExtract, action) : 0;
    }

    // === 委託所有其他方法 ===

    @Override
    public int getManaStored() {
        return delegate.getManaStored();
    }

    @Override
    public void addMana(int amount) {
        if (canReceive) delegate.addMana(amount);
    }

    @Override
    public void consumeMana(int amount) {
        if (canExtract) delegate.consumeMana(amount);
    }

    @Override
    public void setMana(int amount) {
        delegate.setMana(amount);
    }

    @Override
    public void onChanged() {
        delegate.onChanged();
    }

    @Override
    public int getMaxManaStored() {
        return delegate.getMaxManaStored();
    }

    @Override
    public int getManaContainerCount() {
        return delegate.getManaContainerCount();
    }

    @Override
    public int getManaStored(int container) {
        return delegate.getManaStored(container);
    }

    @Override
    public void setMana(int container, int mana) {
        delegate.setMana(container, mana);
    }

    @Override
    public int getMaxManaStored(int container) {
        return delegate.getMaxManaStored(container);
    }

    @Override
    public int getNeededMana(int container) {
        return delegate.getNeededMana(container);
    }

    @Override
    public int insertMana(int container, int amount, com.github.nalamodikk.common.capability.mana.ManaAction action) {
        return canReceive ? delegate.insertMana(container, amount, action) : amount;
    }

    @Override
    public int extractMana(int container, int amount, com.github.nalamodikk.common.capability.mana.ManaAction action) {
        return canExtract ? delegate.extractMana(container, amount, action) : 0;
    }

}