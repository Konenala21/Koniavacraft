package com.github.nalamodikk.common.block.blockentity.mana_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * ⚙️ 魔力粉碎機 GUI 界面 (v2 - 自動尺寸)
 */
public class ManaGrinderScreen extends AutoSizedModularScreen<ManaGrinderMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_grinder_gui.png");

    public ManaGrinderScreen(ManaGrinderMenu menu, Inventory playerInventory, Component title) {
        // ✨ v2: 自動檢測尺寸並繪製背景
        super(menu, playerInventory, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        // 1. 進度條 (與魔力注入機相同的繪製方式)
        root.add(new AbstractWidget(67, 36, 44, 12) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                if (menu.isWorking()) {
                    int progress = menu.getProgressPercentage();
                    int fillWidth = Math.max(0, (width * progress) / 100);

                    if (fillWidth > 0) {
                        graphics.blit(TEXTURE,
                            0, 0,
                            176, 52,
                            fillWidth, height,
                            256, 256
                        );
                    }
                }
            }
        });

        // 2. 魔力條
        // 位置 (9, 17)，尺寸由 ManaBarWidget 決定 (預設 14x50)
        // 如果您的背景槽位尺寸不同，可以在建構子中調整
        root.add(new ManaBarWidget(9, 17,
            menu::getCurrentMana, 
            menu::getMaxMana
        ).setSize(10, 48).setDrawBackground(false));

        // 3. JEI 配方提示區域 (右上角書本)
        root.add(new AbstractWidget(149, 4, 21, 15) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY) {
                // 使用背景貼圖上的書本圖示，這裡不重畫
            }

            @Override
            public List<Component> getTooltip() {
                if (ModList.get().isLoaded("jei")) {
                    return List.of();
                }
                return List.of(Component.translatable("tooltip.koniava.mana_grinder.recipes"));
            }
        });
    }

    // ✨ v2: renderBg 完全刪除，AutoSizedModularScreen 會自動處理
}
