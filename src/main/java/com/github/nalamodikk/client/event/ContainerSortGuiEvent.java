package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.gui.widget.SortButton;
import com.github.nalamodikk.common.config.ModClientConfig;
import com.github.nalamodikk.common.inventory.sort.SortTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.OptionalInt;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ContainerSortGuiEvent {

    @SubscribeEvent
    public static void onContainerInit(ScreenEvent.Init.Post event) {
        if (!ModClientConfig.INSTANCE.sortButtonEnabled.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (menu instanceof InventoryMenu) return;

        try {
            MenuType<?> menuType = menu.getType();
            if (isExcludedMenuType(menuType)) return;
            ResourceLocation menuId = BuiltInRegistries.MENU.getKey(menuType);
            if (menuId != null && menuId.getNamespace().equals(KoniavacraftMod.MOD_ID)) return;
        } catch (UnsupportedOperationException e) {
            return;
        }

        net.minecraft.world.entity.player.Inventory playerInv = mc.player.getInventory();

        OptionalInt containerMinY = menu.slots.stream()
            .filter(slot -> slot.container != playerInv)
            .mapToInt(slot -> slot.y)
            .min();

        OptionalInt playerInvMinY = menu.slots.stream()
            .filter(slot -> slot.container == playerInv && slot.getContainerSlot() >= 9)
            .mapToInt(slot -> slot.y)
            .min();

        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();

        if (containerMinY.isPresent()) {
            int btnY = guiTop + containerMinY.getAsInt() - 14;
            event.addListener(new SortButton(guiLeft + 156, btnY, 16, 12, SortTarget.CONTAINER));
        }

        if (playerInvMinY.isPresent()) {
            int btnY = guiTop + playerInvMinY.getAsInt() - 14;
            event.addListener(new SortButton(guiLeft + 156, btnY, 16, 12, SortTarget.PLAYER_INVENTORY));
        }
    }

    private static boolean isExcludedMenuType(MenuType<?> type) {
        return type == MenuType.CRAFTING
            || type == MenuType.FURNACE
            || type == MenuType.BLAST_FURNACE
            || type == MenuType.SMOKER
            || type == MenuType.ENCHANTMENT
            || type == MenuType.ANVIL
            || type == MenuType.BEACON
            || type == MenuType.GRINDSTONE
            || type == MenuType.STONECUTTER
            || type == MenuType.LOOM
            || type == MenuType.CARTOGRAPHY_TABLE;
    }
}
