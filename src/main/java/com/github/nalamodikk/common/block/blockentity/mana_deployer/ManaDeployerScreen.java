package com.github.nalamodikk.common.block.blockentity.mana_deployer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.github.nalamodikk.common.network.packet.server.deployer.SetDeployerIntervalPacket;
import com.github.nalamodikk.common.network.packet.server.deployer.ToggleDeployerEnabledPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ManaDeployerScreen extends AutoSizedModularScreen<ManaDeployerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_deployer_gui_texture.png");

    private EditBox intervalBox;
    private boolean suppressResponder = false;

    public ManaDeployerScreen(ManaDeployerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, 176, 166);
    }

    @Override
    protected void init() {
        super.init();

        // Interval EditBox — absolute screen coords
        intervalBox = new EditBox(this.font, leftPos + 96, topPos + 67, 56, 12, Component.empty());
        intervalBox.setMaxLength(5);
        intervalBox.setFilter(s -> s.matches("\\d*"));
        intervalBox.setValue(String.valueOf(menu.getIntervalTick()));
        intervalBox.setResponder(text -> {
            if (suppressResponder) return;
            try {
                int ticks = Integer.parseInt(text);
                if (ticks >= 10 && ticks <= 12000) {
                    SetDeployerIntervalPacket.sendToServer(menu.getBlockPos(), ticks);
                }
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(intervalBox);
    }

    @Override
    protected void containerTick() {
        // Keep the editbox in sync when not focused
        if (intervalBox != null && !intervalBox.isFocused()) {
            suppressResponder = true;
            intervalBox.setValue(String.valueOf(menu.getIntervalTick()));
            suppressResponder = false;
        }
    }

    @Override
    protected void buildGui(Panel root) {
        root.add(new ManaBarWidget(7, 17, menu::getCurrentMana, menu::getMaxMana)
                .setSize(10, 48).setDrawBackground(false));

        // ON/OFF toggle button (top-right of machine area)
        root.add(new AbstractWidget(152, 54, 18, 18) {
            @Override
            protected void renderWidget(GuiGraphics g, int lx, int ly, int sx, int sy) {
                boolean en = menu.isEnabled();
                g.fill(0, 0, width, height, en ? 0xFF2A7A30 : 0xFF7A2A2A);
                if (isMouseOver(sx, sy)) g.fill(0, 0, width, height, 0x33FFFFFF);
                Component label = Component.literal(en ? "ON" : "OFF");
                int tx = (width - ManaDeployerScreen.this.font.width(label)) / 2;
                int ty = (height - 8) / 2;
                g.drawString(ManaDeployerScreen.this.font, label, tx, ty, 0xFFFFFF, false);
            }

            @Override
            protected boolean onMouseClicked(int lx, int ly, int button) {
                if (button == 0) {
                    ToggleDeployerEnabledPacket.sendToServer(menu.getBlockPos());
                    return true;
                }
                return false;
            }
        });

        // Mode label
        root.add(new AbstractWidget(25, 57, 120, 10) {
            @Override
            protected void renderWidget(GuiGraphics g, int lx, int ly, int sx, int sy) {
                String modeKey = "block.koniava.mana_deployer.mode."
                        + menu.getMode().name().toLowerCase();
                g.drawString(ManaDeployerScreen.this.font,
                        Component.translatable("screen.koniava.mana_deployer.mode_label",
                                Component.translatable(modeKey)),
                        0, 0, 0x404040, false);
            }
        });

        // Speed label (EditBox is added in init() at same row, x=96)
        root.add(new AbstractWidget(25, 69, 68, 10) {
            @Override
            protected void renderWidget(GuiGraphics g, int lx, int ly, int sx, int sy) {
                g.drawString(ManaDeployerScreen.this.font,
                        Component.translatable("screen.koniava.mana_deployer.speed_label"),
                        0, 0, 0x404040, false);
            }
        });
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        // Skip inventory label — not enough vertical space
    }
}
