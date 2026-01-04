package com.github.nalamodikk.common.coreapi.machine.logic.IO;

import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;


import java.util.EnumMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class SmartInputHelper {

    public static <T> void tryPull(ServerLevel level, BlockPos pos, EnumMap<Direction, IOHandlerUtils.IOType> ioMap,
                                   T storage, Predicate<T> canInsert, BiFunction<T, T, Integer> transferFunc,
                                   SmartOutputHelper.CapabilityProvider<T> provider) {

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = ioMap.getOrDefault(dir, IOHandlerUtils.IOType.DISABLED);
            if (!type.canInsert()) continue;

            BlockPos neighborPos = pos.relative(dir);
            Direction outputSide = dir.getOpposite();
            T neighborStorage = provider.get(level, neighborPos, outputSide);
            if (neighborStorage == null || !canInsert.test(storage)) continue;

            int moved = transferFunc.apply(neighborStorage, storage);
            if (moved > 0) break; // 若有成功搬移則跳出
        }
    }
}
