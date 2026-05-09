package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.EnumMap;
import java.util.Map;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModDataComponents {




    public static final DataComponentType<BasicTechWandItem.TechWandMode> TECH_WAND_MODE =
            DataComponentType.<BasicTechWandItem.TechWandMode>builder()
                    .persistent(StringRepresentable.fromEnum(BasicTechWandItem.TechWandMode::values))
                    .networkSynchronized(
                            ByteBufCodecs.stringUtf8(255).map(
                                    s -> Enum.valueOf(BasicTechWandItem.TechWandMode.class, s),
                                    BasicTechWandItem.TechWandMode::name
                            )
                    )
                    .build();

    public static final DataComponentType<BlockPos> SAVED_BLOCK_POS =
            DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build();

    // 自製 enumMap Codec：使用 unboundedMap + EnumMap 優化包裝
    public static final Codec<Map<Direction, Boolean>> DIRECTION_BOOL_MAP_CODEC =
            Codec.unboundedMap(Direction.CODEC, Codec.BOOL).xmap(
                    map -> new EnumMap<>(map),
                    map -> map
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, Map<Direction, Boolean>> DIRECTION_BOOL_MAP_STREAM_CODEC =
            StreamCodec.of(
                    (buf, map) -> {
                        buf.writeVarInt(map.size());
                        for (Map.Entry<Direction, Boolean> entry : map.entrySet()) {
                            buf.writeEnum(entry.getKey());
                            buf.writeBoolean(entry.getValue());
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        Map<Direction, Boolean> map = new EnumMap<>(Direction.class);
                        for (int i = 0; i < size; i++) {
                            Direction dir = buf.readEnum(Direction.class);
                            boolean value = buf.readBoolean();
                            map.put(dir, value);
                        }
                        return map;
                    }
            );

    public static final DataComponentType<EnumMap<Direction, Boolean>> CONFIGURED_DIRECTIONS =
            DataComponentType.<EnumMap<Direction, Boolean>>builder()
                    .persistent(CodecsLibrary.DIRECTION_BOOLEAN_MAP)              // 儲存用（NBT）
                    .networkSynchronized(CodecsLibrary.DIRECTION_BOOLEAN_CODEC)   // 封包同步用
                    .build();


    public static final DataComponentType<EnumMap<Direction, IOHandlerUtils.IOType>> CONFIGURED_DIRECTIONS_IO =
            DataComponentType.<EnumMap<Direction, IOHandlerUtils.IOType>>builder()
                    .persistent(CodecsLibrary.DIRECTION_IOTYPE_MAP)                  // 儲存用（NBT）
                    .networkSynchronized(CodecsLibrary.DIRECTION_IOTYPE_CODEC)       // 封包同步用
                    .build();

    public static final DataComponentType<Map<Direction, Boolean>> SAVED_DIRECTIONS =
            DataComponentType.<Map<Direction, Boolean>>builder()
                    .persistent(DIRECTION_BOOL_MAP_CODEC)
                    .networkSynchronized(DIRECTION_BOOL_MAP_STREAM_CODEC)
                    .build();

    public static final DataComponentType<Boolean> NARA_IMPRINT =
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build();

    public static final DataComponentType<Integer> MANA_STORED =
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build();

    public static final DataComponentType<Integer> MAX_MANA =
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build();

    /** Stores which research entry a Research Note represents. */
    public static final DataComponentType<ResourceLocation> RESEARCH_ID =
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(
                            ByteBufCodecs.stringUtf8(255).map(
                                    ResourceLocation::tryParse,
                                    ResourceLocation::toString))
                    .build();


    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.DATA_COMPONENT_TYPE, helper -> {
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mode_index"), ManaDebugToolItem.MODE_INDEX);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "tech_wand_mode"), TECH_WAND_MODE);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "saved_directions"), SAVED_DIRECTIONS);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "configured_directions"), CONFIGURED_DIRECTIONS);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "configured_directions_io"), CONFIGURED_DIRECTIONS_IO);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_imprint"), NARA_IMPRINT);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_stored"), MANA_STORED);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "max_mana"), MAX_MANA);
            helper.register(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "research_id"), RESEARCH_ID);
        });
    }
}