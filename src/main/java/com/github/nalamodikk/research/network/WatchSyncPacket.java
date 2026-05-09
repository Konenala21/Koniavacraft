package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.client.NaraWatchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server → client: sends a snapshot of the player's research knowledge so the
 * client can open {@link NaraWatchScreen} without a round-trip for every query.
 */
public record WatchSyncPacket(List<ResourceLocation> completed, int tier)
        implements CustomPacketPayload {

    public static final Type<WatchSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "watch_sync"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, WatchSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), WatchSyncPacket::completed,
                    ByteBufCodecs.VAR_INT,                                      WatchSyncPacket::tier,
                    WatchSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendTo(ServerPlayer player, Set<ResourceLocation> completed, int tier) {
        PacketDistributor.sendToPlayer(player,
                new WatchSyncPacket(List.copyOf(completed), tier));
    }

    public static void handle(WatchSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(
                        new NaraWatchScreen(new HashSet<>(packet.completed()), packet.tier()))
        );
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, WatchSyncPacket::handle);
    }
}
