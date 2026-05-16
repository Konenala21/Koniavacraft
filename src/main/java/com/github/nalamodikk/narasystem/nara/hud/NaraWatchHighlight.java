package com.github.nalamodikk.narasystem.nara.hud;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NaraWatchHighlight {

    private static final int POST_DIALOGUE_TICKS = 120;

    private static boolean active = false;
    private static boolean postDialogue = false;
    private static int remainingTicks = 0;

    public static void show() {
        active = true;
        postDialogue = false;
        remainingTicks = Integer.MAX_VALUE;
    }

    public static void tick() {
        if (!active) return;

        if (!postDialogue && !NaraDialogueManager.isActive()) {
            postDialogue = true;
            remainingTicks = POST_DIALOGUE_TICKS;
        }

        if (postDialogue) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                active = false;
            }
        }
    }

    public static boolean isActive() { return active; }

    public static float getPulse() {
        return (float)(0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 400.0));
    }

    public static float getScale() {
        return (float)(1.0 + 0.18 * Math.sin(System.currentTimeMillis() / 500.0));
    }
}
