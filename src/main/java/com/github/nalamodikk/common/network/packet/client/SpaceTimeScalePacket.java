package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C：設定太空維度行星公轉/自轉的時間倍率。
 * timeScale = 1.0 為現實比例，越大轉越快。
 * 由 /koniava timescale <值> 指令觸發。
 */
public record SpaceTimeScalePacket(float timeScale) implements CustomPacketPayload {

    public static final Type<SpaceTimeScalePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "space_timescale"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceTimeScalePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, SpaceTimeScalePacket::timeScale, SpaceTimeScalePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (!FMLEnvironment.dist.isClient()) return;
                    ClientHandler.handle(packet);
                }));
    }

    // client-only 類別載入集中於巢狀類別，dedicated server 不會觸發
    private static final class ClientHandler {
        static void handle(SpaceTimeScalePacket packet) {
            com.github.nalamodikk.client.renderer.dimension.SpacePlanetManager.timeScale = packet.timeScale;
        }
    }
}
