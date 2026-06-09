package com.github.nalamodikk.common.network.packet.server.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * C2S：玩家挖停著飛船上的方塊。client 算出準心瞄準的 local 方塊後發這個,server 挖「收到的那一格」,
 * 不再 server 自己 raycast。原本 server raycast 用的是同步過來、有網路延遲的玩家位置,玩家走動時
 * 會跟 client 準心算的不同一格 → 挖到別的方塊。改由 client 指定就跟準心一致(同 vanilla 挖方塊)。
 */
public record ShipBreakBlockPacket(int entityId, BlockPos localPos) implements CustomPacketPayload {

    public static final Type<ShipBreakBlockPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_break_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipBreakBlockPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ShipBreakBlockPacket::entityId,
                    BlockPos.STREAM_CODEC, ShipBreakBlockPacket::localPos,
                    ShipBreakBlockPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ShipBreakBlockPacket::handle);
    }

    public static void handle(ShipBreakBlockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Entity e = player.level().getEntity(packet.entityId());
            if (e instanceof ShipEntity ship && ship.isParked()) {
                // 範圍防作弊:算到「被挖的方塊本身」的距離,不是船原點(大船的方塊離原點很遠 → 用原點會誤擋)。
                BlockPos lp = packet.localPos();
                Vec3 blockWorld = ship.rotatedWorldPoint(lp.getX() + 0.5, lp.getY() + 0.5, lp.getZ() + 0.5);
                if (blockWorld.distanceToSqr(player.position()) <= 64.0) { // 8 格內
                    ship.breakLocalBlock(player, lp);
                }
            }
        });
    }
}
