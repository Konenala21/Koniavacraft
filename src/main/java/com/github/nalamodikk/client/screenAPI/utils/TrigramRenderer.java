package com.github.nalamodikk.client.screenAPI.utils;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

/**
 * Procedural renderer for the Eight Trigrams (八卦) symbols.
 * Renders Yang (solid) and Yin (broken) lines based on the aspect.
 */
public class TrigramRenderer {

    private static final Map<Aspect, Integer> TRIGRAM_CODES = new HashMap<>();

    static {
        // Bits: Top, Middle, Bottom (1 = Yang, 0 = Yin)
        TRIGRAM_CODES.put(ModAspects.QIAN, 0b111); // ☰
        TRIGRAM_CODES.put(ModAspects.DUI,  0b011); // ☱
        TRIGRAM_CODES.put(ModAspects.LI,   0b101); // ☲
        TRIGRAM_CODES.put(ModAspects.ZHEN, 0b001); // ☳
        TRIGRAM_CODES.put(ModAspects.XUN,  0b110); // ☴
        TRIGRAM_CODES.put(ModAspects.KAN,  0b010); // ☵
        TRIGRAM_CODES.put(ModAspects.GEN,  0b100); // ☶
        TRIGRAM_CODES.put(ModAspects.KUN,  0b000); // ☷
    }

    /**
     * Renders a trigram symbol centered at (x, y).
     * @param size The width/height of the symbol.
     */
    public static void render(GuiGraphics g, Aspect aspect, int x, int y, int size, int color) {
        Integer code = TRIGRAM_CODES.get(aspect);
        if (code == null) return;

        int lineH = Math.max(1, size / 8);
        int spacing = Math.max(1, size / 6);
        int startY = y - (lineH * 3 + spacing * 2) / 2;

        for (int i = 0; i < 3; i++) {
            boolean isYang = ((code >> (2 - i)) & 1) == 1;
            int currentY = startY + i * (lineH + spacing);

            if (isYang) {
                // Solid line
                g.fill(x - size / 2, currentY, x + size / 2, currentY + lineH, color);
            } else {
                // Broken line
                int gap = size / 4;
                g.fill(x - size / 2, currentY, x - gap / 2, currentY + lineH, color);
                g.fill(x + gap / 2, currentY, x + size / 2, currentY + lineH, color);
            }
        }
    }
}
