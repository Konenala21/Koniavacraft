package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyHelmetItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaConcentrationHelper;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ManaArmorHudRenderer {

    private static final int PAD_X = 5;
    private static final int PAD_Y = 5;
    private static final int LINE_H = 10;
    private static final int COLOR_MANA = 0x00CCFF;
    private static final int COLOR_LABEL = 0xAAAAAA;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        if (!(mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ManaAlloyHelmetItem)) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int x = PAD_X;
        int y = PAD_Y;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            boolean isManaArmor = stack.getItem() instanceof ManaArmorItem;
            boolean isManaBoots = stack.getItem() instanceof ManaSprintBootsItem;
            if (!isManaArmor && !isManaBoots) continue;

            int mana = ManaArmorItem.getMana(stack);
            int max  = ManaArmorItem.getMaxMana(stack);
            String label = stack.getHoverName().getString();
            Component line = Component.literal(label + ": ").withStyle(s -> s.withColor(COLOR_LABEL))
                    .append(Component.literal(mana + "/" + max).withStyle(s -> s.withColor(COLOR_MANA)));
            g.drawString(font, line, x, y, COLOR_MANA, true);
            y += LINE_H;
        }

        int pct = Math.round(ManaConcentrationHelper.getConcentration() * 100);
        int envColor = pct >= 50 ? 0x55FF55 : pct >= 20 ? 0xFFFF55 : pct >= 10 ? 0xFFAA00 : 0xFF5555;
        Component envLine = Component.translatable("hud.koniava.mana_armor.env_concentration", pct)
                .withStyle(s -> s.withColor(envColor));
        g.drawString(font, envLine, x, y, envColor, true);
    }
}
