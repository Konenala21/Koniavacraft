package com.github.nalamodikk.common.network.packet.server.deployer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record SetDeployerIntervalPacket(BlockPos pos, int intervalTick) implements CustomPacketPayload {

    public static final Type<SetDeployerIntervalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "set_deployer_interval"));

    public static final StreamCodec<ByteBuf, SetDeployerIntervalPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetDeployerIntervalPacket::pos,
                    ByteBufCodecs.VAR_INT, SetDeployerIntervalPacket::intervalTick,
                    SetDeployerIntervalPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, SetDeployerIntervalPacket::handle);
    }

    public static void handle(SetDeployerIntervalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (net.minecraft.world.phys.Vec3.atCenterOf(packet.pos())
                    .distanceToSqr(player.position()) > 64.0) return;
            Level level = player.level();
            if (!level.isLoaded(packet.pos())) return;
            if (level.getBlockEntity(packet.pos()) instanceof ManaDeployerBlockEntity deployer) {
                deployer.setIntervalTick(packet.intervalTick());
            }
        });
    }

    public static void sendToServer(BlockPos pos, int intervalTick) {
        PacketDistributor.sendToServer(new SetDeployerIntervalPacket(pos, intervalTick));
    }
}
