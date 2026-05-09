package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Set;

/**
 * Server → client: syncs the player's discovered aspects to {@link ClientResearchCache}.
 * Does NOT open any screen — use this whenever aspects change outside the watch-open flow.
 */
public record AspectSyncPacket(List<ResourceLocation> discovered)
        implements CustomPacketPayload {

    public static final Type<AspectSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "aspect_sync"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, AspectSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    AspectSyncPacket::discovered,
                    AspectSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendTo(ServerPlayer player) {
        var knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        Set<ResourceLocation> discovered = knowledge.getDiscoveredAspects();
        PacketDistributor.sendToPlayer(player, new AspectSyncPacket(List.copyOf(discovered)));
    }

    public static void handle(AspectSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientResearchCache.update(packet.discovered()));
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, AspectSyncPacket::handle);
    }
}
