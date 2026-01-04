package com.github.nalamodikk.common.coreapi.machine.logic.IO;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.energy.IEnergyStorage;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class SmartOutputHelper {

    private static final int MAX_OUTPUT_PER_TICK = 40;

    public static boolean tryOutput(ServerLevel level, BlockPos pos, EnumMap<Direction, IOHandlerUtils.IOType> ioMap,
                                    List<ResourceChannel<?>> resourceChannels) {
        boolean didOutput = false;

        for (ResourceChannel<?> channel : resourceChannels) {
            didOutput |= channel.distribute(level, pos, ioMap);
        }

        return didOutput;
    }

    public static class ResourceChannel<T> {
        private final T source;
        private final Predicate<T> canOutput;
        private final ToIntFunction<T> getStored;
        private final int maxPerTick;
        private final CapabilityProvider<T> capabilityProvider;
        private final BiFunction<T, Integer, Integer> sender;
        private final java.util.function.IntUnaryOperator extractor;

        public ResourceChannel(T source,
                               Predicate<T> canOutput,
                               ToIntFunction<T> getStored,
                               int maxPerTick,
                               CapabilityProvider<T> capabilityProvider,
                               BiFunction<T, Integer, Integer> sender,
                               java.util.function.IntUnaryOperator extractor) {
            this.source = source;
            this.canOutput = canOutput;
            this.getStored = getStored;
            this.maxPerTick = maxPerTick;
            this.capabilityProvider = capabilityProvider;
            this.sender = sender;
            this.extractor = extractor;
        }

        public boolean distribute(ServerLevel level, BlockPos pos, EnumMap<Direction, IOHandlerUtils.IOType> ioMap) {
            if (!canOutput.test(source) || getStored.applyAsInt(source) <= 0) return false;

            List<T> targets = new ArrayList<>();
            List<Integer> demands = new ArrayList<>();
            int totalDemand = 0;

            for (Direction dir : Direction.values()) {
                IOHandlerUtils.IOType type = ioMap.getOrDefault(dir, IOHandlerUtils.IOType.DISABLED);
                if (!type.outputs()) continue;

                BlockPos targetPos = pos.relative(dir);
                Direction inputSide = dir.getOpposite();

                T target = capabilityProvider.get(level, targetPos, inputSide);
                if (target != null) {
                    int demand = getDemand(target);
                    if (demand > 0) {
                        targets.add(target);
                        demands.add(demand);
                        totalDemand += demand;
                    }
                }
            }

            int toSend = Math.min(getStored.applyAsInt(source), maxPerTick);
            return distributeResources(targets, demands, totalDemand, toSend, sender, extractor);
        }

        private int getDemand(T target) {
            if (target instanceof IUnifiedManaHandler mana) {
                return mana.getMaxManaStored() - mana.getManaStored();
            } else if (target instanceof IEnergyStorage energy) {
                return energy.getMaxEnergyStored() - energy.getEnergyStored();
            }
            return 0;
        }
    }

    private static <T> boolean distributeResources(List<T> targets, List<Integer> demands, int totalDemand,
                                                   int totalToSend, BiFunction<T, Integer, Integer> sender,
                                                   java.util.function.IntUnaryOperator extractor) {
        boolean didSend = false;
        int sentTotal = 0;

        for (int i = 0; i < targets.size(); i++) {
            int portion = (int) Math.round(totalToSend * (demands.get(i) / (double) totalDemand));
            int accepted = sender.apply(targets.get(i), portion);
            if (accepted > 0) {
                extractor.applyAsInt(accepted);
                didSend = true;
            }
            sentTotal += accepted;
        }

        int remainder = totalToSend - sentTotal;
        if (remainder > 0 && !targets.isEmpty()) {
            int accepted = sender.apply(targets.get(0), remainder);
            if (accepted > 0) {
                extractor.applyAsInt(accepted);
                didSend = true;
            }
        }

        return didSend;
    }

    public interface CapabilityProvider<T> {
        T get(ServerLevel level, BlockPos pos, Direction direction);
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
                currentDelay = 1;
            } else {
                noOutputStreak++;
                currentDelay = Math.min(10, noOutputStreak);
            }
        }
    }
}
