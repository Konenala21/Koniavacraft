package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.research.skill.SkillEncoding;
import net.minecraft.client.Minecraft;

/**
 * Client-side mirror of {@link com.github.nalamodikk.research.skill.SkillCooldowns},
 * fed by the server when a cast succeeds, so the HUD can show the selected skill's
 * remaining cooldown. Display only; the server stays authoritative.
 */
public final class ClientSkillCooldowns {

    private static final long[] slotEnd = new long[SkillEncoding.MAX_SLOTS];
    private static final int[] slotDur = new int[SkillEncoding.MAX_SLOTS];
    private static long globalEnd = 0;

    public static void onCast(int slot, int cooldownTicks, int gcdTicks) {
        long now = now();
        if (slot >= 0 && slot < slotEnd.length) {
            slotEnd[slot] = now + cooldownTicks;
            slotDur[slot] = cooldownTicks;
        }
        globalEnd = now + gcdTicks;
    }

    /** Remaining ticks before the slot can cast (max of its own CD and the global CD). */
    public static int remaining(int slot) {
        long now = now();
        long end = (slot >= 0 && slot < slotEnd.length) ? slotEnd[slot] : 0;
        return (int) Math.max(0, Math.max(end, globalEnd) - now);
    }

    /** 0..1 progress of the slot's own cooldown (1 = just cast, 0 = ready). */
    public static float fraction(int slot) {
        if (slot < 0 || slot >= slotEnd.length || slotDur[slot] <= 0) return 0F;
        long left = slotEnd[slot] - now();
        if (left <= 0) return 0F;
        return Math.min(1F, (float) left / slotDur[slot]);
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    private ClientSkillCooldowns() {}
}
