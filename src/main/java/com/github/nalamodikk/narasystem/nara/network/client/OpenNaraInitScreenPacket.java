package com.github.nalamodikk.narasystem.nara.network.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.event.NaraIntroSchedulerEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record OpenNaraInitScreenPacket() implements CustomPacketPayload {

    public static final Type<OpenNaraInitScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "open_nara_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenNaraInitScreenPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenNaraInitScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    // ✅ 正確範本
    public static void registerToServer(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> {});
    }


    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    NaraIntroSchedulerEvent.schedule(35); // 3 秒延遲播放
                })
        );
    }

}
