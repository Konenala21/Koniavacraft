package com.github.nalamodikk.narasystem.nara.network.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import com.github.nalamodikk.narasystem.nara.network.server.NaraCreeperSpawnPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// 客戶端 → 伺服器：請求生成苦力怕彩蛋
public record NaraCreeperPunishPacket() implements CustomPacketPayload {

    public static final Type<NaraCreeperPunishPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_creeper_punish"));

    public static final StreamCodec<FriendlyByteBuf, NaraCreeperPunishPacket> STREAM_CODEC =
            StreamCodec.unit(new NaraCreeperPunishPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, NaraCreeperPunishPacket::handle);
    }

    private static void handle(NaraCreeperPunishPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.isCreative() || player.isSpectator()) {
                NaraServerEvents.schedulePunishmentDialogue(player.getUUID(), 40);
                return;
            }
            NaraCreeperSpawnPacket.spawnWardenNearPlayer(player);
            NaraServerEvents.schedulePunishmentDialogue(player.getUUID(), 100);
        });
    }
}
