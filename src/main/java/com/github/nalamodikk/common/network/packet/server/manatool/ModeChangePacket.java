package com.github.nalamodikk.common.network.packet.server.manatool;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

    // ✅ 正確封包處理器（NeoForge 封包系統期望的格式）
    public static void handle(ModeChangePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof ManaDebugToolItem tool) {
                    tool.cycleMode(stack, packet.forward(), player); // ✅ server 才執行
                }
            }
        });
    }


    // ✅ 註冊方法：由 ModNetworking 統一調用
    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ModeChangePacket::handle);
    }

    // ✅ 客戶端傳送封包
    public static void sendToServer(boolean forward) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ModeChangePacket(forward));
        }
    }
}
