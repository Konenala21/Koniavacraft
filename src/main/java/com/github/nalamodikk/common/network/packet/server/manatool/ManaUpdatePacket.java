package com.github.nalamodikk.common.network.packet.server.manatool;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.client.ManaUpdatePacketClient;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


public record ManaUpdatePacket(BlockPos pos, int mana) implements CustomPacketPayload {

    public static final Type<ManaUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsLibrary.BLOCK_POS, ManaUpdatePacket::pos,
                    ByteBufCodecs.VAR_INT, ManaUpdatePacket::mana,
                    ManaUpdatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerClientOnly(PayloadRegistrar registrar) {
        // 這行不能直接用 method reference，會讓 class loader 解析 Client 類而爆炸
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        // ✅ 在執行時才觸碰 client-only 類別
                        ManaUpdatePacketClient.handle(packet, context);
                    }
                })
        );
    }
    // ✅ 正確範本
    public static void registerToServer(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> {});
    }




    // ✅ 伺服器端封包發送工具
    public static void sendManaUpdate(ServerPlayer player, BlockPos pos, int mana) {
        PacketDistributor.sendToPlayer(player, new ManaUpdatePacket(pos, mana));
    }
}
