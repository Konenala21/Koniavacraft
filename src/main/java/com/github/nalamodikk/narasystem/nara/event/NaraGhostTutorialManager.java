package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableMenu;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTutorialPacket;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
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
 * Dev / 教程預覽用的「ghost block」教程派發。
 *
 * <p>用 /koniava nara tutorial &lt;id&gt; 指令觸發：在玩家頭上放對應機器的 ghost 方塊，
 * 等 chunk update 到 client → 自動開 GUI → 玩家關 GUI 後播 tutorial → 清掉 ghost。
 *
 * <p>兩 phase：phase 1（GUI 開啟前的 delay 計時，{@code ghostOpenDelay}）、
 * phase 2（GUI 開啟、等玩家關閉，{@code pendingTestTutorial}）。
 */
final class NaraGhostTutorialManager {
    private NaraGhostTutorialManager() {}

    private static final int GHOST_OPEN_DELAY = 10;

    // Phase 1
    private static final Map<UUID, Integer> ghostOpenDelay = new HashMap<>();
    private static final Map<UUID, String> ghostOpenTutorialId = new HashMap<>();
    // Phase 2
    private static final Set<UUID> pendingTestTutorial = new HashSet<>();
    private static final Map<UUID, String> pendingTestTutorialId = new HashMap<>();
    // Ghost block 追蹤
    private static final Map<UUID, BlockPos> ghostBlocks = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> ghostBlockLevels = new HashMap<>();
    // 正在被 ghost cleanup 移除的位置；機器 onRemove 看到就 skip 掉 NBT drop
    private static final Set<Long> ghostPositionKeys = new HashSet<>();

    // 機器教程都用 setOverlayOnScreen(true) 渲染在 ghost GUI 之上，因此可以 GUI 開啟瞬間就播
    private static final Set<String> FIRE_ON_GUI_OPEN = Set.of(
            NaraTutorialFlow.MANA_GEN_PLACED,
            NaraTutorialFlow.MANA_GEN_CRAFT,
            NaraTutorialFlow.MANA_GRINDER_CRAFT,
            NaraTutorialFlow.MANA_INFUSER_CRAFT,
            NaraTutorialFlow.MANA_CRAFTING_CRAFT,
            NaraTutorialFlow.MANA_DEPLOYER_CRAFT,
            NaraTutorialFlow.MANA_CHARGER_CRAFT,
            NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT,
            NaraTutorialFlow.RESEARCH_TABLE,
            NaraTutorialFlow.FIRST_RESEARCH
    );

    static boolean isGhostBlock(BlockPos pos) {
        return ghostPositionKeys.contains(pos.asLong());
    }

    static void schedule(ServerPlayer player, String tutorialId) {
        UUID uuid = player.getUUID();
        // 清乾淨先前可能殘留的 ghost（double-call leak 防護）
        BlockPos existingGhost = ghostBlocks.remove(uuid);
        ResourceKey<Level> existingDim = ghostBlockLevels.remove(uuid);
        if (existingGhost != null) {
            ServerLevel existingLvl = existingDim != null
                    ? player.server.getLevel(existingDim) : player.serverLevel();
            if (existingLvl != null) removeGhostBlock(existingLvl, existingGhost);
        }
        ghostOpenDelay.remove(uuid);
        ghostOpenTutorialId.remove(uuid);
        pendingTestTutorial.remove(uuid);
        pendingTestTutorialId.remove(uuid);

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
        ghostBlocks.put(uuid, ghostPos);
        ghostBlockLevels.put(uuid, level.dimension());
        ghostOpenDelay.put(uuid, GHOST_OPEN_DELAY);
        ghostOpenTutorialId.put(uuid, tutorialId);
    }

    /** NaraServerEvents.onServerTick 內呼叫，順序保持原樣（在 tutorial 主 tick 之後、punishment 之前）。 */
    static void tick(ServerTickEvent.Post event) {
        tickPhase1OpenDelay(event);
        tickPhase2GuiCloseWait(event);
    }

