package com.github.nalamodikk.common.network.packet.client.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.armor.ManaShieldEffectManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ManaShieldHitPacket(int entityId) implements CustomPacketPayload {

    public static final Type<ManaShieldHitPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_shield_hit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaShieldHitPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeInt(p.entityId),
                    buf -> new ManaShieldHitPacket(buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        ManaShieldEffectManager.addEffect(packet.entityId());
                    }
                }));
    }
}
