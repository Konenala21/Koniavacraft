package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.UpgradeItem;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class UpgradeItemTooltipEvent {
    private UpgradeItemTooltipEvent() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof UpgradeItem upgradeItem)) {
            return;
        }

        UpgradeType type = upgradeItem.getUpgradeType();
        if (!Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tooltip.koniava.upgrade.hold_shift"));
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.koniava.upgrade." + type.getSerializedName()));
        event.getToolTip().add(Component.translatable("tooltip.koniava.upgrade.stackable", stack.getMaxStackSize()));
        event.getToolTip().add(Component.translatable("tooltip.koniava.upgrade.install_in", getCompatibleMachinesText(type)));
        event.getToolTip().add(Component.translatable("tooltip.koniava.upgrade.config." + type.getSerializedName()));
    }

    private static MutableComponent getCompatibleMachinesText(UpgradeType type) {
        return switch (type) {
            case SPEED, EFFICIENCY ->
                    Component.translatable("tooltip.koniava.upgrade.compat.solar_mana_collector");
            case ACCELERATED_PROCESSING, EXPANDED_FUEL_CHAMBER, CATALYTIC_CONVERTER, DIAGNOSTIC_DISPLAY ->
                    Component.translatable("tooltip.koniava.upgrade.compat.mana_generator");
        };
    }
}
