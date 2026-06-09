package com.github.nalamodikk.common.network.packet.server.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * C2S：右鍵互動停著飛船上的方塊(放方塊 / 開容器 / 上船座位 / 切換門等)。client 算出準心瞄準的 pick
 * (local + 面 + 命中點)後發這個,server 用「收到的 pick」跑 interactWithPick,不再自己 raycast。
 * 原本 server 用同步過來、有網路延遲的玩家位置 raycast,玩家走動時放/開的會跟 client 準心不同一格。
 */
public record ShipInteractPacket(int entityId, BlockPos local, int faceIndex, Vec3 hit, boolean offHand)
        implements CustomPacketPayload {

    public static final Type<ShipInteractPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_interact"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipInteractPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ShipInteractPacket::entityId,
                    BlockPos.STREAM_CODEC, ShipInteractPacket::local,
                    ByteBufCodecs.VAR_INT, ShipInteractPacket::faceIndex,
                    VEC3, ShipInteractPacket::hit,
                    ByteBufCodecs.BOOL, ShipInteractPacket::offHand,
                    ShipInteractPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipInteractPacket::handle);
    }

    public static void handle(ShipInteractPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Entity e = player.level().getEntity(packet.entityId());
            if (e instanceof ShipEntity ship && ship.distanceToSqr(player) <= 64.0) {
                InteractionHand hand = packet.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                Direction face = Direction.from3DDataValue(packet.faceIndex());
                ship.interactWithPick(player, hand, new ShipEntity.Pick(packet.local(), face, packet.hit()));
            }
        });
    }
}
