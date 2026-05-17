package com.github.nalamodikk.common.network.packet.server.deployer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlockEntity;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record CycleDeployerSpeedPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<CycleDeployerSpeedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "cycle_deployer_speed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CycleDeployerSpeedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsLibrary.BLOCK_POS,
                    CycleDeployerSpeedPacket::pos,
                    CycleDeployerSpeedPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, CycleDeployerSpeedPacket::handle);
    }

    public static void handle(CycleDeployerSpeedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            if (!level.isLoaded(packet.pos())) return;
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof ManaDeployerBlockEntity deployer) {
                deployer.cycleSpeed();
            }
        });
    }

    public static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new CycleDeployerSpeedPacket(pos));
    }
}
