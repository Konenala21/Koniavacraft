package com.github.nalamodikk.common.network.packet.server.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Comparator;

/** 組裝台 GUI「拆解」按鈕：收回組裝台附近最近的一艘飛船（把方塊寫回世界）。 */
public record ShipDisassemblePacket(BlockPos pos) implements CustomPacketPayload {

    public static final int RANGE = 16; // 收船作用範圍

    public static final Type<ShipDisassemblePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_disassemble"));

    public static final StreamCodec<ByteBuf, ShipDisassemblePacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, ShipDisassemblePacket::pos, ShipDisassemblePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipDisassemblePacket::handle);
    }

    public static void handle(ShipDisassemblePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (Vec3.atCenterOf(packet.pos()).distanceToSqr(player.position()) > 64.0) return;
            Level level = player.level();
            Vec3 center = Vec3.atCenterOf(packet.pos());
            AABB area = new AABB(center, center).inflate(RANGE);
            ShipEntity nearest = level.getEntitiesOfClass(ShipEntity.class, area).stream()
                    .min(Comparator.comparingDouble(s -> s.position().distanceToSqr(center)))
                    .orElse(null);
            if (nearest == null) {
                player.displayClientMessage(
                        Component.translatable("message.koniava.ship.no_ship_nearby"), true);
                return;
            }
            if (!nearest.disassemble()) {
                player.displayClientMessage(
                        Component.translatable("message.koniava.ship.dock_blocked"), true);
            }
        });
    }

    public static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new ShipDisassemblePacket(pos));
    }
}
