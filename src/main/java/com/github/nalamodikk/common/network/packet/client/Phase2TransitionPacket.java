package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.cinematic.Phase2TransitionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** S2C：boss 進入二階段方塊機甲變身演出，client 啟動環繞鏡頭 + 鎖輸入。 */
public record Phase2TransitionPacket(int bossEntityId) implements CustomPacketPayload {

    public static final Type<Phase2TransitionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "phase2_transition"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Phase2TransitionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, Phase2TransitionPacket::bossEntityId,
                    Phase2TransitionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        Phase2TransitionManager.start(packet.bossEntityId());
                    }
                }));
    }
}
