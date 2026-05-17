package com.github.nalamodikk.common.block.blockentity.mana_deployer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.github.nalamodikk.common.network.packet.server.deployer.CycleDeployerSpeedPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ManaDeployerScreen extends AutoSizedModularScreen<ManaDeployerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_deployer_gui_texture.png");

    public ManaDeployerScreen(ManaDeployerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        // Mana bar on the left
        root.add(new ManaBarWidget(8, 17,
                menu::getCurrentMana,
                menu::getMaxMana
        ).setSize(10, 48).setDrawBackground(false));

        // Mode label
        root.add(new AbstractWidget(30, 60, 116, 10) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                String modeKey = "block.koniava.mana_deployer.mode."
                        + menu.getMode().name().toLowerCase();
                Component modeText = Component.translatable("screen.koniava.mana_deployer.mode_label",
                        Component.translatable(modeKey));
                graphics.drawString(
                        ManaDeployerScreen.this.font,
                        modeText,
                        0, 0,
                        0x404040,
                        false
                );
            }
        });

        // Speed label (clickable, cycles speed preset)
        root.add(new AbstractWidget(30, 74, 116, 10) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                int ticks = menu.getIntervalTick();
                Component speedText = Component.translatable("screen.koniava.mana_deployer.speed_label",
                        ticks, String.format("%.1f", 20f / ticks));
                int color = isMouseOver(screenX, screenY) ? 0x2277FF : 0x404040;
                graphics.drawString(ManaDeployerScreen.this.font, speedText, 0, 0, color, false);
            }

            @Override
            protected boolean onMouseClicked(int localX, int localY, int button) {
                if (button == 0) {
                    CycleDeployerSpeedPacket.sendToServer(menu.getBlockPos());
                    return true;
                }
                return false;
            }
        });
    }
}
