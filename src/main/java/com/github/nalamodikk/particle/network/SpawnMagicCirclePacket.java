package com.github.nalamodikk.particle.network;

import com.github.nalamodikk.particle.effects.ClientEffectManager;
import com.github.nalamodikk.particle.effects.ClientMagicCircleEffect;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * ?賹??啾??????殉???謚??祈蝒??
 */
public record SpawnMagicCirclePacket(BlockPos pos, int effectType) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpawnMagicCirclePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("koniava", "spawn_magic_circle"));

    public static final StreamCodec<ByteBuf, SpawnMagicCirclePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SpawnMagicCirclePacket::pos,
        ByteBufCodecs.INT, SpawnMagicCirclePacket::effectType,
        SpawnMagicCirclePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * ?桅???祈蝒??
     */
    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(
            TYPE,
            STREAM_CODEC,
            SpawnMagicCirclePacket::handle
        );
    }

    /**
     * ???????堆撓???
     */
    public static void handle(SpawnMagicCirclePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // ?撖?????遴竣?????
            ClientMagicCircleEffect.Config config = switch (packet.effectType) {
                case 1 -> ClientMagicCircleEffect.Config.manaGenerator();
                case 2 -> ClientMagicCircleEffect.Config.persistent();
                default -> new ClientMagicCircleEffect.Config();
            };

            // ?????
            ClientEffectManager.getInstance().createMagicCircle(packet.pos, config);
        });
    }
}
