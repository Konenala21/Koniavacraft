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
 * 駕駛飛船的輸入：每 tick 從駕駛 client 傳當前按鍵狀態（前後/左右/上下 + 視角 yaw）給 server。
 * 飛船是 server 權威（isControlledByLocalInstance=false，client 不送 MoveVehiclePacket），
 * 所以移動在 server 算，client 只送輸入。
 */
public record ShipControlPacket(float forward, float strafe, int vertical, float yaw)
        implements CustomPacketPayload {

    public static final Type<ShipControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_control"));

    public static final StreamCodec<ByteBuf, ShipControlPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, ShipControlPacket::forward,
                    ByteBufCodecs.FLOAT, ShipControlPacket::strafe,
                    ByteBufCodecs.VAR_INT, ShipControlPacket::vertical,
                    ByteBufCodecs.FLOAT, ShipControlPacket::yaw,
                    ShipControlPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipControlPacket::handle);
    }

    public static void handle(ShipControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            // 只有駕駛（核心位）能控制；乘客的輸入忽略
            if (player.getVehicle() instanceof ShipEntity ship && ship.getControllingPassenger() == player) {
                ship.setControlInput(packet.forward(), packet.strafe(), packet.vertical(), packet.yaw());
            }
        });
    }

    public static void sendToServer(float forward, float strafe, int vertical, float yaw) {
        PacketDistributor.sendToServer(new ShipControlPacket(forward, strafe, vertical, yaw));
    }
}
