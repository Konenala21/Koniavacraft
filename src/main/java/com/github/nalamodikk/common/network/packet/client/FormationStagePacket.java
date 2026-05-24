package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.particle.ClientInteractiveFormationManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

public record FormationStagePacket(UUID ownerUuid, int stage) implements CustomPacketPayload {

    public static final Type<FormationStagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "formation_stage"));

    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> { buf.writeLong(uuid.getMostSignificantBits()); buf.writeLong(uuid.getLeastSignificantBits()); },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FormationStagePacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_CODEC,            FormationStagePacket::ownerUuid,
                    ByteBufCodecs.VAR_INT, FormationStagePacket::stage,
                    FormationStagePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void broadcast(net.minecraft.server.level.ServerPlayer player, int stage) {
        var packet = new FormationStagePacket(player.getUUID(), stage);
        PacketDistributor.sendToPlayersNear(
                player.serverLevel(), null,
                player.getX(), player.getY(), player.getZ(), 64.0, packet
        );
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        ClientInteractiveFormationManager.setStage(packet.ownerUuid(), packet.stage());
                    }
                }));
    }
}
