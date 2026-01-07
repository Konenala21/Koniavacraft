package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 🔮 魔力注入機 GUI 界面 (v2 - 自動尺寸)
 */
public class ManaInfuserScreen extends AutoSizedModularScreen<ManaInfuserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_infuser_gui.png");

    public ManaInfuserScreen(ManaInfuserMenu menu, Inventory playerInventory, Component title) {
        // ✨ v2: 自動檢測尺寸並繪製背景
        super(menu, playerInventory, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        // 1. 魔力條 (9, 17) - 10x48
        root.add(new ManaBarWidget(9, 17, menu::getCurrentMana, menu::getMaxMana)
                .setSize(10, 48)
                .setDrawBackground(false)); // 根據原版代碼調整大小

        // 2. 進度條 (67, 36) - 44x12 (與粉碎機一致)
        // 使用匿名 Widget 直接繪製大圖上的進度條
        root.add(new AbstractWidget(67, 36, 44, 12) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                if (menu.isWorking()) {
                    int progress = menu.getProgressPercentage();
                    // 計算像素寬度
                    int fillWidth = Math.max(0, (width * progress) / 100);

                    if (fillWidth > 0) {
                        graphics.blit(TEXTURE, 
                            0, 0,           // 螢幕相對座標
                            176, 52,        // UV 起點 (從大圖右側截取)
                            fillWidth, height, // 繪製大小
                            256, 256
                        );
                    }
                }
            }

            @Override
            public List<Component> getTooltip() {
                if (menu.isWorking()) {
                    return List.of(Component.translatable("gui.koniava.mana_infuser.progress", menu.getProgressPercentage()));
                } else {
                    return List.of(Component.translatable("gui.koniava.mana_infuser.status.idle"));
                }
            }
        });
    }

    // ✨ v2: renderBg 完全刪除，AutoSizedModularScreen 會自動處理
}
