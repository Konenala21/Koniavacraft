package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.client.ResearchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Server → client: reverts a single research grid cell to the server-authoritative state.
 * Sent whenever the server rejects a ResearchAspectPlacePacket so the client's
 * optimistic update is rolled back.
 *
 * aspectId = empty  → cell should be EMPTY
 * aspectId = present → cell should contain this aspect
 */
public record ResearchCellRevertPacket(
        BlockPos tablePos,
        ResourceLocation researchId,
        int q,
        int r,
        Optional<ResourceLocation> aspectId)
        implements CustomPacketPayload {

    public static final Type<ResearchCellRevertPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "research_cell_revert"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ResearchCellRevertPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,                                    ResearchCellRevertPacket::tablePos,
                    ResourceLocation.STREAM_CODEC,                            ResearchCellRevertPacket::researchId,
                    ByteBufCodecs.VAR_INT,                                    ResearchCellRevertPacket::q,
                    ByteBufCodecs.VAR_INT,                                    ResearchCellRevertPacket::r,
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),    ResearchCellRevertPacket::aspectId,
                    ResearchCellRevertPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendTo(ServerPlayer player, BlockPos tablePos, ResourceLocation researchId,
                               int q, int r, @Nullable ResourceLocation aspectId) {
        PacketDistributor.sendToPlayer(player, new ResearchCellRevertPacket(
                tablePos, researchId, q, r, Optional.ofNullable(aspectId)));
    }

    public static void handle(ResearchCellRevertPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof ResearchScreen screen) {
                Aspect aspect = packet.aspectId().map(ModAspects::get).orElse(null);
                screen.revertCell(packet.tablePos(), packet.researchId(), packet.q(), packet.r(), aspect);
            }
        });
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, ResearchCellRevertPacket::handle);
    }
}
