package com.github.nalamodikk.common.network.packet.client.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C：飛船上某個 contraption 方塊的 blockstate 變了（互動方塊切狀態，例如門開關）。
 * contraption 只在 spawn 整包傳，中途改要單獨送這個更新給 client，否則 client 看不到門開、碰撞也不更新。
 * stateId 用 Block.getId/stateById 壓成 int。
 */
public record ShipBlockUpdatePacket(int entityId, BlockPos localPos, int stateId) implements CustomPacketPayload {

    public static final Type<ShipBlockUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_block_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipBlockUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ShipBlockUpdatePacket::entityId,
                    BlockPos.STREAM_CODEC, ShipBlockUpdatePacket::localPos,
                    ByteBufCodecs.VAR_INT, ShipBlockUpdatePacket::stateId,
                    ShipBlockUpdatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, ShipBlockUpdatePacket::handle);
    }

    public static void handle(ShipBlockUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() == null) return;
            Entity e = context.player().level().getEntity(packet.entityId());
            if (e instanceof ShipEntity ship) {
                ship.updateContraptionBlock(packet.localPos(), Block.stateById(packet.stateId()));
            }
        });
    }

    public static void sendToClients(ShipEntity ship, BlockPos local, BlockState state) {
        PacketDistributor.sendToPlayersTrackingEntity(ship,
                new ShipBlockUpdatePacket(ship.getId(), local, Block.getId(state)));
    }
}
