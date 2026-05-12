package com.github.nalamodikk.common.network.packet.server.manatool;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ModeChangePacket(boolean forward) implements CustomPacketPayload {

    public static final Type<ModeChangePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mode_change"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModeChangePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ModeChangePacket::forward,
                    ModeChangePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ??甇?Ⅱ撠????剁?NeoForge 撠?蝟餌絞???撘?
    public static void handle(ModeChangePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof ManaDebugToolItem tool) {
                    tool.cycleMode(stack, packet.forward(), player); // ??server ?銵?
                }
            }
        });
    }


    // ??閮餃??寞?嚗 ModNetworking 蝯曹?隤輻
    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ModeChangePacket::handle);
    }

    // ??摰Ｘ蝡臬????
    public static void sendToServer(boolean forward) {
        PacketDistributor.sendToServer(new ModeChangePacket(forward));
    }
}
