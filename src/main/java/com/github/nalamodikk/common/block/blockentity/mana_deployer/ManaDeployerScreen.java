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

        // Interval input box — absolute coords
        // x=80 places it right after the "Ticks:" label (which occupies panel x=24~74)
        intervalBox = new EditBox(this.font, leftPos + 80, topPos + 67, 60, 12, Component.empty());
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
        // Sync the box value while it is not being actively edited
        if (intervalBox != null && !intervalBox.isFocused()) {
            suppressResponder = true;
            intervalBox.setValue(String.valueOf(menu.getIntervalTick()));
            suppressResponder = false;
        }
    }

    @Override
    protected void buildGui(Panel root) {
        // Mana bar — left side
        root.add(new ManaBarWidget(7, 17, menu::getCurrentMana, menu::getMaxMana)
                .setSize(10, 48).setDrawBackground(false));

        // ON/OFF toggle button — right side, same row as mode label
        root.add(new AbstractWidget(148, 54, 20, 20) {
            @Override
            protected void renderWidget(GuiGraphics g, int lx, int ly, int sx, int sy) {
                boolean en = menu.isEnabled();
                g.fill(0, 0, width, height, en ? 0xFF2A7A30 : 0xFF7A2A2A);
                if (isMouseOver(sx, sy)) g.fill(0, 0, width, height, 0x33FFFFFF);
                // 1px inner border
                g.fill(1, 1, width - 1, height - 1, en ? 0xFF3DB347 : 0xFFB33D3D);
                if (isMouseOver(sx, sy)) g.fill(1, 1, width - 1, height - 1, 0x22FFFFFF);
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

        // Mode label — fixed short name, no double-prefix
        root.add(new AbstractWidget(24, 57, 122, 10) {
            @Override
            protected void renderWidget(GuiGraphics g, int lx, int ly, int sx, int sy) {
                // Use screen-specific short key to avoid double "Mode:" prefix
                String modeKey = "screen.koniava.mana_deployer.mode."
                        + menu.getMode().name().toLowerCase();
                g.drawString(ManaDeployerScreen.this.font,
                        Component.translatable("screen.koniava.mana_deployer.mode_label",
                                Component.translatable(modeKey)),
                        0, 0, 0x404040, false);
            }
        });

        // "Ticks:" label (EditBox is at absolute leftPos+80, same row)
        root.add(new AbstractWidget(24, 69, 54, 10) {
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
        // Inventory label omitted — not enough vertical space in the machine area
    }
}
