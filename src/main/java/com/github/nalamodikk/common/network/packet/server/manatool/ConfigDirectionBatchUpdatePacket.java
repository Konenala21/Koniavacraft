package com.github.nalamodikk.common.network.packet.server.manatool;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.common.utils.item.ItemStackUtils;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔄 批次 IO 配置更新封包
 *
 * 用於一次性更新多個方向的 IO 配置，減少網路開銷
 */
public record ConfigDirectionBatchUpdatePacket(
        BlockPos pos,
        Map<Direction, IOHandlerUtils.IOType> configMap
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigDirectionBatchUpdatePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "config_direction_batch_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigDirectionBatchUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            CodecsLibrary.BLOCK_POS,
            ConfigDirectionBatchUpdatePacket::pos,
            ByteBufCodecs.map(
                    HashMap::new,
                    CodecsLibrary.DIRECTION,
                    CodecsLibrary.STREAM_CODEC
            ),
            ConfigDirectionBatchUpdatePacket::configMap,
            ConfigDirectionBatchUpdatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 🛠️ 伺服端處理邏輯
     */
    public static void handle(ConfigDirectionBatchUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 船機器 BE 在影子維度：優先用開著的設定選單的 BE(=影子 BE)，不然 raw 查 player.level()+距離檢查會擋掉。
                BlockEntity be;
                Level level;
                if (player.containerMenu instanceof com.github.nalamodikk.common.screen.block.shared.UniversalConfigMenu menu
                        && menu.getBlockEntity() != null) {
                    be = menu.getBlockEntity();
                    level = be.getLevel();
                } else {
                    level = player.level();
                    if (net.minecraft.world.phys.Vec3.atCenterOf(packet.pos())
                            .distanceToSqr(player.position()) > 64.0) return;
                    be = level.getBlockEntity(packet.pos());
                }

                if (be instanceof IConfigurableBlock configurable) {
                    boolean hasChanges = false;

                    // ⭐ 批次更新所有方向的配置
                    for (Map.Entry<Direction, IOHandlerUtils.IOType> entry : packet.configMap().entrySet()) {
                        Direction direction = entry.getKey();
                        IOHandlerUtils.IOType newType = entry.getValue();

                        // 只更新真正有變化的配置
                        if (configurable.getIOConfig(direction) != newType) {
                            configurable.setIOConfig(direction, newType);
                            hasChanges = true;

                            if (KoniavacraftMod.IS_DEV) {
                                KoniavacraftMod.LOGGER.debug("[Server] Updated direction {} → {}", direction, newType);
                            }
                        }
                    }

                    // ⭐ 只在有變更時才執行同步操作
                    if (hasChanges) {
                        be.setChanged();

                        // 同步給所有客戶端（但只調用一次）
                        level.sendBlockUpdated(packet.pos(), be.getBlockState(), be.getBlockState(), 3);

                        // 更新玩家手上的魔杖配置
                        updateWandConfig(player, configurable);

                        if (KoniavacraftMod.IS_DEV) {
                            KoniavacraftMod.LOGGER.info("[Server] ✅ Batch updated {} directions", packet.configMap().size());
                        }
                    } else {
                        if (KoniavacraftMod.IS_DEV) {
                            KoniavacraftMod.LOGGER.debug("[Server] ⏩ Skipped batch update (no changes)");
                        }
                    }
                }
            }
        });
    }

    /**
     * 🔧 更新魔杖的 DataComponent 配置
     */
    private static void updateWandConfig(ServerPlayer player, IConfigurableBlock configurable) {
        ItemStack wand = ItemStackUtils.findHeldWand(player);
        if (wand.isEmpty()) {
            return;
        }

        // 構建新的配置映射
        EnumMap<Direction, Boolean> updatedConfig = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            updatedConfig.put(dir, configurable.isOutput(dir));
        }

        // 只在配置真的不同時才更新
        EnumMap<Direction, Boolean> existing = wand.get(ModDataComponents.CONFIGURED_DIRECTIONS);
        if (!updatedConfig.equals(existing)) {
            wand.set(ModDataComponents.CONFIGURED_DIRECTIONS, updatedConfig);

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.debug("[Server] 🔁 Updated wand config");
            }
        }
    }

    /**
     * 📝 註冊封包
     */
    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ConfigDirectionBatchUpdatePacket::handle);
    }
}
