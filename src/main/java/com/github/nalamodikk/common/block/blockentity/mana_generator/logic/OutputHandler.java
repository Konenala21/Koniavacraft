
package com.github.nalamodikk.common.block.blockentity.mana_generator.logic;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class OutputHandler {

    private static final int MAX_MANA_OUTPUT_PER_TICK = 40;
    private static final int MAX_ENERGY_OUTPUT_PER_TICK = 1000;

    // 新增這個方法到 OutputHandler 類別中
    public static boolean tryOutput(
            ServerLevel level,
            BlockPos pos,
            ManaStorage manaStorage,
            IEnergyStorage energyStorage,
            EnumMap<Direction, IOHandlerUtils.IOType> ioMap,
            // 新增這兩個參數
            EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler, Direction>> manaCaches,
            EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches
    ) {
        List<IUnifiedManaHandler> manaTargets = new ArrayList<>();
        List<Integer> manaDemands = new ArrayList<>();
        int totalManaDemand = 0;

        List<IEnergyStorage> energyTargets = new ArrayList<>();
        List<Integer> energyDemands = new ArrayList<>();
        int totalEnergyDemand = 0;

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = ioMap.getOrDefault(dir, IOHandlerUtils.IOType.DISABLED);
            if (!type.outputs()) continue;

            // ✅ 使用快取獲取 capability
            var manaCache = manaCaches.get(dir);
            var energyCache = energyCaches.get(dir);

            IUnifiedManaHandler manaTarget = manaCache != null ? manaCache.getCapability() : null;
            IEnergyStorage energyTarget = energyCache != null ? energyCache.getCapability() : null;

            // 魔力接收端處理
            if (manaTarget != null && manaStorage != null && manaTarget.canReceive()) {
                int demand = manaTarget.getMaxManaStored() - manaTarget.getManaStored();
                // TODO [OutputHandlerV2]: demand is (maxMana - storedMana) — static model.
                // Future: dynamic sliding average of actual received mana over past N ticks.
                if (demand > 0) {
                    manaTargets.add(manaTarget);
                    manaDemands.add(demand);
                    totalManaDemand += demand;
                }
            }

            // 能量接收端處理
            if (energyTarget != null && energyStorage != null && energyTarget.canReceive()) {
                // Use long arithmetic to avoid int overflow with large-capacity receivers
                // (e.g. AE2 Energy Acceptor reports getMaxEnergyStored() near Integer.MAX_VALUE,
                // which overflows to a negative int when subtracted, causing demand <= 0 and
                // the target being silently skipped).
                long demandLong = (long) energyTarget.getMaxEnergyStored() - (long) energyTarget.getEnergyStored();
                int demand = (int) Math.min(demandLong, (long) Integer.MAX_VALUE);
                // TODO [OutputHandlerV2]: demand is (maxEnergy - storedEnergy) — static model.
                // Future: dynamic sliding average of actual received energy over past N ticks.
                if (demand > 0) {
                    energyTargets.add(energyTarget);
                    energyDemands.add(demand);
                    totalEnergyDemand += demand;
                }
            }
        }

        boolean didOutput = false;

        // 魔力輸出
        if (manaStorage != null && totalManaDemand > 0 && manaStorage.getManaStored() > 0) {
            int totalToSend = Math.min(manaStorage.getManaStored(), MAX_MANA_OUTPUT_PER_TICK);
            int sentTotal = 0; // 實際送出的總和

            for (int i = 0; i < manaTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (manaDemands.get(i) / (double) totalManaDemand));
                int accepted = manaTargets.get(i).receiveMana(portion, ManaAction.EXECUTE);
                if (accepted > 0) {
                    manaStorage.extractMana(accepted, ManaAction.EXECUTE);
                    didOutput = true;
                }
                sentTotal += accepted; // 累加實際送出量
            }

            // 處理剩餘的魔力：給需求最大的目標
            int remainder = totalToSend - sentTotal;
            if (remainder > 0 && !manaTargets.isEmpty()) {
                int hungriestIdx = 0;
                for (int j = 1; j < manaDemands.size(); j++) {
                    if (manaDemands.get(j) > manaDemands.get(hungriestIdx)) hungriestIdx = j;
                }
                int accepted = manaTargets.get(hungriestIdx).receiveMana(remainder, ManaAction.EXECUTE);
                if (accepted > 0) {
                    manaStorage.extractMana(accepted, ManaAction.EXECUTE);
                    didOutput = true;
                }
            }
        }

        // 能量輸出
        if (energyStorage != null && totalEnergyDemand > 0 && energyStorage.getEnergyStored() > 0) {
            int sentTotal = 0;
            int totalToSend = Math.min(energyStorage.getEnergyStored(), MAX_ENERGY_OUTPUT_PER_TICK);

            for (int i = 0; i < energyTargets.size(); i++) {
                int portion = (int) Math.round(totalToSend * (energyDemands.get(i) / (double) totalEnergyDemand));
                int accepted = energyTargets.get(i).receiveEnergy(portion, false);
                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                    didOutput = true;
                }
                sentTotal += accepted; // 累加實際送出量
            }

            // 處理剩餘的能量：給需求最大的目標
            int remainder = totalToSend - sentTotal;
            if (remainder > 0 && !energyTargets.isEmpty()) {
                int hungriestIdx = 0;
                for (int j = 1; j < energyDemands.size(); j++) {
                    if (energyDemands.get(j) > energyDemands.get(hungriestIdx)) hungriestIdx = j;
                }
                int accepted = energyTargets.get(hungriestIdx).receiveEnergy(remainder, false);
                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                    didOutput = true;
                }
            }
        }

        return didOutput;
    }

    public static class OutputThrottleController {
        private int noOutputStreak = 0;
        private int currentDelay = 0;

        public boolean shouldTryOutput() {
            return currentDelay-- <= 0;
        }

        public void recordOutputResult(boolean success) {
            if (success) {
                noOutputStreak = 0;
                currentDelay = 1; // 成功後下次仍會馬上再嘗試一次
            } else {
                noOutputStreak++;
                currentDelay = Math.min(10, noOutputStreak); // 每失敗一次就延長下一次的 delay（最多10）
            }
        }
    }




}
