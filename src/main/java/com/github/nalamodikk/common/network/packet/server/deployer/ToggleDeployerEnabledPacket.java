package com.github.nalamodikk.common.network.packet.server.deployer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ToggleDeployerEnabledPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ToggleDeployerEnabledPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "toggle_deployer_enabled"));

    public static final StreamCodec<ByteBuf, ToggleDeployerEnabledPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ToggleDeployerEnabledPacket::pos,
                    ToggleDeployerEnabledPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ToggleDeployerEnabledPacket::handle);
    }

    public static void handle(ToggleDeployerEnabledPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (net.minecraft.world.phys.Vec3.atCenterOf(packet.pos())
                    .distanceToSqr(player.position()) > 64.0) return;
            Level level = player.level();
            if (!level.isLoaded(packet.pos())) return;
            if (level.getBlockEntity(packet.pos()) instanceof ManaDeployerBlockEntity deployer) {
                deployer.toggleEnabled();
            }
        });
    }

    public static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new ToggleDeployerEnabledPacket(pos));
    }
}
