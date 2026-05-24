package com.github.nalamodikk.common.particle;

import com.github.nalamodikk.common.network.packet.client.FormationStagePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InteractiveFormationManager {

    public static final int STAGE_OFF         = 0;
    public static final int STAGE_SINGLE      = 1;
    public static final int STAGE_RING        = 2;
    public static final int STAGE_RING_SEQ    = 3;
    public static final int STAGE_DOUBLE_RING = 4;
    private static final int STAGE_COUNT      = 5;

    private static final Map<UUID, Integer> playerStages = new HashMap<>();

    public static int advance(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int current = playerStages.getOrDefault(uuid, STAGE_OFF);
        int next = (current + 1) % STAGE_COUNT;

        if (next == STAGE_OFF) {
            playerStages.remove(uuid);
        } else {
            playerStages.put(uuid, next);
        }

        // Broadcast to everyone within 64 blocks (including the activating player)
        FormationStagePacket.broadcast(player, next);
        return next;
    }

    public static int currentStage(UUID uuid) {
        return playerStages.getOrDefault(uuid, STAGE_OFF);
    }
}
