package com.github.nalamodikk.narasystem.nara.network.client;

import com.github.nalamodikk.narasystem.nara.network.server.NaraSyncPacket;
import com.github.nalamodikk.narasystem.nara.util.NaraHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NaraSyncPacketclient {
    public static void handleNaraSync(NaraSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            NaraHelper.setBound(player, packet.isBound());

        }).exceptionally(e -> {
            context.disconnect(Component.translatable("message.koniava.nara.sync_error", e.getMessage()));
            return null;
        });
    }

}
