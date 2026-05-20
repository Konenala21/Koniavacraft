package com.github.nalamodikk.common.block.blockentity.mana_charger;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ManaChargerScreen extends AutoSizedModularScreen<ManaChargerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_charger_gui.png");

    public ManaChargerScreen(ManaChargerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        root.add(new ManaBarWidget(9, 20,
                menu::getCurrentMana,
                menu::getMaxMana
        ).setSize(10, 47).setDrawBackground(false));
    }
}
