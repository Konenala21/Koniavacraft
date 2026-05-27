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

/** C2S：玩家在進場過場中按跳過鍵，請求 server 讓 boss 直接結束進場演出。 */
public record VoidMirrorSkipIntroPacket() implements CustomPacketPayload {

    public static final VoidMirrorSkipIntroPacket INSTANCE = new VoidMirrorSkipIntroPacket();

    public static final Type<VoidMirrorSkipIntroPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_mirror_skip_intro"));

    public static final StreamCodec<FriendlyByteBuf, VoidMirrorSkipIntroPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, VoidMirrorSkipIntroPacket::handle);
    }

    private static void handle(VoidMirrorSkipIntroPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!sl.dimension().equals(ModDimensions.VOID_MIRROR)) return;
            for (PlayerCloneEntity clone : sl.getEntitiesOfClass(PlayerCloneEntity.class,
                    new AABB(BlockPos.ZERO).inflate(300),
                    c -> c.getSourceUUID().map(player.getUUID()::equals).orElse(false))) {
                clone.skipIntro();
            }
        });
    }
}
