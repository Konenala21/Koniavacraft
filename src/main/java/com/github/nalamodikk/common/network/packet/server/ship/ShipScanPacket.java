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

/** 組裝台 GUI「掃描」按鈕：請 server 重新掃描盒內飛船。 */
public record ShipScanPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ShipScanPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_scan"));

    public static final StreamCodec<ByteBuf, ShipScanPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ShipScanPacket::pos,
                    ShipScanPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipScanPacket::handle);
    }

    public static void handle(ShipScanPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (Vec3.atCenterOf(packet.pos()).distanceToSqr(player.position()) > 64.0) return;
            Level level = player.level();
            if (!level.isLoaded(packet.pos())) return;
            if (level.getBlockEntity(packet.pos()) instanceof ShipAssemblyPadBlockEntity pad) {
                pad.scan();
            }
        });
    }

    public static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new ShipScanPacket(pos));
    }
}
