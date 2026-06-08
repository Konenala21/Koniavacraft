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
 * 飛船駕駛輸入（server 權威）：駕駛 client 每 tick 把方向鍵狀態 + 視角 yaw 送給 server。
 * server 是唯一真相，依輸入在 tick 算移動，再經 entity tracking 把位置廣播給所有 client（含駕駛），
 * client 端用 lerp 平滑跟隨（不自己預測 → 不抖、不分家）。
 */
public record ShipInputPacket(float forward, float strafe, int vertical, float yaw, float pitch) implements CustomPacketPayload {

    public static final Type<ShipInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_input"));

    public static final StreamCodec<ByteBuf, ShipInputPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, ShipInputPacket::forward,
                    ByteBufCodecs.FLOAT, ShipInputPacket::strafe,
                    ByteBufCodecs.VAR_INT, ShipInputPacket::vertical,
                    ByteBufCodecs.FLOAT, ShipInputPacket::yaw,
                    ByteBufCodecs.FLOAT, ShipInputPacket::pitch,
                    ShipInputPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipInputPacket::handle);
    }

    public static void handle(ShipInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.getVehicle() instanceof ShipEntity ship)) return;
            // 只接受坐在駕駛位（前 MAX_DRIVERS 個座位）的玩家輸入，並依其座位 index 存到對應槽
            int seat = ship.seatIndexOf(player);
            if (seat >= 0 && seat < ShipEntity.MAX_DRIVERS) {
                ship.setControlInput(seat, packet.forward(), packet.strafe(), packet.vertical(), packet.yaw(), packet.pitch());
            }
        });
    }

    public static void sendToServer(float forward, float strafe, int vertical, float yaw, float pitch) {
        PacketDistributor.sendToServer(new ShipInputPacket(forward, strafe, vertical, yaw, pitch));
    }
}
