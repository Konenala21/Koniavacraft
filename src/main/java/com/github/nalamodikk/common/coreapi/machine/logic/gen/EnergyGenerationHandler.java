package com.github.nalamodikk.common.coreapi.machine.logic.gen;


import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.Supplier;

/**
 * 專責處理 NeoForge 能量產出邏輯的工具類別。
 * 每次呼叫 generate() 嘗試將固定數量的能量注入儲存。
 */
public class EnergyGenerationHandler {



    private final IEnergyStorage energyStorage;
    private final Supplier<Integer> energyRateSupplier;

    /**
     * 建立能量處理器。
     *
     * @param energyStorage 能量儲存目標（可為 null）
     */
    public EnergyGenerationHandler(IEnergyStorage energyStorage, Supplier<Integer> energyRateSupplier) {
        this.energyStorage = energyStorage;
        this.energyRateSupplier = energyRateSupplier;
    }
    /**
     * 建立能量處理器（使用預設能量產出）。
     */
    public EnergyGenerationHandler(IEnergyStorage energyStorage, int energyPerTick) {
        this(energyStorage, () -> energyPerTick); // 包成 supplier 傳入
    }



    /**
     * 嘗試生成能量，若儲存空間足夠則注入。
     *
     * @return 是否成功注入能量（代表仍可繼續運作）
     */
    public boolean generate() {
        if (energyStorage == null || energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) {
            return false;
        }

        int energy = energyRateSupplier.get(); // ✅ 動態獲得當前燃料的能量值
        int accepted = energyStorage.receiveEnergy(energy, false);
        return accepted > 0;
    }
}
