package com.github.nalamodikk.research.skill;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side, per-player skill cooldowns: each skill slot cools down on its own
 * so the player rotates through the core's encoded skills (cast one, switch, cast
 * another while the first cools), plus a short global cooldown that stops the
 * degenerate "swap-cast every slot in one tick" case.
 *
 * Transient (lives in RAM only); cooldowns are a moment-to-moment thing, so losing
 * them on restart is fine. Times are in game ticks ({@code level.getGameTime()}).
 */
public final class SkillCooldowns {

    /** Short global cooldown between any two casts (ticks). */
    public static final int GLOBAL_COOLDOWN = 5;

    private static final Map<UUID, long[]> SLOT_END = new HashMap<>();
    private static final Map<UUID, Long> GLOBAL_END = new HashMap<>();

    /** True if both the global cooldown and the given slot are off cooldown. */
    public static boolean ready(Player player, int slot, long now) {
        if (now < GLOBAL_END.getOrDefault(player.getUUID(), 0L)) return false;
        long[] ends = SLOT_END.get(player.getUUID());
        if (ends == null || slot < 0 || slot >= ends.length) return true;
        return now >= ends[slot];
    }

    /** Start the slot's cooldown and the global cooldown. */
    public static void start(Player player, int slot, long now, int cooldownTicks) {
        long[] ends = SLOT_END.computeIfAbsent(player.getUUID(), k -> new long[SkillEncoding.MAX_SLOTS]);
        if (slot >= 0 && slot < ends.length) ends[slot] = now + cooldownTicks;
        GLOBAL_END.put(player.getUUID(), now + GLOBAL_COOLDOWN);
    }

    public static void clear(UUID playerId) {
        SLOT_END.remove(playerId);
        GLOBAL_END.remove(playerId);
    }

    private SkillCooldowns() {}
}
