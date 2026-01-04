package com.github.nalamodikk.common.compat.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class ModNeoNalaEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {
    private static final int DECIMAL_DIGITS = 4;
    private BigDecimal energy;
    private final BigDecimal capacity;

    public ModNeoNalaEnergyStorage(BigInteger capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.capacity = new BigDecimal(capacity).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        BigDecimal amount = BigDecimal.valueOf(maxReceive);
        BigDecimal accepted = amount.min(capacity.subtract(energy));
        if (!simulate) energy = energy.add(accepted);
        return accepted.intValue();
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        BigDecimal amount = BigDecimal.valueOf(maxExtract);
        BigDecimal extracted = energy.min(amount);
        if (!simulate) energy = energy.subtract(extracted);
        return extracted.intValue();
    }

    @Override public int getEnergyStored() { return energy.intValue(); }
    @Override public int getMaxEnergyStored() { return capacity.intValue(); }
    @Override public boolean canExtract() { return energy.compareTo(BigDecimal.ZERO) > 0; }
    @Override public boolean canReceive() { return energy.compareTo(capacity) < 0; }

    public BigDecimal getRawEnergyStored() {
        return energy;
    }

    public BigDecimal getRawMaxCapacity() {
        return capacity;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Energy", energy.toPlainString());
        tag.putString("Capacity", capacity.toPlainString());
        return tag;
    }

    // 🆕 添加能量設定方法（用於 NBT 載入）
    public void setEnergyStored(BigInteger energyAmount) {
        this.energy = new BigDecimal(energyAmount).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        // 確保不超過容量
        if (this.energy.compareTo(capacity) > 0) {
            this.energy = capacity;
        }
    }

    public void deserializeNBT(CompoundTag tag) {
        this.energy = new BigDecimal(tag.getString("Energy")).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        // 若你有計畫支援容量動態變化，再取消以下註解
        // this.capacity = new BigDecimal(tag.getString("Capacity")).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
    }

    // 為了支援新版 HolderLookup.Provider，但實際上你不需要它，這是為了 NeoForge 相容
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return serializeNBT(); // 呼叫無 provider 的版本
    }


    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt instanceof CompoundTag compoundTag)
            deserializeNBT(compoundTag);
    }


}
