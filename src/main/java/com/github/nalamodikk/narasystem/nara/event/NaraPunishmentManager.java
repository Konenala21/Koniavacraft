package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraCloseDialoguePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 娜拉懲罰系統 server-side 狀態與邏輯。
 * 玩家做了讓娜拉生氣的事 → schedulePunishmentDialogue → 延遲後播放懲罰對話；
 * 若玩家在 dialog 觸發前死亡，等重生再延遲補發。
 */
final class NaraPunishmentManager {
    private NaraPunishmentManager() {}

    private static final Map<UUID, Integer> pendingDialogue = new HashMap<>();
    private static final Set<UUID> awaitingRespawn = new HashSet<>();
    private static final Set<UUID> punishmentActive = new HashSet<>();

    static void schedule(UUID playerUUID, int delayTicks) {
        pendingDialogue.put(playerUUID, delayTicks);
        punishmentActive.add(playerUUID);
    }

    static void cancel(UUID uuid) {
        pendingDialogue.remove(uuid);
        punishmentActive.remove(uuid);
        awaitingRespawn.remove(uuid);
    }

    static boolean isActive(UUID uuid) {
        return punishmentActive.contains(uuid);
    }

    static void handleDeath(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!punishmentActive.contains(uuid)) return;
        PacketDistributor.sendToPlayer(player, new NaraCloseDialoguePacket());
        pendingDialogue.remove(uuid);
        awaitingRespawn.add(uuid);
    }

    static void handleRespawn(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!awaitingRespawn.remove(uuid)) return;
        pendingDialogue.put(uuid, 20);
    }

    /** 在 NaraServerEvents.onServerTick 內被呼叫，順序保持原樣（懲罰倒數在 tutorial tick 之後）。 */
    static void tick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, Integer>> it = pendingDialogue.entrySet().iterator();
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
    }

    static void clearAll() {
        pendingDialogue.clear();
        awaitingRespawn.clear();
        punishmentActive.clear();
    }

    static void clearForPlayer(UUID uuid) {
        pendingDialogue.remove(uuid);
        awaitingRespawn.remove(uuid);
        punishmentActive.remove(uuid);
    }

    private static void sendPunishmentDialogue(ServerPlayer player) {
        punishmentActive.remove(player.getUUID());
        // grant nara_ignored advancement（避免引用 NaraServerEvents 形成循環，本地實作）
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_ignored");
        var holder = player.server.getAdvancements().get(loc);
        if (holder != null) player.getAdvancements().award(holder, "obtained");
        PacketDistributor.sendToPlayer(player, new NaraStartDialoguePacket.Punishment());
    }
}
