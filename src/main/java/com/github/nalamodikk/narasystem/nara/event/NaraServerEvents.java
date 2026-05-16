package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraCloseDialoguePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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

    public static void schedulePunishmentDialogue(UUID playerUUID, int delayTicks) {
        pendingPunishmentDialogue.put(playerUUID, delayTicks);
        naraPunishmentActive.add(playerUUID);
    }

    public static void scheduleWardenDespawn(UUID wardenUUID, int delayTicks) {
        pendingWardenDespawn.put(wardenUUID, delayTicks);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
