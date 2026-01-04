package com.github.nalamodikk.common.utils.capability;


import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.*;

public class IOHandlerUtils {

    public enum IOType {
        INPUT, OUTPUT, BOTH, DISABLED;

        public boolean canExtract() {
            return this == INPUT || this == BOTH;
        }
        public boolean outputs() {
            return this == OUTPUT || this == BOTH;
        }

        public boolean canInsert() {
            return this == OUTPUT || this == BOTH;
        }
    }

    public static boolean canExtract(Direction dir, EnumMap<Direction, IOType> config) {
        return config.getOrDefault(dir, IOType.DISABLED).canExtract();
    }

    public static boolean canInsert(Direction dir, EnumMap<Direction, IOType> config) {
        return config.getOrDefault(dir, IOType.DISABLED).canInsert();
    }

    public static void extractManaFromNeighbors(Level level, BlockPos pos, IUnifiedManaHandler selfStorage, EnumMap<Direction, IOType> config, int maxExtractPerTick) {
        if (level.isClientSide) return; // 🛑 客戶端不執行
        if (selfStorage == null || selfStorage.getManaStored() >= selfStorage.getMaxManaStored()) return; // 🛑 滿了或異常

        long remainingCapacity = selfStorage.getMaxManaStored() - selfStorage.getManaStored();

        List<Direction> inputs = config.entrySet().stream()
                .filter(e -> e.getValue() == IOType.INPUT || e.getValue() == IOType.BOTH)
                .map(Map.Entry::getKey)
                .toList();

        // 🧠 依據鄰居 mana 多寡排序（魔力多優先）
        List<Neighbor> neighbors = new ArrayList<>();

        for (Direction dir : inputs) {
            BlockPos neighborPos = pos.relative(dir);
            Direction neighborFacing = dir.getOpposite();

            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
            if (neighborEntity == null) continue;

            if (neighborEntity instanceof IConfigurableBlock configurable) {
                IOType neighborSetting = configurable.getIOMap().getOrDefault(neighborFacing, IOType.DISABLED);
                if (neighborSetting != IOType.OUTPUT && neighborSetting != IOType.BOTH) continue;
            }

            IUnifiedManaHandler neighborStorage = CapabilityUtils.getNeighborMana(level, neighborPos, dir);
            if (neighborStorage == null || neighborStorage.getManaStored() <= 0) continue;

            neighbors.add(new Neighbor(dir, neighborStorage));
        }

        // 🔽 依據鄰居儲存量排序（多的排前面）
        neighbors.sort(Comparator.comparingLong(n -> -n.handler().getManaStored()));

        for (Neighbor entry : neighbors) {
            Direction dir = entry.direction();
            IUnifiedManaHandler neighbor = entry.handler();

            // 🧪 預先模擬最大可抽取量（這裡寫死為 50，可調整）
            long toExtract = Math.min(maxExtractPerTick, remainingCapacity);
            long simulatedExtract = neighbor.extractMana((int) toExtract, ManaAction.get(true));
            if (simulatedExtract <= 0) continue;

            long simulatedInsert = selfStorage.receiveMana((int) simulatedExtract, ManaAction.get(true));
            if (simulatedInsert <= 0) continue;

            // ✅ 正式抽取與注入
            long extracted = neighbor.extractMana((int) simulatedInsert, ManaAction.get(false));
            long inserted = selfStorage.receiveMana((int) extracted, ManaAction.get(false));

            if (inserted > 0 && level.getBlockEntity(pos) instanceof BlockEntity be) {
                be.setChanged(); // 標記更新
            }

            // 🔁 更新剩餘容量，若已滿就停止
            remainingCapacity -= inserted;
            if (remainingCapacity <= 0) break;
        }
    }

    // 放在工具類或內部類內也可以
    public record Neighbor(Direction direction, IUnifiedManaHandler handler) {}


    public static void extractEnergyFromNeighbors(Level level, BlockPos pos, IEnergyStorage selfStorage, EnumMap<Direction, IOType> config, int maxPerSide) {
        for (Direction dir : Direction.values()) {
            if (!canExtract(dir, config)) continue;

            BlockPos neighborPos = pos.relative(dir);
            IEnergyStorage neighbor = CapabilityUtils.getNeighborEnergy(level, neighborPos, dir);
            if (neighbor == null) continue;

            int simulated = neighbor.extractEnergy(maxPerSide, true);
            if (simulated > 0) {
                int extracted = neighbor.extractEnergy(maxPerSide, false);
                selfStorage.receiveEnergy(extracted, false);
            }
        }
    }

    public static void extractItemsFromNeighbors(Level level, BlockPos pos, IItemHandler selfStorage, EnumMap<Direction, IOType> config) {
        // 範例用：你可以根據需要補進具體抽物品邏輯
    }


    /**
     * 將 Boolean 型方向設定（true 為輸出）轉換成 IOType 設定。
     * true 會變成 OUTPUT，false 會變成 DISABLED。
     */
    public static EnumMap<Direction, IOType> convertBooleanMapToIOType(EnumMap<Direction, Boolean> booleanMap) {
        EnumMap<Direction, IOType> ioMap = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            boolean isOutput = booleanMap.getOrDefault(dir, false);
            ioMap.put(dir, isOutput ? IOType.OUTPUT : IOType.DISABLED);
        }
        return ioMap;
    }

    /**
     * 將 IOType 型方向設定轉換成 Boolean 設定（僅視為輸出/非輸出）。
     * OUTPUT 與 BOTH 會視為 true，其餘為 false。
     */
    public static EnumMap<Direction, Boolean> convertIOTypeToBooleanMap(EnumMap<Direction, IOType> ioMap) {
        EnumMap<Direction, Boolean> booleanMap = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            IOType type = ioMap.getOrDefault(dir, IOType.DISABLED);
            booleanMap.put(dir, type == IOType.OUTPUT || type == IOType.BOTH);
        }
        return booleanMap;
    }



    public static IOType nextIOType(IOType current) {
        return switch (current) {
            case DISABLED -> IOType.OUTPUT;
            case OUTPUT -> IOType.INPUT;
            case INPUT -> IOType.BOTH;
            case BOTH -> IOType.DISABLED;
        };
    }

}
