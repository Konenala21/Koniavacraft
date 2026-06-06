package com.github.nalamodikk.common.network.packet.server.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipAssemblyPadBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 組裝台 GUI「組裝」按鈕：請 server 把盒內飛船組裝出航（移除方塊 + 生成 ShipEntity）。 */
public record ShipAssemblePacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ShipAssemblePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_assemble"));

    public static final StreamCodec<ByteBuf, ShipAssemblePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ShipAssemblePacket::pos,
                    ShipAssemblePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipAssemblePacket::handle);
    }

    public static void handle(ShipAssemblePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (Vec3.atCenterOf(packet.pos()).distanceToSqr(player.position()) > 64.0) return;
            Level level = player.level();
            if (!level.isLoaded(packet.pos())) return;
            if (level.getBlockEntity(packet.pos()) instanceof ShipAssemblyPadBlockEntity pad) {
                pad.assembleShip();
            }
        });
    }

    public static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new ShipAssemblePacket(pos));
    }
}
