package com.github.nalamodikk.research.client;

import com.github.nalamodikk.research.network.AspectSyncPacket;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.network.WatchSyncPacket;
import com.github.nalamodikk.research.jei.AspectSynthesisJEIPlugin;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;

public class ResearchClientPayloadHandler {

    public static void handleKnowledgeSync(KnowledgeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientResearchCache.update(packet.discovered(), packet.completed(),
                    packet.availableOverrides(), packet.tier());
            AspectSynthesisJEIPlugin.refreshAspectIngredients();
        });
    }

    public static void handleAspectSync(AspectSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientResearchCache.updateAspects(packet.discovered());
            AspectSynthesisJEIPlugin.refreshAspectIngredients();
        });
    }

    public static void handleWatchSync(WatchSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientResearchCache.update(packet.discovered(), packet.completed(), packet.availableOverrides(), packet.tier());
            AspectSynthesisJEIPlugin.refreshAspectIngredients();
            Minecraft.getInstance().setScreen(
                    new NaraWatchScreen(new HashSet<>(packet.completed()), packet.tier()));
        });
    }
}
