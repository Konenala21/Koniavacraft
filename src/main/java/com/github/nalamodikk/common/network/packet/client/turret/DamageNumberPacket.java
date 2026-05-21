package com.github.nalamodikk.common.network.packet.client.turret;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.DamageNumberRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record DamageNumberPacket(double x, double y, double z, float damage, byte dmgType, int entityId)
        implements CustomPacketPayload {

    public static final byte NORMAL = 0;
    public static final byte CRIT   = 1;
    public static final byte MAGIC  = 2;

    public static final Type<DamageNumberPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "damage_number"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageNumberPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeDouble(p.x);
                        buf.writeDouble(p.y);
                        buf.writeDouble(p.z);
                        buf.writeFloat(p.damage);
                        buf.writeByte(p.dmgType);
                        buf.writeInt(p.entityId);
                    },
                    buf -> new DamageNumberPacket(
                            buf.readDouble(), buf.readDouble(), buf.readDouble(),
                            buf.readFloat(), buf.readByte(), buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        DamageNumberRenderer.add(packet.x(), packet.y(), packet.z(),
                                packet.damage(), packet.dmgType(), packet.entityId());
                    }
                }));
    }
}
