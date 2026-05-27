package com.github.nalamodikk.common.block.blockentity.mana_plate_press;

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

public class ManaPlatePressScreen extends AutoSizedModularScreen<ManaPlatePressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_plate_press_gui.png");

    public ManaPlatePressScreen(ManaPlatePressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        root.add(new ManaBarWidget(9, 17, menu::getCurrentMana, menu::getMaxMana)
                .setSize(10, 48)
                .setDrawBackground(false));

        root.add(new AbstractWidget(67, 36, 44, 12) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                if (menu.isWorking()) {
                    int fillWidth = Math.max(0, (width * menu.getProgressPercentage()) / 100);
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

            @Override
            public List<Component> getTooltip() {
                if (menu.isWorking()) {
                    return List.of(Component.translatable("gui.koniava.mana_plate_press.progress", menu.getProgressPercentage()));
                } else {
                    return List.of(Component.translatable("gui.koniava.mana_plate_press.status.idle"));
                }
            }
        });
    }
}
