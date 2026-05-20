package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTutorialPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraCloseDialoguePacket;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.research.ResearchGate;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class NaraServerEvents {

    private static final Map<UUID, Integer> pendingPunishmentDialogue = new HashMap<>();
    private static final Set<UUID> awaitingRespawn = new HashSet<>();
    private static final Set<UUID> naraPunishmentActive = new HashSet<>();
    private static final Map<UUID, Integer> pendingWardenDespawn = new HashMap<>();
    // Players who crafted a research table and are waiting for their GUI to close
    private static final Set<UUID> pendingResearchTableTutorial = new HashSet<>();
    // Login restore delay: prevents tutorial firing within the first few seconds of login
    private static final Map<UUID, Integer> tutorialLoginDelay = new HashMap<>();
    private static final int LOGIN_TUTORIAL_DELAY = 60; // 3 seconds

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        pendingPunishmentDialogue.clear();
        awaitingRespawn.clear();
        naraPunishmentActive.clear();
        pendingWardenDespawn.clear();
        pendingResearchTableTutorial.clear();
        tutorialLoginDelay.clear();
        ArcaneConduitBlockEntity.clearAllStaticCachesGracefully();
        ArcaneConduitBlock.clearStaticCaches();
        ResearchGate.clearCache();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        pendingPunishmentDialogue.remove(uuid);
        naraPunishmentActive.remove(uuid);
        awaitingRespawn.remove(uuid);
        pendingResearchTableTutorial.remove(uuid);
        tutorialLoginDelay.remove(uuid);
    }

    public static void schedulePunishmentDialogue(UUID playerUUID, int delayTicks) {
        pendingPunishmentDialogue.put(playerUUID, delayTicks);
        naraPunishmentActive.add(playerUUID);
    }

    public static void cancelPunishmentState(UUID uuid) {
        pendingPunishmentDialogue.remove(uuid);
        naraPunishmentActive.remove(uuid);
        awaitingRespawn.remove(uuid);
    }

    public static void scheduleWardenDespawn(UUID wardenUUID, int delayTicks) {
        pendingWardenDespawn.put(wardenUUID, delayTicks);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getCrafting().getItem() != ModBlocks.RESEARCH_TABLE.get().asItem()) return;

        var savedData = ResearchSavedData.get(sp.serverLevel());
        var knowledge = savedData.getOrCreate(sp.getUUID());
        if (!knowledge.hasSeenTutorial(NaraTutorialFlow.RESEARCH_TABLE)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.RESEARCH_TABLE);
            savedData.setDirty();
            pendingResearchTableTutorial.add(sp.getUUID());
        }
    }

    public static void restorePendingTutorials(ServerPlayer player) {
        var knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        if (knowledge.getPendingTutorials().contains(NaraTutorialFlow.RESEARCH_TABLE)) {
            pendingResearchTableTutorial.add(player.getUUID());
            tutorialLoginDelay.put(player.getUUID(), LOGIN_TUTORIAL_DELAY);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Tick down login delays before checking pending tutorials
        tutorialLoginDelay.entrySet().removeIf(e -> e.setValue(e.getValue() - 1) <= 0);

        // Research table tutorial: fire once the player closes their crafting GUI
        if (!pendingResearchTableTutorial.isEmpty()) {
            Iterator<UUID> pit = pendingResearchTableTutorial.iterator();
            while (pit.hasNext()) {
                UUID uuid = pit.next();
                if (tutorialLoginDelay.containsKey(uuid)) continue;
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) {
                    pit.remove();
                    continue;
                }
                if (player.containerMenu == player.inventoryMenu) {
                    pit.remove();
                    var savedData = ResearchSavedData.get(player.serverLevel());
                    if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.RESEARCH_TABLE)) {
                        NaraTutorialPacket.send(player, NaraTutorialFlow.RESEARCH_TABLE);
                        savedData.setDirty();
                    }
                }
            }
        }

        Iterator<Map.Entry<UUID, Integer>> it = pendingPunishmentDialogue.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    sendPunishmentDialogue(player);
                } else {
                    awaitingRespawn.add(entry.getKey());
                }
            } else {
                entry.setValue(remaining);
            }
        }

        Iterator<Map.Entry<UUID, Integer>> wi = pendingWardenDespawn.entrySet().iterator();
        while (wi.hasNext()) {
            Map.Entry<UUID, Integer> entry = wi.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                wi.remove();
                UUID wardenId = entry.getKey();
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    Entity entity = level.getEntity(wardenId);
                    if (entity != null) { entity.discard(); break; }
                }
            } else {
                entry.setValue(remaining);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!naraPunishmentActive.contains(uuid)) return;
        PacketDistributor.sendToPlayer(player, new NaraCloseDialoguePacket());
        pendingPunishmentDialogue.remove(uuid);
        awaitingRespawn.add(uuid);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!awaitingRespawn.remove(uuid)) return;
        pendingPunishmentDialogue.put(uuid, 20);
    }

    private static void sendPunishmentDialogue(ServerPlayer player) {
        naraPunishmentActive.remove(player.getUUID());
        grantAdvancement(player, "nara_ignored");
        PacketDistributor.sendToPlayer(player, new NaraStartDialoguePacket.Punishment());
    }

    private static void grantAdvancement(ServerPlayer player, String id) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, id);
        var holder = player.server.getAdvancements().get(loc);
        if (holder != null) player.getAdvancements().award(holder, "obtained");
    }

    public static void grantWelcomeAdvancement(ServerPlayer player) {
        grantAdvancement(player, "nara_welcome");
    }

    public static void grantAngryAdvancement(ServerPlayer player) {
        grantAdvancement(player, "nara_angry");
    }
}
