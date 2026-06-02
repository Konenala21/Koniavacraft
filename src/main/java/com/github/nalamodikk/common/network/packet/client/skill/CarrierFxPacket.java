package com.github.nalamodikk.common.network.packet.client.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.CarrierFxClient;
import com.github.nalamodikk.research.skill.fx.CarrierFxType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C:某個需要 shader 的載體在世界某處放了一發,通知附近 client 播對應的 shader 視覺。
 * handler 只在 client 跑,所以對 {@link CarrierFxClient} 的引用永不在 dedicated server 載入。
 */
public record CarrierFxPacket(int typeId, double x, double y, double z) implements CustomPacketPayload {

    public static final Type<CarrierFxPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "carrier_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CarrierFxPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CarrierFxPacket::typeId,
                    ByteBufCodecs.DOUBLE, CarrierFxPacket::x,
                    ByteBufCodecs.DOUBLE, CarrierFxPacket::y,
                    ByteBufCodecs.DOUBLE, CarrierFxPacket::z,
                    CarrierFxPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CarrierFxPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CarrierFxClient.play(packet.typeId(), new Vec3(packet.x(), packet.y(), packet.z())));
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, CarrierFxPacket::handle);
    }

    /** 送給座標 64 格內的所有玩家。 */
    public static void send(ServerLevel level, Vec3 pos, CarrierFxType type) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, 64.0,
                new CarrierFxPacket(type.ordinal(), pos.x, pos.y, pos.z));
    }
}
