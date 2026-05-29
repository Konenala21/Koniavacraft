package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.core.WandCoreItem;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTutorialPacket;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 娜拉教程系統主要派發。所有非懲罰、非 ghost dev tutorial 的教程 state + handler 集中於此。
 *
 * <p>四類教程結構：
 * <ul>
 *     <li>延遲類（First Scan / First Watch Open / First Research / First Altar Formed）：純倒數，到時 fire packet</li>
 *     <li>GUI 關閉類（Research Table / Mana Gen Craft / Mana Crafting Craft / Wand Rod Craft）：等玩家關 GUI 或 timeout</li>
 *     <li>放置類（Mana Grinder / Infuser / Crafting / Deployer / Charger / Solar Collector / Mana Gen）：合成後等放置→自動開 GUI</li>
 *     <li>裝備類（Boots Equip）：穿上靴子立刻播</li>
 * </ul>
 *
 * <p>還含 Warden cleanup（schedule 延遲 discard 一隻 warden），跟教程不直接相關但 state 太小不另開 class。
 */
final class NaraTutorialManager {
    private NaraTutorialManager() {}

    private static final int LOGIN_TUTORIAL_DELAY = 60;
    private static final int FIRST_SCAN_DELAY = 10;
    private static final int FIRST_WATCH_OPEN_DELAY = 5;
    private static final int FIRST_RESEARCH_DELAY = 10;
    private static final int FIRST_ALTAR_FORMED_DELAY = 10;
    private static final int GUI_CLOSE_TIMEOUT_TICKS = 200;

    // GUI 關閉 / 延遲類
    private static final Set<UUID> pendingResearchTableTutorial = new HashSet<>();
    private static final Map<UUID, Integer> tutorialLoginDelay = new HashMap<>();
    private static final Map<UUID, Integer> pendingFirstScanTutorial = new HashMap<>();
    private static final Map<UUID, Integer> pendingFirstWatchOpenTutorial = new HashMap<>();
    private static final Map<UUID, Integer> pendingManaGenCraft = new HashMap<>();
    private static final Map<UUID, Integer> pendingManaGenPlacement = new HashMap<>();
    private static final Map<UUID, Integer> pendingFirstResearch = new HashMap<>();
    private static final Map<UUID, Integer> pendingFirstAltarFormed = new HashMap<>();
    // Wand rod
    private static final Map<UUID, Integer> pendingWandRodCraft = new HashMap<>();
    private static final Set<UUID> watchingForWandCore = new HashSet<>();
    private static int wandCoreCheckTimer = 0;
    // 機器放置類
    private static final Set<UUID> pendingManaGrinderPlaced = new HashSet<>();
    private static final Set<UUID> pendingManaInfuserPlaced = new HashSet<>();
    private static final Map<UUID, Integer> pendingManaCraftingPlaced = new HashMap<>();
    private static final Set<UUID> pendingManaCraftingWaitForPlace = new HashSet<>();
    private static final Set<UUID> pendingManaDeployerPlaced = new HashSet<>();
    private static final Set<UUID> pendingManaChargerPlaced = new HashSet<>();
    private static final Set<UUID> pendingSolarCollectorPlaced = new HashSet<>();
    // Warden cleanup
    private static final Map<UUID, Integer> pendingWardenDespawn = new HashMap<>();

    // ── schedule 入口 ───────────────────────────────────────────────────────

