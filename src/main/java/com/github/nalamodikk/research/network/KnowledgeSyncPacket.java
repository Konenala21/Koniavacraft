package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.client.ResearchClientPayloadHandler;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
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

/**
 * Server -> client: syncs the player's full research knowledge (aspects + research + locked research + tier).
 */
public record KnowledgeSyncPacket(Map<ResourceLocation, Integer> discovered, List<ResourceLocation> completed,
                                  List<ResourceLocation> availableOverrides, List<ResourceLocation> lockedResearch,
                                  int tier)
        implements CustomPacketPayload {

    public static final Type<KnowledgeSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "knowledge_sync"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, KnowledgeSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), KnowledgeSyncPacket::discovered,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), KnowledgeSyncPacket::completed,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), KnowledgeSyncPacket::availableOverrides,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), KnowledgeSyncPacket::lockedResearch,
                    ByteBufCodecs.VAR_INT, KnowledgeSyncPacket::tier,
                    KnowledgeSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendTo(ServerPlayer player) {
        PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        PacketDistributor.sendToPlayer(player, new KnowledgeSyncPacket(
                new HashMap<>(knowledge.getDiscoveredAspectsMap()),
                List.copyOf(knowledge.getCompletedResearch()),
                List.copyOf(knowledge.getAvailableResearchOverrides()),
                List.copyOf(knowledge.getLockedResearch()),
                knowledge.getCurrentTier()
        ));
    }

    public static void handle(KnowledgeSyncPacket packet, IPayloadContext context) {
        ResearchClientPayloadHandler.handleKnowledgeSync(packet, context);
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                ResearchClientPayloadHandler.handleKnowledgeSync(packet, context);
            }
        });
    }
}
