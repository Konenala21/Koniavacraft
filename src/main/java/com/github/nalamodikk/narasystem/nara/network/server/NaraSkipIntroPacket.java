package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record NaraSkipIntroPacket() implements CustomPacketPayload {

    public static final Type<NaraSkipIntroPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_skip_intro"));

    public static final StreamCodec<FriendlyByteBuf, NaraSkipIntroPacket> STREAM_CODEC =
            StreamCodec.unit(new NaraSkipIntroPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, NaraSkipIntroPacket::handle);
    }

    private static void handle(NaraSkipIntroPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            NaraServerEvents.cancelPunishmentState(player.getUUID());
        });
    }

    public static void send() {
        PacketDistributor.sendToServer(new NaraSkipIntroPacket());
    }
}
