package com.github.nalamodikk.common.network.packet.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** C2S：玩家在二階段變身演出中按跳過鍵，請求 server 結束變身過場。 */
public record Phase2SkipPacket() implements CustomPacketPayload {

    public static final Phase2SkipPacket INSTANCE = new Phase2SkipPacket();

    public static final Type<Phase2SkipPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "phase2_skip"));

    public static final StreamCodec<FriendlyByteBuf, Phase2SkipPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, Phase2SkipPacket::handle);
    }

    private static void handle(Phase2SkipPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!sl.dimension().equals(ModDimensions.VOID_MIRROR)) return;
            for (PlayerCloneEntity clone : sl.getEntitiesOfClass(PlayerCloneEntity.class,
                    new AABB(BlockPos.ZERO).inflate(300),
                    c -> c.getSourceUUID().map(player.getUUID()::equals).orElse(false))) {
                clone.skipPhase2Transition();
            }
        });
    }
}
