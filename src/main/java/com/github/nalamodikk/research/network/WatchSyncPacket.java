package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.client.ResearchClientPayloadHandler;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server -> client: sends a snapshot of the player's research knowledge so the
 * client can open {@link com.github.nalamodikk.research.client.NaraWatchScreen} without a round-trip for every query.
 */
public record WatchSyncPacket(List<ResourceLocation> completed, List<ResourceLocation> availableOverrides,
                              List<ResourceLocation> lockedResearch, int tier,
                              Map<ResourceLocation, Integer> discovered,
                              Map<ResourceLocation, List<ResourceLocation>> scannedTargets)
        implements CustomPacketPayload {

    public static final Type<WatchSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "watch_sync"));

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<ResourceLocation>> RL_LIST_CODEC =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<io.netty.buffer.ByteBuf, WatchSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    RL_LIST_CODEC,                                                                               WatchSyncPacket::completed,
                    RL_LIST_CODEC,                                                                               WatchSyncPacket::availableOverrides,
                    RL_LIST_CODEC,                                                                               WatchSyncPacket::lockedResearch,
                    ByteBufCodecs.VAR_INT,                                                                       WatchSyncPacket::tier,
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT),       WatchSyncPacket::discovered,
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, RL_LIST_CODEC),               WatchSyncPacket::scannedTargets,
                    WatchSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendTo(ServerPlayer player, Set<ResourceLocation> completed, Set<ResourceLocation> availableOverrides,
                              Set<ResourceLocation> lockedResearch, int tier, Map<ResourceLocation, Integer> discovered,
                              Map<ResourceLocation, List<ResourceLocation>> scannedTargets) {
        PacketDistributor.sendToPlayer(player,
                new WatchSyncPacket(List.copyOf(completed), List.copyOf(availableOverrides), List.copyOf(lockedResearch),
                        tier, new HashMap<>(discovered), new HashMap<>(scannedTargets)));
    }

    public static void handle(WatchSyncPacket packet, IPayloadContext context) {
        ResearchClientPayloadHandler.handleWatchSync(packet, context);
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                ResearchClientPayloadHandler.handleWatchSync(packet, context);
            }
        });
    }
}
