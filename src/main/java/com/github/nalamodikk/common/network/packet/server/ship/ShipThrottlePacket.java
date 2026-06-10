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
 * 飛船油門（0~1）：駕駛 client 用滾輪/控制台調整後送 server，server 設 DATA_THROTTLE（同步回所有 client + HUD）。
 * 油門 = 速度上限的使用比例（上限由引擎數決定），消耗也隨油門縮放。
 */
public record ShipThrottlePacket(float throttle) implements CustomPacketPayload {

    public static final Type<ShipThrottlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_throttle"));

    public static final StreamCodec<ByteBuf, ShipThrottlePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, ShipThrottlePacket::throttle, ShipThrottlePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipThrottlePacket::handle);
    }

    public static void handle(ShipThrottlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.getVehicle() instanceof ShipEntity ship)) return;
            int seat = ship.seatIndexOf(player);                 // 只有駕駛位的玩家能調油門
            if (seat >= 0 && seat < ShipEntity.MAX_DRIVERS) ship.setThrottle(packet.throttle());
        });
    }

    public static void sendToServer(float throttle) {
        PacketDistributor.sendToServer(new ShipThrottlePacket(throttle));
    }
}
