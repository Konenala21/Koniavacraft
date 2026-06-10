package com.github.nalamodikk.common.network.packet.server.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 飛船油門（0~1）：玩家用滾輪（駕駛中）或控制台 GUI（站在船上）調整後送 server，server 設 DATA_THROTTLE
 * （同步回所有 client + HUD）。帶船 ID，所以站著開面板調 / 駕駛中滾輪調都走這條。
 * 油門 = 速度上限的使用比例（上限由引擎數決定），消耗也隨油門縮放。
 */
public record ShipThrottlePacket(int shipId, float throttle) implements CustomPacketPayload {

    public static final Type<ShipThrottlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_throttle"));

    public static final StreamCodec<ByteBuf, ShipThrottlePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ShipThrottlePacket::shipId,
                    ByteBufCodecs.FLOAT, ShipThrottlePacket::throttle,
                    ShipThrottlePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipThrottlePacket::handle);
    }

    public static void handle(ShipThrottlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Entity e = player.level().getEntity(packet.shipId());
            if (!(e instanceof ShipEntity ship)) return;
            // 防作弊：駕駛位的玩家，或站在船附近(會 raycast 到核心開面板的範圍內)才能調。
            boolean driver = player.getVehicle() == ship && ship.seatIndexOf(player) >= 0
                    && ship.seatIndexOf(player) < ShipEntity.MAX_DRIVERS;
            boolean nearby = ship.distanceToSqr(player) <= 64 * 64; // 大船寬鬆,夠站甲板上開面板
            if (driver || nearby) ship.setThrottle(packet.throttle());
        });
    }

    public static void sendToServer(int shipId, float throttle) {
        PacketDistributor.sendToServer(new ShipThrottlePacket(shipId, throttle));
    }
}
