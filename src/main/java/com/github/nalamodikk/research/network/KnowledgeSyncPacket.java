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

/**
 * Server → client: syncs the player's full research knowledge (aspects + research + tier).
 */
public record KnowledgeSyncPacket(List<ResourceLocation> discovered, List<ResourceLocation> completed, int tier)
        implements CustomPacketPayload {

    public static final Type<KnowledgeSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "knowledge_sync"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, KnowledgeSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), KnowledgeSyncPacket::discovered,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), KnowledgeSyncPacket::completed,
                    ByteBufCodecs.VAR_INT, KnowledgeSyncPacket::tier,
                    KnowledgeSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendTo(ServerPlayer player) {
        var knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        PacketDistributor.sendToPlayer(player, new KnowledgeSyncPacket(
                List.copyOf(knowledge.getDiscoveredAspects()),
                List.copyOf(knowledge.getCompletedResearch()),
                knowledge.getCurrentTier()
        ));
    }

    public static void handle(KnowledgeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientResearchCache.update(packet.discovered(), packet.completed(), packet.tier()));
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, KnowledgeSyncPacket::handle);
    }
}
