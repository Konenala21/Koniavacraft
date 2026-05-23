package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableMenu;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    // Machine craft tutorials
    private static final Map<UUID, Integer> pendingManaGrinderCraft = new HashMap<>();
    private static final Map<UUID, Integer> pendingManaInfuserCraft = new HashMap<>();
    private static final Map<UUID, Integer> pendingManaCraftingCraft = new HashMap<>();
    // Dev test tutorial (ghost block)
    private static final Map<UUID, Integer> pendingTestTutorial = new HashMap<>();
    private static final Map<UUID, String> pendingTestTutorialId = new HashMap<>();
    private static final Map<UUID, BlockPos> ghostBlocks = new HashMap<>();

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
        pendingManaGrinderCraft.clear();
        pendingManaInfuserCraft.clear();
        pendingManaCraftingCraft.clear();
        for (Map.Entry<UUID, BlockPos> e : ghostBlocks.entrySet()) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null) p.serverLevel().removeBlock(e.getValue(), false);
        }
        ghostBlocks.clear();
        pendingTestTutorial.clear();
        pendingTestTutorialId.clear();
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
        pendingManaGrinderCraft.remove(uuid);
        pendingManaInfuserCraft.remove(uuid);
        pendingManaCraftingCraft.remove(uuid);
        BlockPos ghostPos = ghostBlocks.remove(uuid);
        if (ghostPos != null) player.serverLevel().removeBlock(ghostPos, false);
        pendingTestTutorial.remove(uuid);
        pendingTestTutorialId.remove(uuid);
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
            pendingManaGrinderCraft.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_INFUSER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_INFUSER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_INFUSER_CRAFT);
            savedData.setDirty();
            pendingManaInfuserCraft.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }

        if (event.getCrafting().getItem() == ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT);
            savedData.setDirty();
            pendingManaCraftingCraft.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getPlacedBlock().is(ModBlocks.MANA_GENERATOR.get())) return;
        if (pendingManaGenPlacement.remove(player.getUUID()) == null) return;

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
                        pendingManaGenPlacement.put(uuid, GUI_CLOSE_TIMEOUT_TICKS);
                    }
                } else {
                    entry.setValue(remaining);
                }
            }
        }

        tickWhenGuiClosed(pendingManaGrinderCraft, NaraTutorialFlow.MANA_GRINDER_CRAFT, event);
        tickWhenGuiClosed(pendingManaInfuserCraft, NaraTutorialFlow.MANA_INFUSER_CRAFT, event);
        tickWhenGuiClosed(pendingManaCraftingCraft, NaraTutorialFlow.MANA_CRAFTING_CRAFT, event);

        if (!pendingTestTutorial.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> tit = pendingTestTutorial.entrySet().iterator();
            while (tit.hasNext()) {
                Map.Entry<UUID, Integer> entry = tit.next();
                UUID uuid = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) {
                    tit.remove();
                    pendingTestTutorialId.remove(uuid);
                    BlockPos gp = ghostBlocks.remove(uuid);
                    if (gp != null) {
                        for (ServerLevel lvl : event.getServer().getAllLevels())
                            if (!lvl.isEmptyBlock(gp)) { lvl.removeBlock(gp, false); break; }
                    }
                    continue;
                }
                int remaining = entry.getValue() - 1;
                boolean timeout = remaining <= 0;
                if (timeout || player.containerMenu == player.inventoryMenu) {
                    tit.remove();
                    String tid = pendingTestTutorialId.remove(uuid);
                    BlockPos gp = ghostBlocks.remove(uuid);
                    if (gp != null) player.serverLevel().removeBlock(gp, false);
                    if (tid != null) NaraTutorialPacket.send(player, tid);
                } else {
                    entry.setValue(remaining);
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

    // Tutorials that fire immediately when the machine GUI opens (not on close)
    private static final Set<String> FIRE_ON_GUI_OPEN = Set.of(
            NaraTutorialFlow.MANA_GEN_PLACED,
            NaraTutorialFlow.MANA_GEN_CRAFT,
            NaraTutorialFlow.MANA_GRINDER_CRAFT,
            NaraTutorialFlow.MANA_INFUSER_CRAFT,
            NaraTutorialFlow.MANA_CRAFTING_CRAFT,
            NaraTutorialFlow.RESEARCH_TABLE,
            NaraTutorialFlow.FIRST_RESEARCH
    );

    public static void scheduleTestTutorial(ServerPlayer player, String tutorialId) {
        BlockState ghostState = getGhostBlockState(tutorialId);
        if (ghostState == null) {
            NaraTutorialPacket.send(player, tutorialId);
            return;
        }
        BlockPos ghostPos = findAirPos(player);
        if (ghostPos == null) {
            NaraTutorialPacket.send(player, tutorialId);
            return;
        }
        ServerLevel level = player.serverLevel();
        level.setBlock(ghostPos, ghostState, 3);
        BlockEntity be = level.getBlockEntity(ghostPos);
        boolean opened = false;
        if (be instanceof MenuProvider mp) {
            player.openMenu(mp, ghostPos);
            opened = true;
        } else if (be instanceof ResearchTableBlockEntity rbe) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ResearchTableMenu(id, inv, rbe),
                    Component.translatable(ghostState.getBlock().getDescriptionId())), ghostPos);
            opened = true;
        }
        if (opened) {
            UUID uuid = player.getUUID();
            ghostBlocks.put(uuid, ghostPos);
            pendingTestTutorial.put(uuid, GUI_CLOSE_TIMEOUT_TICKS);
            if (FIRE_ON_GUI_OPEN.contains(tutorialId)) {
                NaraTutorialPacket.send(player, tutorialId);
                pendingTestTutorialId.put(uuid, null); // null = cleanup only, already fired
            } else {
                pendingTestTutorialId.put(uuid, tutorialId);
            }
        } else {
            level.removeBlock(ghostPos, false);
            NaraTutorialPacket.send(player, tutorialId);
        }
    }

    private static BlockState getGhostBlockState(String id) {
        return switch (id) {
            case NaraTutorialFlow.RESEARCH_TABLE, NaraTutorialFlow.FIRST_RESEARCH ->
                    ModBlocks.RESEARCH_TABLE.get().defaultBlockState();
            case NaraTutorialFlow.MANA_GEN_CRAFT, NaraTutorialFlow.MANA_GEN_PLACED ->
                    ModBlocks.MANA_GENERATOR.get().defaultBlockState();
            case NaraTutorialFlow.MANA_GRINDER_CRAFT ->
                    ModBlocks.MANA_GRINDER.get().defaultBlockState();
            case NaraTutorialFlow.MANA_INFUSER_CRAFT ->
                    ModBlocks.MANA_INFUSER.get().defaultBlockState();
            case NaraTutorialFlow.MANA_CRAFTING_CRAFT ->
                    ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get().defaultBlockState();
            default -> null;
        };
    }

    private static BlockPos findAirPos(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos base = player.blockPosition().above(2);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                if (level.isEmptyBlock(pos)) return pos;
            }
        }
        return null;
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
