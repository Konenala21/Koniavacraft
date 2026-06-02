package com.github.nalamodikk.research.client;

import com.github.nalamodikk.research.network.AspectSyncPacket;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.network.WatchSyncPacket;
import com.github.nalamodikk.research.jei.AspectSynthesisJEIPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ResearchClientPayloadHandler {

    /**
     * 跑一段會更新快取的動作,回報「已發現本源」集合有沒有變。只有變了才值得做昂貴的
     * JEI ingredient 重整 (refreshAspectIngredients),否則每次同步(開研究台/手錶/登入)都刷會卡。
     */
    private static boolean refreshJeiIfAspectsChanged(Runnable update) {
        Map<ResourceLocation, Integer> before = new HashMap<>(ClientResearchCache.getDiscoveredAspectCounts());
        update.run();
        boolean changed = !before.equals(ClientResearchCache.getDiscoveredAspectCounts());
        if (changed && ModList.get().isLoaded("jei")) AspectSynthesisJEIPlugin.refreshAspectIngredients();
        return changed;
    }

    public static void handleKnowledgeSync(KnowledgeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> refreshJeiIfAspectsChanged(() -> {
            ClientResearchCache.update(packet.discovered(), packet.completed(),
                    packet.availableOverrides(), packet.lockedResearch(), packet.tier());
            ClientResearchCache.updateScannedTargets(packet.scannedTargets());
        }));
    }

    public static void handleAspectSync(AspectSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 只有本源真的變了才刷 JEI:研究台每次開都送這個 sync,JEI ingredient 重整很重,
            // 每次開都刷會明顯卡頓。
            boolean changed = ClientResearchCache.updateAspects(packet.discovered());
            if (changed && ModList.get().isLoaded("jei")) AspectSynthesisJEIPlugin.refreshAspectIngredients();
        });
    }

    public static void handleWatchSync(WatchSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            refreshJeiIfAspectsChanged(() -> {
                ClientResearchCache.update(packet.discovered(), packet.completed(), packet.availableOverrides(),
                        packet.lockedResearch(), packet.tier());
                ClientResearchCache.updateScannedTargets(packet.scannedTargets());
            });
            Minecraft.getInstance().setScreen(
                    new NaraWatchScreen(new HashSet<>(packet.completed()), packet.tier()));
        });
    }
}
