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

    // GUI height = 215 to match the full texture layout
    // Machine area: y=17~130  |  inventory separator: y=131~133  |  player inv: y=134~191  |  hotbar: y=192~207
    private static final int GUI_W = 176;
    private static final int GUI_H = 215;

    private EditBox intervalBox;
    private boolean suppressResponder = false;

    public ManaDeployerScreen(ManaDeployerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, GUI_W, GUI_H);
    }

    @Override
    protected void init() {
        super.init();

        // Interval EditBox — same row as "Ticks:" label at panel y=76
        // Label occupies panel x=25~74 (50px), EditBox starts at x=78
        intervalBox = new EditBox(this.font, leftPos + 78, topPos + 74, 62, 12, Component.empty());
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
        if (intervalBox != null && !intervalBox.isFocused()) {
            suppressResponder = true;
            intervalBox.setValue(String.valueOf(menu.getIntervalTick()));
            suppressResponder = false;
        }
    }

    @Override
    protected void buildGui(Panel root) {
        // Mana bar — left strip, taller to use the larger machine area
        root.add(new ManaBarWidget(7, 17, menu::getCurrentMana, menu::getMaxMana)
                .setSize(10, 80).setDrawBackground(false));

        // ON/OFF toggle — right of item slot, aligned horizontally
        root.add(new AbstractWidget(148, 27, 20, 20) {
            @Override
            protected void renderWidget(GuiGraphics g, int lx, int ly, int sx, int sy) {
                boolean en = menu.isEnabled();
                // Outer fill
                g.fill(0, 0, width, height, en ? 0xFF2A7A30 : 0xFF7A2A2A);
                if (isMouseOver(sx, sy)) g.fill(0, 0, width, height, 0x33FFFFFF);
                // Inner highlight border
                g.fill(1, 1, width - 1, height - 1, en ? 0xFF3DB347 : 0xFFB33D3D);
                if (isMouseOver(sx, sy)) g.fill(1, 1, width - 1, height - 1, 0x22FFFFFF);
                Component lbl = Component.literal(en ? "ON" : "OFF");
                g.drawString(ManaDeployerScreen.this.font, lbl,
                        (width - ManaDeployerScreen.this.font.width(lbl)) / 2,
                        (height - 8) / 2, 0xFFFFFF, false);
            }
            @Override
            protected boolean onMouseClicked(int lx, int ly, int button) {
                if (button == 0) { ToggleDeployerEnabledPacket.sendToServer(menu.getBlockPos()); return true; }
                return false;
            }
        });

        // Mode label
        root.add(new AbstractWidget(25, 62, 122, 10) {
            @Override
            protected void renderWidget(GuiGraphics g, int lx, int ly, int sx, int sy) {
                String modeKey = "screen.koniava.mana_deployer.mode."
                        + menu.getMode().name().toLowerCase();
                g.drawString(ManaDeployerScreen.this.font,
                        Component.translatable("screen.koniava.mana_deployer.mode_label",
                                Component.translatable(modeKey)),
                        0, 0, 0x404040, false);
            }
        });

        // "Ticks:" label — EditBox is at absolute leftPos+78, topPos+74
        root.add(new AbstractWidget(25, 76, 50, 10) {
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
        // Inventory label drawn at y=128 (just above the inventory separator bar)
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, 128, 4210752, false);
    }
}
