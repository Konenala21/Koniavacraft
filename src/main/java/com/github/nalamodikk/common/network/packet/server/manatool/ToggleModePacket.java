package com.github.nalamodikk.common.network.packet.server.manatool;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ToggleModePacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ToggleModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "toggle_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsLibrary.BLOCK_POS,
                    ToggleModePacket::pos,
                    ToggleModePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ToggleModePacket::handle);
    }

    public static void handle(ToggleModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Level level = player.level();

            if (!level.isLoaded(packet.pos())) return;
            if (net.minecraft.world.phys.Vec3.atCenterOf(packet.pos())
                    .distanceToSqr(player.position()) > 64.0) return;

            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof ManaGeneratorBlockEntity generator) {
                generator.toggleMode(); // 執行切換模式
            }
        });
    }

    // 🔁 工具方法（選配）供外部快速發送
    public static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new ToggleModePacket(pos));
    }
}
