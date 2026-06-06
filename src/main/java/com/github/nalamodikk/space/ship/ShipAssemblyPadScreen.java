package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.ship.ShipAssemblePacket;
import com.github.nalamodikk.common.network.packet.server.ship.ShipScanPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ShipAssemblyPadScreen extends AbstractContainerScreen<ShipAssemblyPadMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/ship_assembly_pad.png");
    private static final int TEX_W = 512, TEX_H = 512;

    public ShipAssemblyPadScreen(ShipAssemblyPadMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 473;
        this.imageHeight = 210;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 12;
        this.titleLabelY = 10;
        addRenderableWidget(Button.builder(
                Component.translatable("screen.koniava.ship_assembly_pad.scan"),
                b -> ShipScanPacket.sendToServer(menu.getPadPos()))
                .bounds(leftPos + 20, topPos + 170, 90, 20)
                .build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.koniava.ship_assembly_pad.assemble"),
                b -> ShipAssemblePacket.sendToServer(menu.getPadPos()))
                .bounds(leftPos + 120, topPos + 170, 90, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int x = 14, y = 34, line = 12;
        g.drawString(font, Component.translatable("screen.koniava.ship_assembly_pad.area",
                        menu.getBoxW(), menu.getBoxH(), menu.getBoxD()),
                x, y, 0x404040, false);
        g.drawString(font, Component.translatable("screen.koniava.ship_assembly_pad.blocks", menu.getBlockCount()),
                x, y + line, 0x404040, false);
        g.drawString(font, Component.translatable("screen.koniava.ship_assembly_pad.cores", menu.getCoreCount()),
                x, y + line * 2, 0x404040, false);
        g.drawString(font, statusText(menu.getStatus()), x, y + line * 3, statusColor(menu.getStatus()), false);
    }

    private static Component statusText(int status) {
        String key = switch (status) {
            case ShipAssemblyPadBlockEntity.STATUS_OK -> "ok";
            case ShipAssemblyPadBlockEntity.STATUS_NO_CORE -> "no_core";
            case ShipAssemblyPadBlockEntity.STATUS_MULTI_CORE -> "multi_core";
            case ShipAssemblyPadBlockEntity.STATUS_FAILED -> "failed";
            case ShipAssemblyPadBlockEntity.STATUS_NO_BASE -> "no_base";
            case ShipAssemblyPadBlockEntity.STATUS_TOO_BIG -> "too_big";
            case ShipAssemblyPadBlockEntity.STATUS_LAUNCHED -> "launched";
            default -> "idle";
        };
        return Component.translatable("screen.koniava.ship_assembly_pad.status." + key);
    }

    private static int statusColor(int status) {
        return (status == ShipAssemblyPadBlockEntity.STATUS_OK
                || status == ShipAssemblyPadBlockEntity.STATUS_LAUNCHED) ? 0x2E7D32 : 0xB71C1C;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
