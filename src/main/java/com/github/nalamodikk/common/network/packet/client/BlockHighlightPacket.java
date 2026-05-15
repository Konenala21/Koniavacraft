package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/**
 * 伺服器發給客戶端，要求在指定位置畫出高亮邊框。
 * colorType: 0 = 紅（材料不足）, 1 = 橘（位置被佔用）
 */
public record BlockHighlightPacket(List<BlockPos> positions, int colorType) implements CustomPacketPayload {

    public static final Type<BlockHighlightPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "block_highlight"));

    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> POS_CODEC =
            StreamCodec.of(
                    (buf, pos) -> buf.writeLong(pos.asLong()),
                    buf -> BlockPos.of(buf.readLong())
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockHighlightPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(java.util.ArrayList::new, POS_CODEC), BlockHighlightPacket::positions,
                    ByteBufCodecs.VAR_INT, BlockHighlightPacket::colorType,
                    BlockHighlightPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        BlockHighlightRenderer.receive(packet.positions(), packet.colorType());
                    }
                }));
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player,
                                     List<BlockPos> positions, int colorType) {
        if (!positions.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new BlockHighlightPacket(positions, colorType));
        }
    }
}
