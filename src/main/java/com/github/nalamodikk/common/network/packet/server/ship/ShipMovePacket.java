package com.github.nalamodikk.common.network.packet.server.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 飛船位置同步：client 權威（駕駛 client 算移動）下，每 tick 把船的位置/yaw 送給 server，
 * 讓 server 端的船跟著移動。vanilla 的 ServerboundMoveVehiclePacket 對這個自訂載具不可靠
 * （server 端船會停在原地，造成下船彈回、拆解無視距離），所以自己同步。
 */
public record ShipMovePacket(double x, double y, double z, float yaw) implements CustomPacketPayload {

    public static final Type<ShipMovePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_move"));

    public static final StreamCodec<ByteBuf, ShipMovePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, ShipMovePacket::x,
                    ByteBufCodecs.DOUBLE, ShipMovePacket::y,
                    ByteBufCodecs.DOUBLE, ShipMovePacket::z,
                    ByteBufCodecs.FLOAT, ShipMovePacket::yaw,
                    ShipMovePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipMovePacket::handle);
    }

    public static void handle(ShipMovePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            // 只接受駕駛（核心位）回報的位置
            if (player.getVehicle() instanceof ShipEntity ship && ship.getControllingPassenger() == player) {
                ship.absMoveTo(packet.x(), packet.y(), packet.z(), packet.yaw(), ship.getXRot());
            }
        });
    }

    public static void sendToServer(double x, double y, double z, float yaw) {
        PacketDistributor.sendToServer(new ShipMovePacket(x, y, z, yaw));
    }
}
