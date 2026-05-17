package com.github.nalamodikk.narasystem.nara.network.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record NaraTutorialPacket(String tutorialId) implements CustomPacketPayload {

    public static final Type<NaraTutorialPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_tutorial"));

    public static final StreamCodec<FriendlyByteBuf, NaraTutorialPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, NaraTutorialPacket::tutorialId,
                    NaraTutorialPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, NaraTutorialPacket::handle);
    }

    private static void handle(NaraTutorialPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> NaraTutorialFlow.start(packet.tutorialId()));
    }

    public static void send(net.minecraft.server.level.ServerPlayer player, String tutorialId) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new NaraTutorialPacket(tutorialId));
    }
}