    private static void tickPhase1OpenDelay(ServerTickEvent.Post event) {
        if (ghostOpenDelay.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> git = ghostOpenDelay.entrySet().iterator();
        while (git.hasNext()) {
            Map.Entry<UUID, Integer> entry = git.next();
            UUID uuid = entry.getKey();
            int remaining = entry.getValue() - 1;
            if (remaining > 0) { entry.setValue(remaining); continue; }
            git.remove();
            String tid = ghostOpenTutorialId.remove(uuid);
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                BlockPos gp = ghostBlocks.remove(uuid);
                ResourceKey<Level> dimKey = ghostBlockLevels.remove(uuid);
                if (gp != null && dimKey != null) {
                    ServerLevel ghostLvl = event.getServer().getLevel(dimKey);
                    if (ghostLvl != null) removeGhostBlock(ghostLvl, gp);
                }
                continue;
            }
            BlockPos gp = ghostBlocks.get(uuid);
            boolean opened = false;
            if (gp != null) {
                ResourceKey<Level> ghostDim = ghostBlockLevels.get(uuid);
                ServerLevel ghostLvl = ghostDim != null
                        ? event.getServer().getLevel(ghostDim) : player.serverLevel();
                BlockEntity be = ghostLvl != null ? ghostLvl.getBlockEntity(gp) : null;
                if (be instanceof MenuProvider mp) {
                    player.openMenu(mp, gp);
                    opened = true;
                } else if (be instanceof ResearchTableBlockEntity rbe) {
                    player.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new ResearchTableMenu(id, inv, rbe),
                            Component.translatable(ghostLvl.getBlockState(gp).getBlock().getDescriptionId())), gp);
                    opened = true;
                }
            }
            if (opened) {
                pendingTestTutorial.add(uuid);
                if (FIRE_ON_GUI_OPEN.contains(tid)) {
                    NaraTutorialPacket.send(player, tid);
                    pendingTestTutorialId.put(uuid, null);
                } else {
                    pendingTestTutorialId.put(uuid, tid);
                }
            } else {
                BlockPos cleanPos = ghostBlocks.remove(uuid);
                ResourceKey<Level> cleanDim = ghostBlockLevels.remove(uuid);
                if (cleanPos != null) {
                    ServerLevel cleanLvl = cleanDim != null
                            ? event.getServer().getLevel(cleanDim) : player.serverLevel();
                    if (cleanLvl != null) removeGhostBlock(cleanLvl, cleanPos);
                }
                if (tid != null) NaraTutorialPacket.send(player, tid);
            }
        }
    }

    private static void tickPhase2GuiCloseWait(ServerTickEvent.Post event) {
        if (pendingTestTutorial.isEmpty()) return;
        Iterator<UUID> tit = pendingTestTutorial.iterator();
        while (tit.hasNext()) {
            UUID uuid = tit.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                tit.remove();
                pendingTestTutorialId.remove(uuid);
                BlockPos gp = ghostBlocks.remove(uuid);
                ResourceKey<Level> dimKey = ghostBlockLevels.remove(uuid);
                if (gp != null && dimKey != null) {
                    ServerLevel ghostLvl = event.getServer().getLevel(dimKey);
                    if (ghostLvl != null) removeGhostBlock(ghostLvl, gp);
                }
                continue;
            }
            if (player.containerMenu == player.inventoryMenu) {
                tit.remove();
                String tid = pendingTestTutorialId.remove(uuid);
                BlockPos gp = ghostBlocks.remove(uuid);
                ResourceKey<Level> dimKey = ghostBlockLevels.remove(uuid);
                if (gp != null) {
                    ServerLevel ghostLvl = dimKey != null
                            ? event.getServer().getLevel(dimKey) : player.serverLevel();
                    if (ghostLvl != null) removeGhostBlock(ghostLvl, gp);
                }
                if (tid != null) NaraTutorialPacket.send(player, tid);
            }
        }
    }

    static void clearAll(MinecraftServer server) {
        for (Map.Entry<UUID, BlockPos> e : ghostBlocks.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            ResourceKey<Level> dimKey = ghostBlockLevels.get(e.getKey());
            ServerLevel targetLvl = dimKey != null
                    ? server.getLevel(dimKey)
                    : (p != null ? p.serverLevel() : null);
            if (targetLvl != null) removeGhostBlock(targetLvl, e.getValue());
        }
        ghostBlocks.clear();
        ghostBlockLevels.clear();
        ghostOpenDelay.clear();
        ghostOpenTutorialId.clear();
        pendingTestTutorial.clear();
        pendingTestTutorialId.clear();
    }

    static void clearForPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ghostOpenDelay.remove(uuid);
        ghostOpenTutorialId.remove(uuid);
        ResourceKey<Level> dimKey = ghostBlockLevels.remove(uuid);
        BlockPos ghostPos = ghostBlocks.remove(uuid);
        if (ghostPos != null) {
            ServerLevel targetLvl = dimKey != null ? player.server.getLevel(dimKey) : player.serverLevel();
            if (targetLvl != null) removeGhostBlock(targetLvl, ghostPos);
        }
        pendingTestTutorial.remove(uuid);
        pendingTestTutorialId.remove(uuid);
    }

    private static void removeGhostBlock(ServerLevel level, BlockPos pos) {
        ghostPositionKeys.add(pos.asLong());
        try {
            level.removeBlock(pos, false);
        } finally {
            ghostPositionKeys.remove(pos.asLong());
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
            case NaraTutorialFlow.MANA_DEPLOYER_CRAFT ->
                    ModBlocks.MANA_DEPLOYER.get().defaultBlockState();
            case NaraTutorialFlow.MANA_CHARGER_CRAFT ->
                    ModBlocks.MANA_CHARGER.get().defaultBlockState();
            case NaraTutorialFlow.SOLAR_COLLECTOR_CRAFT ->
                    ModBlocks.SOLAR_MANA_COLLECTOR.get().defaultBlockState();
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
}
