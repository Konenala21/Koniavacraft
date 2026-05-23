package com.github.nalamodikk.narasystem.nara.hud;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NaraGuiHighlight {

    private static boolean active = false;
    private static float relX, relY;
    private static int size = 40;

    public static void show(float relX, float relY, int size) {
        NaraGuiHighlight.relX = relX;
        NaraGuiHighlight.relY = relY;
        NaraGuiHighlight.size = size;
        active = true;
    }

    public static void hide() {
        active = false;
    }

    public static boolean isActive() { return active; }
    public static float getRelX()   { return relX; }
    public static float getRelY()   { return relY; }
    public static int getSize()     { return size; }

    public static float getPulse() {
        return (float)(0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 400.0));
    }

    public static float getScale() {
        return (float)(1.0 + 0.18 * Math.sin(System.currentTimeMillis() / 500.0));
    }
}
