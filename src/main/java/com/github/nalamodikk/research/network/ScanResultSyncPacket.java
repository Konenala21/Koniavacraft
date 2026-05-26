package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.client.ClientResearchCache;
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
 * Server -> client: notifies the client of a newly completed scan so the HUD
 * and scan log can be updated without a full KnowledgeSyncPacket.
 */
public record ScanResultSyncPacket(
        ResourceLocation targetId,
        List<ResourceLocation> aspectIds)
        implements CustomPacketPayload {

    public static final Type<ScanResultSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "scan_result_sync"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ScanResultSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,                             ScanResultSyncPacket::targetId,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(32)), ScanResultSyncPacket::aspectIds,
                    ScanResultSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendTo(ServerPlayer player, ResourceLocation targetId, List<Aspect> aspects) {
        List<ResourceLocation> ids = aspects.stream().map(Aspect::getId).toList();
        PacketDistributor.sendToPlayer(player, new ScanResultSyncPacket(targetId, ids));
    }

    public static void handle(ScanResultSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientResearchCache.addScannedTarget(packet.targetId(), packet.aspectIds()));
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, ScanResultSyncPacket::handle);
    }
}
