package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.core.WandCoreItem;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTutorialPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraCloseDialoguePacket;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.research.ResearchGate;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
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
    private static final Set<UUID> pendingResearchTableTutorial = new HashSet<>();
    private static final Map<UUID, Integer> tutorialLoginDelay = new HashMap<>();
    private static final int LOGIN_TUTORIAL_DELAY = 60;
    // Delayed tutorial packets: UUID -> countdown ticks -> tutorial id
    private static final Map<UUID, Integer> pendingFirstScanTutorial = new HashMap<>();
    private static final Map<UUID, Integer> pendingFirstWatchOpenTutorial = new HashMap<>();
    private static final int FIRST_SCAN_DELAY = 10;
    private static final int FIRST_WATCH_OPEN_DELAY = 5;
    // Mana generator tutorial: step1=craft, step2=placement
    private static final Set<UUID> pendingManaGenCraft = new HashSet<>();
    private static final Set<UUID> pendingManaGenPlacement = new HashSet<>();
    // First research complete + first altar formed
    private static final Map<UUID, Integer> pendingFirstResearch = new HashMap<>();
    private static final Map<UUID, Integer> pendingFirstAltarFormed = new HashMap<>();
    private static final int FIRST_RESEARCH_DELAY = 10;
    private static final int FIRST_ALTAR_FORMED_DELAY = 10;
    // Wand rod craft tutorial
    private static final Set<UUID> pendingWandRodCraft = new HashSet<>();
    private static final Set<UUID> watchingForWandCore = new HashSet<>();
    private static int wandCoreCheckTimer = 0;

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        pendingPunishmentDialogue.clear();
        awaitingRespawn.clear();
        naraPunishmentActive.clear();
        pendingWardenDespawn.clear();
        pendingResearchTableTutorial.clear();
        tutorialLoginDelay.clear();
        pendingFirstScanTutorial.clear();
        pendingFirstWatchOpenTutorial.clear();
        pendingManaGenCraft.clear();
        pendingManaGenPlacement.clear();
        pendingFirstResearch.clear();
        pendingFirstAltarFormed.clear();
        pendingWandRodCraft.clear();
        watchingForWandCore.clear();
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
        pendingFirstScanTutorial.remove(uuid);
        pendingFirstWatchOpenTutorial.remove(uuid);
        pendingManaGenCraft.remove(uuid);
        pendingManaGenPlacement.remove(uuid);
        pendingFirstResearch.remove(uuid);
        pendingFirstAltarFormed.remove(uuid);
        pendingWandRodCraft.remove(uuid);
        watchingForWandCore.remove(uuid);
    }

    public static void scheduleFirstScanTutorial(ServerPlayer player) {
        var knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.FIRST_SCAN)) return;
        knowledge.markTutorialSeen(NaraTutorialFlow.FIRST_SCAN);
        ResearchSavedData.get(player.serverLevel()).setDirty();
        pendingFirstScanTutorial.put(player.getUUID(), FIRST_SCAN_DELAY);
    }

    public static void scheduleFirstWatchOpenTutorial(ServerPlayer player) {
        var knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.FIRST_WATCH_OPEN)) return;
        knowledge.markTutorialSeen(NaraTutorialFlow.FIRST_WATCH_OPEN);
        ResearchSavedData.get(player.serverLevel()).setDirty();
        pendingFirstWatchOpenTutorial.put(player.getUUID(), FIRST_WATCH_OPEN_DELAY);
    }

    public static void scheduleFirstResearchTutorial(ServerPlayer player) {
        var savedData = ResearchSavedData.get(player.serverLevel());
        var knowledge = savedData.getOrCreate(player.getUUID());
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.FIRST_RESEARCH)) return;
        knowledge.markTutorialSeen(NaraTutorialFlow.FIRST_RESEARCH);
        savedData.setDirty();
        pendingFirstResearch.put(player.getUUID(), FIRST_RESEARCH_DELAY);
    }

    public static void scheduleFirstAltarFormedTutorial(ServerPlayer player) {
        var savedData = ResearchSavedData.get(player.serverLevel());
        var knowledge = savedData.getOrCreate(player.getUUID());
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.FIRST_ALTAR_FORMED)) return;
        knowledge.markTutorialSeen(NaraTutorialFlow.FIRST_ALTAR_FORMED);
        savedData.setDirty();
        pendingFirstAltarFormed.put(player.getUUID(), FIRST_ALTAR_FORMED_DELAY);
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
        var savedData = ResearchSavedData.get(sp.serverLevel());
        var knowledge = savedData.getOrCreate(sp.getUUID());

        if (event.getCrafting().getItem() == ModBlocks.RESEARCH_TABLE.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.RESEARCH_TABLE)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.RESEARCH_TABLE);
            savedData.setDirty();
            pendingResearchTableTutorial.add(sp.getUUID());
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_GENERATOR.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GEN_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_GEN_CRAFT);
            savedData.setDirty();
            pendingManaGenCraft.add(sp.getUUID());
        }

        if (event.getCrafting().getItem() instanceof WandRodItem
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.WAND_ROD_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.WAND_ROD_CRAFT);
            savedData.setDirty();
            pendingWandRodCraft.add(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getPlacedBlock().is(ModBlocks.MANA_GENERATOR.get())) return;
        if (!pendingManaGenPlacement.remove(player.getUUID())) return;

        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof ManaGeneratorBlockEntity be)) return;

        player.openMenu(
                new SimpleMenuProvider(be, Component.translatable("block.koniava.mana_generator")),
                event.getPos()
        );
        var savedData = ResearchSavedData.get(level);
        if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.MANA_GEN_PLACED)) {
            NaraTutorialPacket.send(player, NaraTutorialFlow.MANA_GEN_PLACED);
            savedData.setDirty();
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

        tickDelayedTutorial(pendingFirstScanTutorial, NaraTutorialFlow.FIRST_SCAN, event);
        tickDelayedTutorial(pendingFirstWatchOpenTutorial, NaraTutorialFlow.FIRST_WATCH_OPEN, event);
        tickDelayedTutorial(pendingFirstResearch, NaraTutorialFlow.FIRST_RESEARCH, event);
        tickDelayedTutorial(pendingFirstAltarFormed, NaraTutorialFlow.FIRST_ALTAR_FORMED, event);

        if (!pendingWandRodCraft.isEmpty()) {
            Iterator<UUID> wit = pendingWandRodCraft.iterator();
            while (wit.hasNext()) {
                UUID uuid = wit.next();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) { wit.remove(); continue; }
                if (player.containerMenu == player.inventoryMenu) {
                    wit.remove();
                    var savedData = ResearchSavedData.get(player.serverLevel());
                    if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.WAND_ROD_CRAFT)) {
                        boolean hasCore = player.getInventory().items.stream()
                                .anyMatch(s -> s.getItem() instanceof WandCoreItem);
                        String tutId = hasCore
                                ? NaraTutorialFlow.WAND_ROD_READY
                                : NaraTutorialFlow.WAND_ROD_NO_ITEMS;
                        NaraTutorialPacket.send(player, tutId);
                        if (!hasCore) watchingForWandCore.add(uuid);
                        savedData.setDirty();
                    }
                }
            }
        }

        if (!watchingForWandCore.isEmpty()) {
            wandCoreCheckTimer++;
            if (wandCoreCheckTimer >= 20) {
                wandCoreCheckTimer = 0;
                Iterator<UUID> cit = watchingForWandCore.iterator();
                while (cit.hasNext()) {
                    UUID uuid = cit.next();
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                    if (player == null) { cit.remove(); continue; }
                    boolean hasCore = player.getInventory().items.stream()
                            .anyMatch(s -> s.getItem() instanceof WandCoreItem);
                    if (hasCore) {
                        cit.remove();
                        var savedData = ResearchSavedData.get(player.serverLevel());
                        if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.WAND_ROD_GOT_CORE)) {
                            NaraTutorialPacket.send(player, NaraTutorialFlow.WAND_ROD_GOT_CORE);
                            savedData.setDirty();
                        }
                    }
                }
            }
        }

        if (!pendingManaGenCraft.isEmpty()) {
            Iterator<UUID> git = pendingManaGenCraft.iterator();
            while (git.hasNext()) {
                UUID uuid = git.next();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) { git.remove(); continue; }
                if (player.containerMenu == player.inventoryMenu) {
                    git.remove();
                    var savedData = ResearchSavedData.get(player.serverLevel());
                    if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.MANA_GEN_CRAFT)) {
                        NaraTutorialPacket.send(player, NaraTutorialFlow.MANA_GEN_CRAFT);
                        savedData.setDirty();
                        pendingManaGenPlacement.add(uuid);
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

    private static void tickDelayedTutorial(Map<UUID, Integer> pendingMap, String tutorialId,
                                             ServerTickEvent.Post event) {
        if (pendingMap.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> it = pendingMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) NaraTutorialPacket.send(player, tutorialId);
            } else {
                entry.setValue(remaining);
            }
        }
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
