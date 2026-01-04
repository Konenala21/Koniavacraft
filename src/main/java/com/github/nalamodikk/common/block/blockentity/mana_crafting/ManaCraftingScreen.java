package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 🧪 魔力合成台 GUI 界面 (v2 - 自動尺寸)
 */
public class ManaCraftingScreen extends AutoSizedModularScreen<ManaCraftingMenu> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_crafting_table_gui.png");

    public ManaCraftingScreen(ManaCraftingMenu container, Inventory inv, Component title) {
        // ✨ v2: 自動檢測尺寸並繪製背景
        super(container, inv, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        // 魔力條 (11, 19) - 7x47
        root.add(new ManaBarWidget(11, 19,
            menu::getManaStored,
            () -> ManaCraftingTableBlockEntity.MAX_MANA
        ).setSize(7, 47));
    }

    // ✨ v2: renderBg 完全刪除，AutoSizedModularScreen 會自動處理
}
