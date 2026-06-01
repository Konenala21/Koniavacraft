package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModItems;
import com.github.nalamodikk.research.skill.SkillCasting;
import com.github.nalamodikk.research.skill.SkillEncoding;
import com.github.nalamodikk.research.skill.StoredSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shows the spell-core wand's current skill (name, index, mana cost) above the
 * hotbar so the player knows what {@code X} (Cycle Spell Skill) will cast.
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class SkillHudOverlay {

    private static final int ACCENT = 0xFFEE7722;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        ItemStack wand = heldSpellWand(mc.player);
        if (wand == null) return;

        WandCoreData data = wand.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
        List<StoredSkill> skills = SkillEncoding.getSkills(data.core());

        GuiGraphics g = event.getGuiGraphics();
        int cx = g.guiWidth() / 2;
        int baseY = g.guiHeight() - 72;

        if (skills.isEmpty()) {
            drawCentered(g, mc, Component.translatable("hud.koniava.skill.none"), cx, baseY + 5, 0xFFAAAAAA);
            return;
        }

        int idx = data.core().getOrDefault(ModDataComponents.SELECTED_SKILL_INDEX, 0);
        if (idx < 0 || idx >= skills.size()) idx = 0;
        StoredSkill skill = skills.get(idx);
        int mana = SkillCasting.effectiveMana(wand, skill.compile().cost().mana());
        String name = skill.name().isBlank()
                ? Component.translatable("gui.koniava.skill_encoder.unnamed").getString()
                : skill.name();

        int nameW = mc.font.width(name);
        SkillIcon.render(g, mc.font, skill, cx - nameW / 2 - 20, baseY - 4, 16);
        drawCentered(g, mc, Component.literal(name), cx, baseY, ACCENT);
        drawCentered(g, mc, Component.translatable("hud.koniava.skill.info", idx + 1, skills.size(), mana),
                cx, baseY + 10, 0xFFCCCCCC);

        // cooldown bar (depletes as the selected slot recovers)
        if (ClientSkillCooldowns.remaining(idx) > 0) {
            int barW = 60;
            int bx = cx - barW / 2, by = baseY + 21;
            g.fill(bx - 1, by - 1, bx + barW + 1, by + 4, 0x80000000);
            int fill = (int) (barW * ClientSkillCooldowns.fraction(idx));
            g.fill(bx, by, bx + fill, by + 3, ACCENT);
        }
    }

    private static void drawCentered(GuiGraphics g, Minecraft mc, Component text, int cx, int y, int color) {
        int w = mc.font.width(text);
        g.fill(cx - w / 2 - 3, y - 2, cx + w / 2 + 3, y + 9, 0x66000000);
        g.drawCenteredString(mc.font, text, cx, y, color);
    }

    @Nullable
    private static ItemStack heldSpellWand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof WandRodItem) {
                WandCoreData data = held.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
                if (data.hasCore() && data.core().getItem() == ModItems.SPELL_CORE.get()) return held;
            }
        }
        return null;
    }

    private SkillHudOverlay() {}
}
