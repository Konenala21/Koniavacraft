package com.github.nalamodikk.common.utils.data;


import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CodecsLibrary {
    public static final StreamCodec<RegistryFriendlyByteBuf, IOHandlerUtils.IOType> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public IOHandlerUtils.IOType decode(RegistryFriendlyByteBuf buf) {
                    int index = buf.readVarInt();
                    IOHandlerUtils.IOType[] values = IOHandlerUtils.IOType.values();
                    return index >= 0 && index < values.length ? values[index] : IOHandlerUtils.IOType.DISABLED;
                }


                @Override
                public void encode(RegistryFriendlyByteBuf buf, IOHandlerUtils.IOType value) {
                    buf.writeVarInt(value.ordinal());
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, EnumMap<Direction, IOHandlerUtils.IOType>> DIRECTION_IOTYPE_CODEC =
            StreamCodec.of(
                    (buf, map) -> {
                        buf.writeVarInt(map.size());
                        for (var entry : map.entrySet()) {
                            CodecsLibrary.DIRECTION.encode(buf, entry.getKey());
                            CodecsLibrary.STREAM_CODEC.encode(buf, entry.getValue());
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        EnumMap<Direction, IOHandlerUtils.IOType> map = new EnumMap<>(Direction.class);
                        for (int i = 0; i < size; i++) {
                            Direction dir = CodecsLibrary.DIRECTION.decode(buf);
                            IOHandlerUtils.IOType type = CodecsLibrary.STREAM_CODEC.decode(buf);
                            map.put(dir, type);
                        }
                        return map;
                    }
            );

    // ======================
    // 🧱 Block / Pos 類型
    // ======================
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS = new StreamCodec<>() {
        @Override
        public BlockPos decode(RegistryFriendlyByteBuf buf) {
            return BlockPos.of(buf.readLong());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BlockPos pos) {
            buf.writeLong(pos.asLong());
        }
    };

    // ======================
    // 🎁 ItemStack 類型
    // ======================
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK = new StreamCodec<>() {
        @Override
        public ItemStack decode(RegistryFriendlyByteBuf buf) {
            return ItemStack.STREAM_CODEC.decode(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ItemStack value) {
            ItemStack.STREAM_CODEC.encode(buf, value);
        }
    };

    // ======================
    // 🧭 Direction / 面向類
    // ======================
    public static final StreamCodec<RegistryFriendlyByteBuf, Direction> DIRECTION = new StreamCodec<>() {
        @Override
        public Direction decode(RegistryFriendlyByteBuf buf) {
            return Direction.from3DDataValue(buf.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, Direction value) {
            buf.writeVarInt(value.get3DDataValue());
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, EnumMap<Direction, Boolean>> DIRECTION_BOOLEAN_CODEC = StreamCodec.of(
            (buf, map) -> {
                buf.writeVarInt(map.size());
                for (Map.Entry<Direction, Boolean> entry : map.entrySet()) {
                    buf.writeVarInt(entry.getKey().get3DDataValue());
                    buf.writeBoolean(entry.getValue());
                }
            },
            buf -> {
                int size = buf.readVarInt();
                EnumMap<Direction, Boolean> map = new EnumMap<>(Direction.class);
                for (int i = 0; i < size; i++) {
                    Direction dir = Direction.from3DDataValue(buf.readVarInt());
                    boolean value = buf.readBoolean();
                    map.put(dir, value);
                }
                return map;
            }
    );

    public static final Codec<EnumMap<Direction, Boolean>> DIRECTION_BOOLEAN_MAP =
            Codec.unboundedMap(Direction.CODEC, Codec.BOOL).xmap(
                    map -> {
                        EnumMap<Direction, Boolean> enumMap = new EnumMap<>(Direction.class);
                        enumMap.putAll(map);
                        return enumMap;
                    },
                    map -> new HashMap<>(map)
            );


    public static final Codec<EnumMap<Direction, IOHandlerUtils.IOType>> DIRECTION_IOTYPE_MAP =
            Codec.unboundedMap(Direction.CODEC, Codec.STRING.xmap(
                    IOHandlerUtils.IOType::valueOf,
                    IOHandlerUtils.IOType::name
            )).xmap(
                    map -> {
                        EnumMap<Direction, IOHandlerUtils.IOType> enumMap = new EnumMap<>(Direction.class);
                        map.forEach(enumMap::put);
                        return enumMap;
                    },
                    Function.identity()
            );
}


