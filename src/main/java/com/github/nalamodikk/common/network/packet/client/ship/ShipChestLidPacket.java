package com.github.nalamodikk.common.network.packet.client.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.event.ShipChestAnimator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C：飛船上某格箱子的「蓋子該開/該關」權威訊號。船的箱子真身在影子維度，vanilla 那套「server 廣播
 * openCount 給 client 開蓋」傳不到視覺船，而影子箱子的 viewer 計數又因玩家在別維度而失效。所以改由
 * server 在玩家真的開/關了這格船箱子(ShipEntity.onChestOpened/onChestClosed，計人數)時直接送這個，
 * client 的 ShipChestAnimator 跟訊號開合蓋，不再靠「畫面上有沒有 GUI」去猜 → 不會抖、不會拍。
 */
public record ShipChestLidPacket(int entityId, BlockPos localPos, boolean open) implements CustomPacketPayload {

    public static final Type<ShipChestLidPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_chest_lid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipChestLidPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ShipChestLidPacket::entityId,
                    BlockPos.STREAM_CODEC, ShipChestLidPacket::localPos,
                    ByteBufCodecs.BOOL, ShipChestLidPacket::open,
                    ShipChestLidPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, ShipChestLidPacket::handle);
    }

    public static void handle(ShipChestLidPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ShipChestAnimator.setLid(packet.entityId(), packet.localPos(), packet.open()));
    }

    public static void sendToClients(com.github.nalamodikk.space.ship.ShipEntity ship, BlockPos local, boolean open) {
        PacketDistributor.sendToPlayersTrackingEntity(ship,
                new ShipChestLidPacket(ship.getId(), local, open));
    }
}
