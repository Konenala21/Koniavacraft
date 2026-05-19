package com.github.nalamodikk.common.network.packet.server.conduit;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PriorityUpdatePacket(BlockPos pos, Direction direction, int priority)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PriorityUpdatePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    KoniavacraftMod.MOD_ID, "priority_update"));

    public static final StreamCodec<ByteBuf, PriorityUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PriorityUpdatePacket::pos,
                    Direction.STREAM_CODEC, PriorityUpdatePacket::direction,
                    ByteBufCodecs.INT, PriorityUpdatePacket::priority,
                    PriorityUpdatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToServer(BlockPos pos, Direction direction, int priority) {
        PacketDistributor.sendToServer(new PriorityUpdatePacket(pos, direction, priority));
    }

    public static void handle(PriorityUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (net.minecraft.world.phys.Vec3.atCenterOf(packet.pos())
                        .distanceToSqr(player.position()) > 64.0) return;
                Level level = player.level();
                if (level.getBlockEntity(packet.pos()) instanceof ArcaneConduitBlockEntity conduit) {
                    conduit.setPriority(packet.direction(), packet.priority());

                    // 同步更新給其他玩家
                    level.sendBlockUpdated(packet.pos(), conduit.getBlockState(), conduit.getBlockState(), 3);
                }
            }
        });
    }

    public static void registerTo(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, PriorityUpdatePacket::handle);
    }
}