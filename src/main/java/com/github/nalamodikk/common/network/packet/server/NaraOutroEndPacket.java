package com.github.nalamodikk.common.network.packet.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.NaraPhantomEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * C2S：boss 死亡演出 outro fade 開始時，告訴 server 把該玩家的 Nara phantom discard
 * 不然 Nara 會在 client camera 歸還後還在場上站著很尷尬
 */
public record NaraOutroEndPacket() implements CustomPacketPayload {

    public static final NaraOutroEndPacket INSTANCE = new NaraOutroEndPacket();

    public static final Type<NaraOutroEndPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_outro_end"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NaraOutroEndPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) return;
            ServerLevel sl = sp.serverLevel();
            // 找該玩家的 Nara phantom，discard
            for (NaraPhantomEntity nara : sl.getEntitiesOfClass(NaraPhantomEntity.class,
                    sp.getBoundingBox().inflate(300),
                    n -> n.getSourceUUID().map(sp.getUUID()::equals).orElse(false))) {
                nara.discard();
            }
        }));
    }
}