    static void scheduleFirstScan(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstScanTutorial.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_SCAN)) return;
        pendingFirstScanTutorial.put(uuid, FIRST_SCAN_DELAY);
    }

    static void scheduleFirstWatchOpen(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstWatchOpenTutorial.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_WATCH_OPEN)) return;
        pendingFirstWatchOpenTutorial.put(uuid, FIRST_WATCH_OPEN_DELAY);
    }

    static void scheduleFirstResearch(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstResearch.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_RESEARCH)) return;
        pendingFirstResearch.put(uuid, FIRST_RESEARCH_DELAY);
    }

    static void scheduleFirstAltarFormed(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingFirstAltarFormed.containsKey(uuid)) return;
        if (ResearchSavedData.get(player.serverLevel()).getOrCreate(uuid)
                .hasSeenTutorial(NaraTutorialFlow.FIRST_ALTAR_FORMED)) return;
        pendingFirstAltarFormed.put(uuid, FIRST_ALTAR_FORMED_DELAY);
    }

    static void scheduleWardenDespawn(UUID wardenUUID, int delayTicks) {
        pendingWardenDespawn.put(wardenUUID, delayTicks);
    }

    // ── 事件 handler 邏輯 ──────────────────────────────────────────────────

    static void handleItemCrafted(ServerPlayer sp, ItemStack crafted) {
        var savedData = ResearchSavedData.get(sp.serverLevel());
        var knowledge = savedData.getOrCreate(sp.getUUID());

        if (crafted.getItem() == ModBlocks.RESEARCH_TABLE.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.RESEARCH_TABLE)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.RESEARCH_TABLE);
            savedData.setDirty();
            pendingResearchTableTutorial.add(sp.getUUID());
        }
        if (crafted.getItem() == ModBlocks.MANA_GENERATOR.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GEN_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_GEN_CRAFT);
            savedData.setDirty();
            pendingManaGenCraft.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }
        if (crafted.getItem() instanceof WandRodItem
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.WAND_ROD_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.WAND_ROD_CRAFT);
            savedData.setDirty();
            pendingWandRodCraft.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }
        if (crafted.getItem() == ModBlocks.MANA_GRINDER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GRINDER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_GRINDER_CRAFT);
            savedData.setDirty();
            pendingManaGrinderPlaced.add(sp.getUUID());
        }
        if (crafted.getItem() == ModBlocks.MANA_INFUSER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_INFUSER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_INFUSER_CRAFT);
            savedData.setDirty();
            pendingManaInfuserPlaced.add(sp.getUUID());
        }
        if (crafted.getItem() == ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT);
            savedData.setDirty();
            pendingManaCraftingPlaced.put(sp.getUUID(), GUI_CLOSE_TIMEOUT_TICKS);
        }
        if (crafted.getItem() == ModBlocks.MANA_DEPLOYER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_DEPLOYER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_DEPLOYER_CRAFT);
            savedData.setDirty();
            pendingManaDeployerPlaced.add(sp.getUUID());
        }
        if (crafted.getItem() == ModBlocks.MANA_CHARGER.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CHARGER_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.MANA_CHARGER_CRAFT);
            savedData.setDirty();
            pendingManaChargerPlaced.add(sp.getUUID());
        }
        if (crafted.getItem() == ModBlocks.SOLAR_MANA_COLLECTOR.get().asItem()
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT)) {
            knowledge.addPendingTutorial(NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT);
            savedData.setDirty();
            pendingSolarCollectorPlaced.add(sp.getUUID());
        }
    }

    static void handleBlockPlaced(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState placed) {
        if (placed.is(ModBlocks.MANA_GENERATOR.get())
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

        checkMachinePlaced(player, level, pos, placed,
                ModBlocks.MANA_GRINDER.get(), pendingManaGrinderPlaced, NaraTutorialFlow.MANA_GRINDER_CRAFT);
        checkMachinePlaced(player, level, pos, placed,
                ModBlocks.MANA_INFUSER.get(), pendingManaInfuserPlaced, NaraTutorialFlow.MANA_INFUSER_CRAFT);
        checkMachinePlaced(player, level, pos, placed,
                ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get(), pendingManaCraftingWaitForPlace, NaraTutorialFlow.MANA_CRAFTING_PLACED);
        checkMachinePlaced(player, level, pos, placed,
                ModBlocks.MANA_DEPLOYER.get(), pendingManaDeployerPlaced, NaraTutorialFlow.MANA_DEPLOYER_CRAFT);
        checkMachinePlaced(player, level, pos, placed,
                ModBlocks.MANA_CHARGER.get(), pendingManaChargerPlaced, NaraTutorialFlow.MANA_CHARGER_CRAFT);
        checkMachinePlaced(player, level, pos, placed,
                ModBlocks.SOLAR_MANA_COLLECTOR.get(), pendingSolarCollectorPlaced, NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT);
    }

    private static void checkMachinePlaced(ServerPlayer player, ServerLevel level, BlockPos pos,
                                            BlockState placed, Block target,
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

    static void handleEquipmentChange(ServerPlayer player, EquipmentSlot slot, ItemStack from, ItemStack to) {
        if (slot != EquipmentSlot.FEET) return;
        if (!(to.getItem() instanceof ManaSprintBootsItem)) return;
        if (from.getItem() instanceof ManaSprintBootsItem) return;
        var savedData = ResearchSavedData.get(player.serverLevel());
        if (savedData.getOrCreate(player.getUUID()).markTutorialSeen(NaraTutorialFlow.BOOTS_EQUIP)) {
            NaraTutorialPacket.send(player, NaraTutorialFlow.BOOTS_EQUIP);
            savedData.setDirty();
        }
    }

    static void restorePendingTutorials(ServerPlayer player) {
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

        if (knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GEN_CRAFT)
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_GEN_PLACED)) {
            pendingManaGenPlacement.put(uuid, GUI_CLOSE_TIMEOUT_TICKS + 1);
        }
        if (knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CRAFTING_CRAFT)
                && !knowledge.hasSeenTutorial(NaraTutorialFlow.MANA_CRAFTING_PLACED)) {
            pendingManaCraftingWaitForPlace.add(uuid);
        }
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

    // ── tick 主邏輯 ────────────────────────────────────────────────────────

    static void tick(ServerTickEvent.Post event) {
        tutorialLoginDelay.entrySet().removeIf(e -> e.setValue(e.getValue() - 1) <= 0);
        tickResearchTableTutorial(event);
        tickDelayedTutorial(pendingFirstScanTutorial, NaraTutorialFlow.FIRST_SCAN, event);
        tickDelayedTutorial(pendingFirstWatchOpenTutorial, NaraTutorialFlow.FIRST_WATCH_OPEN, event);
        tickDelayedTutorial(pendingFirstResearch, NaraTutorialFlow.FIRST_RESEARCH, event);
        tickDelayedTutorial(pendingFirstAltarFormed, NaraTutorialFlow.FIRST_ALTAR_FORMED, event);
        tickWandRodCraft(event);
        tickWandCoreWatch(event);
        tickManaGenCraft(event);
        tickManaCraftingPlaced(event);
        // pendingManaGenPlacement 過期清掉（不 fire tutorial，純清理）
        if (!pendingManaGenPlacement.isEmpty()) {
            pendingManaGenPlacement.entrySet().removeIf(e -> e.setValue(e.getValue() - 1) <= 0);
        }
    }

    /** Warden discard 倒數，由 NaraServerEvents.onServerTick 在 punishment 之後呼叫。 */
    static void tickWardenDespawn(ServerTickEvent.Post event) {
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

    private static void tickResearchTableTutorial(ServerTickEvent.Post event) {
        if (pendingResearchTableTutorial.isEmpty()) return;
        Iterator<UUID> pit = pendingResearchTableTutorial.iterator();
        while (pit.hasNext()) {
            UUID uuid = pit.next();
            if (tutorialLoginDelay.containsKey(uuid)) continue;
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) { pit.remove(); continue; }
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

    private static void tickWandRodCraft(ServerTickEvent.Post event) {
        if (pendingWandRodCraft.isEmpty()) return;
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

    private static void tickWandCoreWatch(ServerTickEvent.Post event) {
        if (watchingForWandCore.isEmpty()) return;
        wandCoreCheckTimer++;
        if (wandCoreCheckTimer < 20) return;
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

    private static void tickManaGenCraft(ServerTickEvent.Post event) {
        if (pendingManaGenCraft.isEmpty()) return;
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

    private static void tickManaCraftingPlaced(ServerTickEvent.Post event) {
        if (pendingManaCraftingPlaced.isEmpty()) return;
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

    // ── 清理 ──────────────────────────────────────────────────────────────

    static void clearAll() {
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
    }

    static void clearForPlayer(UUID uuid) {
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
    }
}
