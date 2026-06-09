package com.github.nalamodikk.common.network.packet.client.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C：飛船上某個 contraption 方塊實體(BE)的 NBT 變了(例如物品底座放了 item、機器內容變)。
 * contraption 只在 spawn 整包傳、render BE 是當下快照；BE 內容之後改(item/進度)要單獨送這個，
 * client 才更新 render BE，BER 才畫得出新的漂浮物品/狀態。tickServerMirror 在 NBT 真的變時才送。
 */
public record ShipBlockEntityDataPacket(int entityId, BlockPos localPos, CompoundTag nbt) implements CustomPacketPayload {

    public static final Type<ShipBlockEntityDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_be_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipBlockEntityDataPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ShipBlockEntityDataPacket::entityId,
                    BlockPos.STREAM_CODEC, ShipBlockEntityDataPacket::localPos,
                    ByteBufCodecs.TRUSTED_COMPOUND_TAG, ShipBlockEntityDataPacket::nbt,
                    ShipBlockEntityDataPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, ShipBlockEntityDataPacket::handle);
    }

    public static void handle(ShipBlockEntityDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() == null) return;
            Entity e = context.player().level().getEntity(packet.entityId());
            if (e instanceof ShipEntity ship) {
                ship.updateContraptionBlockEntityData(packet.localPos(), packet.nbt());
            }
        });
    }

    public static void sendToClients(ShipEntity ship, BlockPos local, CompoundTag nbt) {
        PacketDistributor.sendToPlayersTrackingEntity(ship,
                new ShipBlockEntityDataPacket(ship.getId(), local, nbt));
    }
}
