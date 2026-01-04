package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.util.NaraHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

public record NaraBindRequestPacket(boolean bind) implements CustomPacketPayload {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<NaraBindRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_bind_request"));

    public static final StreamCodec<FriendlyByteBuf, NaraBindRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, NaraBindRequestPacket::bind,
                    NaraBindRequestPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, NaraBindRequestPacket::handle);
    }

    public static void handle(NaraBindRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var sender = context.player();
            if (sender != null) {
                NaraHelper.setBound(sender, packet.bind());
                if (sender instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new NaraSyncPacket(packet.bind()));
                    LOGGER.debug("綁定請求封包發送測試");
                }
            }
        });
    }

    // ✅ 正確範本
    public static void registerToServer(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> {});
    }



    public static void send(boolean bind) {
        PacketDistributor.sendToServer(new NaraBindRequestPacket(bind));

    }
}
