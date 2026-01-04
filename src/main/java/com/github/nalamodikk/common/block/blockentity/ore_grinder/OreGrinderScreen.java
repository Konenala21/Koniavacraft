package com.github.nalamodikk.common.block.blockentity.ore_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ArrowProgressWidget;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ⚙️ 粉碎機 GUI 界面 (v2 - 自動尺寸)
 */
public class OreGrinderScreen extends AutoSizedModularScreen<OreGrinderMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/ore_grinder_gui.png");

    public OreGrinderScreen(OreGrinderMenu menu, Inventory playerInventory, Component title) {
        // ✨ v2: 自動檢測尺寸並繪製背景
        super(menu, playerInventory, title, TEXTURE, 176, 222);
    }

    @Override
    protected void buildGui(Panel root) {
        // 1. 進度條 (箭頭)
        // 位置 (79, 35)，尺寸 24x17 (基於之前 ArrowProgressWidget 的預設)
        root.add(new ArrowProgressWidget(79, 35, 
            menu::getProgress, 
            menu::getMaxProgress
        ));

        // 2. 魔力條
        // 位置 (9, 17)，尺寸由 ManaBarWidget 決定 (預設 14x50)
        // 如果您的背景槽位尺寸不同，可以在建構子中調整
        root.add(new ManaBarWidget(9, 17, 
            menu::getCurrentMana, 
            menu::getMaxMana
        ));
    }

    // ✨ v2: renderBg 完全刪除，AutoSizedModularScreen 會自動處理
}