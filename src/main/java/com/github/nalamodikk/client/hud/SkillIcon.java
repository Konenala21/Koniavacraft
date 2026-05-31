package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.skill.StoredSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a skill's icon: the player's custom item icon if one was set, otherwise an
 * auto-generated badge from the skill's dominant aspect (its first effect, or the
 * carrier) as a tinted hex cell with the aspect's initial.
 */
public final class SkillIcon {

    private static final ResourceLocation HEX_CELL =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");

    /** The custom icon item of a stored skill, or null if none/invalid (use auto). */
    @Nullable
    public static Item customItem(StoredSkill skill) {
        if (skill.icon().isEmpty()) return null;
        Item item = BuiltInRegistries.ITEM.get(skill.icon().get());
        return item == Items.AIR ? null : item;
    }

    /** The aspect that drives the auto-generated icon: first effect, else carrier. */
    @Nullable
    public static Aspect dominantAspect(StoredSkill skill) {
        if (!skill.effects().isEmpty()) {
            Aspect e = ModAspects.get(skill.effects().get(0));
            if (e != null) return e;
        }
        return ModAspects.get(skill.carrier());
    }

    public static void render(GuiGraphics g, Font font, StoredSkill skill, int x, int y, int size) {
        render(g, font, customItem(skill), dominantAspect(skill), x, y, size);
    }

    /** Custom item takes precedence; otherwise the dominant aspect badge. */
    public static void render(GuiGraphics g, Font font, @Nullable Item custom, @Nullable Aspect dominant,
                              int x, int y, int size) {
        if (custom != null) {
            g.renderFakeItem(new ItemStack(custom), x, y);
            return;
        }
        if (dominant == null) return;
        int color = dominant.getColor();
        RenderSystem.setShaderColor(ch(color, 16), ch(color, 8), ch(color, 0), 1.0F);
        g.blit(HEX_CELL, x, y, 0, 0, size, size, size, size);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        String label = dominant.getId().getPath().substring(0, 1).toUpperCase();
        g.drawString(font, label, x + size / 3, y + size / 3, 0xFFFFFF, true);
    }

    private static float ch(int color, int shift) {
        return ((color >> shift) & 0xFF) / 255.0F;
    }

    private SkillIcon() {}
}
