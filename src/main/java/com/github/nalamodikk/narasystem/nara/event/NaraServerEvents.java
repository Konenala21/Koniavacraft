package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableMenu;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.core.WandCoreItem;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTutorialPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraCloseDialoguePacket;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.research.ResearchGate;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
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

    // Punishment 子系統的 state 已搬到 NaraPunishmentManager
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
    private static final Map<UUID, Integer> pendingManaGenCraft = new HashMap<>();
    private static final Map<UUID, Integer> pendingManaGenPlacement = new HashMap<>();
    // First research complete + first altar formed
    private static final Map<UUID, Integer> pendingFirstResearch = new HashMap<>();
    private static final Map<UUID, Integer> pendingFirstAltarFormed = new HashMap<>();
    private static final int FIRST_RESEARCH_DELAY = 10;
    private static final int FIRST_ALTAR_FORMED_DELAY = 10;
    private static final int GUI_CLOSE_TIMEOUT_TICKS = 200;
    // Wand rod craft tutorial
    private static final Map<UUID, Integer> pendingWandRodCraft = new HashMap<>();
    private static final Set<UUID> watchingForWandCore = new HashSet<>();
    private static int wandCoreCheckTimer = 0;
    // Machine placement tutorials (fire when player places + opens machine GUI, not on craft GUI close)
    private static final Set<UUID> pendingManaGrinderPlaced = new HashSet<>();
    private static final Set<UUID> pendingManaInfuserPlaced = new HashSet<>();
    private static final Map<UUID, Integer> pendingManaCraftingPlaced = new HashMap<>();
    private static final Set<UUID> pendingManaCraftingWaitForPlace = new HashSet<>();
    private static final Set<UUID> pendingManaDeployerPlaced = new HashSet<>();
    private static final Set<UUID> pendingManaChargerPlaced = new HashSet<>();
    private static final Set<UUID> pendingSolarCollectorPlaced = new HashSet<>();
    // Dev test tutorial (ghost block)
    // Phase 1: block placed, waiting for chunk update to reach client before opening GUI
    // Ghost tutorial 子系統的 state 已搬到 NaraGhostTutorialManager

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NaraPunishmentManager.clearAll();
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
        pendingManaGrinderPlaced.clear();
        pendingManaInfuserPlaced.clear();
        pendingManaCraftingPlaced.clear();
        pendingManaCraftingWaitForPlace.clear();
        pendingManaDeployerPlaced.clear();
        pendingManaChargerPlaced.clear();
        pendingSolarCollectorPlaced.clear();
        NaraGhostTutorialManager.clearAll(event.getServer());
        ArcaneConduitBlockEntity.clearAllStaticCachesGracefully();
        ArcaneConduitBlock.clearStaticCaches();
        ResearchGate.clearCache();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        NaraPunishmentManager.clearForPlayer(uuid);
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
        pendingManaGrinderPlaced.remove(uuid);
        pendingManaInfuserPlaced.remove(uuid);
        pendingManaCraftingPlaced.remove(uuid);
        pendingManaCraftingWaitForPlace.remove(uuid);
        pendingManaDeployerPlaced.remove(uuid);
        pendingManaChargerPlaced.remove(uuid);
        pendingSolarCollectorPlaced.remove(uuid);
        NaraGhostTutorialManager.clearForPlayer(player);
    }

    public static void scheduleFirstScanTutorial(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstScanTutorial.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_SCAN)) return;
        pendingFirstScanTutorial.put(uuid, FIRST_SCAN_DELAY);
    }

    public static void scheduleFirstWatchOpenTutorial(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstWatchOpenTutorial.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_WATCH_OPEN)) return;
        pendingFirstWatchOpenTutorial.put(uuid, FIRST_WATCH_OPEN_DELAY);
    }

    public static void scheduleFirstResearchTutorial(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstResearch.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_RESEARCH)) return;
        pendingFirstResearch.put(uuid, FIRST_RESEARCH_DELAY);
    }

    public static void scheduleFirstAltarFormedTutorial(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstAltarFormed.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_ALTAR_FORMED)) return;
        pendingFirstAltarFormed.put(uuid, FIRST_ALTAR_FORMED_DELAY);
    }

    public static void schedulePunishmentDialogue(UUID playerUUID, int delayTicks) {
        NaraPunishmentManager.schedule(playerUUID, delayTicks);
    }

    public static void cancelPunishmentState(UUID uuid) {
        NaraPunishmentManager.cancel(uuid);
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
            pendingManaGenCraft.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }

        if (event.getCrafting().getItem() instanceof WandRodItem
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.WAND_ROD_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.WAND_ROD_CRAFT);
            savedData.setDirty();
            pendingWandRodCraft.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_GRINDER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GRINDER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_GRINDER_CRAFT);
            savedData.setDirty();
            pendingManaGrinderPlaced.add(sp.getUUID());
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_INFUSER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_INFUSER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_INFUSER_CRAFT);
            savedData.setDirty();
            pendingManaInfuserPlaced.add(sp.getUUID());
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT);
            savedData.setDirty();
            pendingManaCraftingPlaced.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_DEPLOYER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_DEPLOYER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_DEPLOYER_CRAFT);
            savedData.setDirty();
            pendingManaDeployerPlaced.add(sp.getUUID());
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_CHARGER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CHARGER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_CHARGER_CRAFT);
            savedData.setDirty();
            pendingManaChargerPlaced.add(sp.getUUID());
        }

        if (event.getCrafting().getItem() == ModBlocks.SOLAR_MANA_COLLECTOR.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT);
            savedData.setDirty();
            pendingSolarCollectorPlaced.add(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();

        if (event.getPlacedBlock().is(ModBlocks.MANA_GENERATOR.get())
                && pendingManaGenPlacement.containsKey(player.getUUID())) {
            if (level.getBlockEntity(pos) instanceof ManaGeneratorBlockEntity be) {
                pendingManaGenPlacement.remove(player.getUUID());
                player.openMenu(
                        new SimpleMenuProvider(be, Component.translatable("block.koniava.mana_generator")),
                        pos
                );
                var savedData = ResearchSavedData.get(level);
                if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.MANA_GEN_PLACED)) {
                    NaraTutorialPacket.send(player, NaraTutorialFlow.MANA_GEN_PLACED);
                    savedData.setDirty();
                }
            }
        }

        checkMachinePlaced(player, level, pos, event.getPlacedBlock(),
                ModBlocks.MANA_GRINDER.get(), pendingManaGrinderPlaced, NaraTutorialFlow.MANA_GRINDER_CRAFT);
        checkMachinePlaced(player, level, pos, event.getPlacedBlock(),
                ModBlocks.MANA_INFUSER.get(), pendingManaInfuserPlaced, NaraTutorialFlow.MANA_INFUSER_CRAFT);
        checkMachinePlaced(player, level, pos, event.getPlacedBlock(),
                ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get(), pendingManaCraftingWaitForPlace, NaraTutorialFlow.MANA_CRAFTING_PLACED);
        checkMachinePlaced(player, level, pos, event.getPlacedBlock(),
                ModBlocks.MANA_DEPLOYER.get(), pendingManaDeployerPlaced, NaraTutorialFlow.MANA_DEPLOYER_CRAFT);
        checkMachinePlaced(player, level, pos, event.getPlacedBlock(),
                ModBlocks.MANA_CHARGER.get(), pendingManaChargerPlaced, NaraTutorialFlow.MANA_CHARGER_CRAFT);
        checkMachinePlaced(player, level, pos, event.getPlacedBlock(),
                ModBlocks.SOLAR_MANA_COLLECTOR.get(), pendingSolarCollectorPlaced, NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT);
    }

    private static void checkMachinePlaced(ServerPlayer player, ServerLevel level, BlockPos pos,
                                            BlockState placed, net.minecraft.world.level.block.Block target,
                                            Set<UUID> pendingSet, String tutorialId) {
        if (!placed.is(target)) return;
        if (!pendingSet.remove(player.getUUID())) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MenuProvider mp)) return;
        player.openMenu(mp, pos);
        var savedData = ResearchSavedData.get(level);
        if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(tutorialId)) {
            NaraTutorialPacket.send(player, tutorialId);
            savedData.setDirty();
        }
    }

    public static void restorePendingTutorials(ServerPlayer player) {
        UUID uuid = player.getUUID();
        var savedData = ResearchSavedData.get(player.serverLevel());
        var knowledge = savedData.getOrCreate(uuid);
        var pending = knowledge.getPendingTutorials();

        if (pending.contains(NaraTutorialFlow.RESEARCH_TABLE)) {
            pendingResearchTableTutorial.add(uuid);
            tutorialLoginDelay.put(uuid, LOGIN_TUTORIAL_DELAY);
        }
        if (pending.contains(NaraTutorialFlow.MANA_GEN_CRAFT))
            pendingManaGenCraft.put(uuid, GUI_CLOSE_TIMEOUT_TICKS);
        if (pending.contains(NaraTutorialFlow.WAND_ROD_CRAFT))
            pendingWandRodCraft.put(uuid, GUI_CLOSE_TIMEOUT_TICKS);
        if (pending.contains(NaraTutorialFlow.MANA_GRINDER_CRAFT))
            pendingManaGrinderPlaced.add(uuid);
        if (pending.contains(NaraTutorialFlow.MANA_INFUSER_CRAFT))
            pendingManaInfuserPlaced.add(uuid);
        if (pending.contains(NaraTutorialFlow.MANA_CRAFTING_CRAFT))
            pendingManaCraftingPlaced.put(uuid, GUI_CLOSE_TIMEOUT_TICKS);
        if (pending.contains(NaraTutorialFlow.MANA_DEPLOYER_CRAFT))
            pendingManaDeployerPlaced.add(uuid);
        if (pending.contains(NaraTutorialFlow.MANA_CHARGER_CRAFT))
            pendingManaChargerPlaced.add(uuid);
        if (pending.contains(NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT))
            pendingSolarCollectorPlaced.add(uuid);

        // Craft tutorial fired but player hasn't placed the generator yet
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GEN_CRAFT)
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GEN_PLACED)) {
            pendingManaGenPlacement.put(uuid, GUI_CLOSE_TIMEOUT_TICKS + 1);
        }

        // Craft tutorial fired but player hasn't placed the mana crafting table yet
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT)
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CRAFTING_PLACED)) {
            pendingManaCraftingWaitForPlace.add(uuid);
        }

        // Wand rod tutorial fired but player hasn't obtained a core yet
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.WAND_ROD_CRAFT)
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.WAND_ROD_GOT_CORE)) {
            boolean hasCore = player.getInventory().items.stream()
                    .anyMatch(s -> s.getItem() instanceof WandCoreItem);
            if (hasCore) {
                if (knowledge.markTutorialSeen(NaraTutorialFlow.WAND_ROD_GOT_CORE)) {
                    NaraTutorialPacket.send(player, NaraTutorialFlow.WAND_ROD_GOT_CORE);
                    savedData.setDirty();
                }
            } else {
                watchingForWandCore.add(uuid);
            }
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
            Iterator<Map.Entry<UUID, Integer>> wit = pendingWandRodCraft.entrySet().iterator();
            while (wit.hasNext()) {
                Map.Entry<UUID, Integer> entry = wit.next();
                UUID uuid = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) { wit.remove(); continue; }
                int remaining = entry.getValue() - 1;
                boolean timeout = remaining <= 0;
                if (timeout || player.containerMenu == player.inventoryMenu) {
                    wit.remove();
                    var savedData = ResearchSavedData.get(player.serverLevel());
                    if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.WAND_ROD_CRAFT)) {
                        NaraTutorialPacket.send(player, NaraTutorialFlow.WAND_ROD_CRAFT);
                        boolean hasCore = player.getInventory().items.stream()
                                .anyMatch(s -> s.getItem() instanceof WandCoreItem);
                        if (!hasCore) watchingForWandCore.add(uuid);
                        savedData.setDirty();
                    }
                } else {
                    entry.setValue(remaining);
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
            Iterator<Map.Entry<UUID, Integer>> git = pendingManaGenCraft.entrySet().iterator();
            while (git.hasNext()) {
                Map.Entry<UUID, Integer> entry = git.next();
                UUID uuid = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) { git.remove(); continue; }
                int remaining = entry.getValue() - 1;
                boolean timeout = remaining <= 0;
                if (timeout || player.containerMenu == player.inventoryMenu) {
                    git.remove();
                    var savedData = ResearchSavedData.get(player.serverLevel());
                    if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.MANA_GEN_CRAFT)) {
                        NaraTutorialPacket.send(player, NaraTutorialFlow.MANA_GEN_CRAFT);
                        savedData.setDirty();
                        pendingManaGenPlacement.put(uuid, GUI_CLOSE_TIMEOUT_TICKS + 1);
                    }
                } else {
                    entry.setValue(remaining);
                }
            }
        }


        if (!pendingManaCraftingPlaced.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> mcit = pendingManaCraftingPlaced.entrySet().iterator();
            while (mcit.hasNext()) {
                Map.Entry<UUID, Integer> entry = mcit.next();
                UUID uuid = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) { mcit.remove(); continue; }
                int remaining = entry.getValue() - 1;
                boolean timeout = remaining <= 0;
                if (timeout || player.containerMenu == player.inventoryMenu) {
                    mcit.remove();
                    var savedData = ResearchSavedData.get(player.serverLevel());
                    if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.MANA_CRAFTING_CRAFT)) {
                        NaraTutorialPacket.send(player, NaraTutorialFlow.MANA_CRAFTING_CRAFT);
                        savedData.setDirty();
                        pendingManaCraftingWaitForPlace.add(uuid);
                    }
                } else {
                    entry.setValue(remaining);
                }
            }
        }

        // Expire the mana generator placement watch after timeout (no tutorial fired, just cleanup)
        if (!pendingManaGenPlacement.isEmpty()) {
            pendingManaGenPlacement.entrySet().removeIf(e -> e.setValue(e.getValue() - 1) <= 0);
        }

        NaraGhostTutorialManager.tick(event);

        NaraPunishmentManager.tick(event);

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
        NaraPunishmentManager.handleDeath(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NaraPunishmentManager.handleRespawn(player);
    }

    public static void scheduleTestTutorial(ServerPlayer player, String tutorialId) {
        NaraGhostTutorialManager.schedule(player, tutorialId);
    }

    public static boolean isGhostBlock(BlockPos pos) {
        return NaraGhostTutorialManager.isGhostBlock(pos);
    }

    private static void tickWhenGuiClosed(Map<UUID, Integer> pending, String tutorialId,
                                           ServerTickEvent.Post event) {
        if (pending.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID uuid = entry.getKey();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) { it.remove(); continue; }
            int remaining = entry.getValue() - 1;
            boolean timeout = remaining <= 0;
            if (timeout || player.containerMenu == player.inventoryMenu) {
                it.remove();
                var savedData = ResearchSavedData.get(player.serverLevel());
                if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(tutorialId)) {
                    NaraTutorialPacket.send(player, tutorialId);
                    savedData.setDirty();
                }
            } else {
                entry.setValue(remaining);
            }
        }
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
                if (player != null) {
                    var savedData = ResearchSavedData.get(player.serverLevel());
                    if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(tutorialId)) {
                        NaraTutorialPacket.send(player, tutorialId);
                        savedData.setDirty();
                    }
                }
            } else {
                entry.setValue(remaining);
            }
        }
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

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getSlot() != EquipmentSlot.FEET) return;
        if (!(event.getTo().getItem() instanceof ManaSprintBootsItem)) return;
        // 只在真正「換上」靴子時觸發；靴子魔力每 tick 變動也會發 equip change，要忽略
        if (event.getFrom().getItem() instanceof ManaSprintBootsItem) return;
        var savedData = ResearchSavedData.get(player.serverLevel());
        if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.BOOTS_EQUIP)) {
            NaraTutorialPacket.send(player, NaraTutorialFlow.BOOTS_EQUIP);
            savedData.setDirty();
        }
    }
}
