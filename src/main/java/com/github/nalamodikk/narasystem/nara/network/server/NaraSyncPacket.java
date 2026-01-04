package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.network.client.NaraSyncPacketclient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record NaraSyncPacket(boolean isBound) implements CustomPacketPayload {
    public static final Type<NaraSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_sync"));

    public static final StreamCodec<FriendlyByteBuf, NaraSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, NaraSyncPacket::isBound,
                    NaraSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, NaraSyncPacketclient::handleNaraSync);
    }


    // ✅ 正確範本
    public static void registerToServer(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> {});
    }




}
