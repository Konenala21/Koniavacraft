package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ManaUpdatePacketClient {
    public static void handle(ManaUpdatePacket packet, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        var handler = CapabilityUtils.getMana(level, packet.pos(), null);
        if (handler != null) {
            handler.setMana(packet.mana());
        }
    }
}
