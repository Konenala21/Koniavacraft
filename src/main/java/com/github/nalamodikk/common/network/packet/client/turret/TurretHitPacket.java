package com.github.nalamodikk.common.network.packet.client.turret;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.turret.TurretHitEffectManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record TurretHitPacket(double x, double y, double z, float chargeRatio) implements CustomPacketPayload {

    public static final Type<TurretHitPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "turret_hit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TurretHitPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeDouble(p.x);
                        buf.writeDouble(p.y);
                        buf.writeDouble(p.z);
                        buf.writeFloat(p.chargeRatio);
                    },
                    buf -> new TurretHitPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public Vec3 pos() { return new Vec3(x, y, z); }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        TurretHitEffectManager.addEffect(packet.pos(), packet.chargeRatio());
                    }
                }));
    }
}
