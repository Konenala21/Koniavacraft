package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.hud.NaraDialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record NaraCloseDialoguePacket() implements CustomPacketPayload {

    public static final Type<NaraCloseDialoguePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_close_dialogue"));

    public static final StreamCodec<FriendlyByteBuf, NaraCloseDialoguePacket> STREAM_CODEC =
            StreamCodec.unit(new NaraCloseDialoguePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, NaraCloseDialoguePacket::handle);
    }

    private static void handle(NaraCloseDialoguePacket packet, IPayloadContext context) {
        context.enqueueWork(NaraDialogueManager::close);
    }
}
