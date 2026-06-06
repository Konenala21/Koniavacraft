package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.ship.ShipAssemblePacket;
import com.github.nalamodikk.common.network.packet.server.ship.ShipDisassemblePacket;
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

    // 面板實際內容區（wand 貼圖在 512x512 的左上 509x216）
    private static final int PANEL_W = 509, PANEL_H = 216;

    public ShipAssemblyPadScreen(ShipAssemblyPadMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        // 左側：組裝在上、掃描在下
        addRenderableWidget(Button.builder(
                Component.translatable("screen.koniava.ship_assembly_pad.assemble"),
                b -> ShipAssemblePacket.sendToServer(menu.getPadPos()))
                .bounds(leftPos + 4, topPos + PANEL_H - 54, 90, 20)
                .build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.koniava.ship_assembly_pad.scan"),
                b -> ShipScanPacket.sendToServer(menu.getPadPos()))
                .bounds(leftPos + 4, topPos + PANEL_H - 30, 90, 20)
                .build());
        // 右側藍色面板：拆解（收回附近的飛船），與掃描鈕同高
        addRenderableWidget(Button.builder(
                Component.translatable("screen.koniava.ship_assembly_pad.disassemble"),
                b -> ShipDisassemblePacket.sendToServer(menu.getPadPos()))
                .bounds(leftPos + 375, topPos + PANEL_H - 30, 90, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, PANEL_W, PANEL_H, TEX_W, TEX_H);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // 不呼叫 super：避免畫出預設的「物品欄」label（這 GUI 沒有物品欄）
        // 文字用白色 + 陰影，在藍底/深藍底上都可讀
        g.drawString(font, title, 12, 8, 0xFFFFFF, true);
        int x = 14, y = 32, line = 12;
        g.drawString(font, Component.translatable("screen.koniava.ship_assembly_pad.area",
                        menu.getBoxW(), menu.getBoxH(), menu.getBoxD()),
                x, y, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("screen.koniava.ship_assembly_pad.blocks", menu.getBlockCount()),
                x, y + line, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("screen.koniava.ship_assembly_pad.cores", menu.getCoreCount()),
                x, y + line * 2, 0xFFFFFF, true);
        g.drawString(font, statusText(menu.getStatus()), x, y + line * 3, statusColor(menu.getStatus()), true);
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
        // 亮色（配陰影）：成功綠、其餘紅
        return (status == ShipAssemblyPadBlockEntity.STATUS_OK
                || status == ShipAssemblyPadBlockEntity.STATUS_LAUNCHED) ? 0x55FF55 : 0xFF6666;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
