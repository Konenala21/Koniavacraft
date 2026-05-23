package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record NaraTutorialSeenPacket(String tutorialId) implements CustomPacketPayload {

    public static final Type<NaraTutorialSeenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_tutorial_seen"));

    public static final StreamCodec<FriendlyByteBuf, NaraTutorialSeenPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, NaraTutorialSeenPacket::tutorialId,
                    NaraTutorialSeenPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, NaraTutorialSeenPacket::handle);
    }

    private static void handle(NaraTutorialSeenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var savedData = ResearchSavedData.get(player.serverLevel());
            savedData.getOrCreate(player.getUUID()).markTutorialSeen(packet.tutorialId());
            savedData.setDirty();
        });
    }

    public static void send(String tutorialId) {
        if (net.minecraft.client.Minecraft.getInstance().getConnection() == null) return;
        PacketDistributor.sendToServer(new NaraTutorialSeenPacket(tutorialId));
    }
}
