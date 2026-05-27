package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.cinematic.VoidMirrorIntroManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record VoidMirrorIntroPacket() implements CustomPacketPayload {

    public static final VoidMirrorIntroPacket INSTANCE = new VoidMirrorIntroPacket();

    public static final Type<VoidMirrorIntroPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_mirror_intro"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidMirrorIntroPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        VoidMirrorIntroManager.start();
                    }
                }));
    }
}
