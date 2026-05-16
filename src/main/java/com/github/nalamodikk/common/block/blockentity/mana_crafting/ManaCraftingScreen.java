package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.component.ResearchLockWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.ButtonWidget;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 🧪 魔力合成台 GUI 界面 (v2 - 自動尺寸)
 */
public class ManaCraftingScreen extends AutoSizedModularScreen<ManaCraftingMenu> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_crafting_table_gui.png");
    private static final ResourceLocation UPGRADE_BUTTON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/upgrade_button.png");

    public ManaCraftingScreen(ManaCraftingMenu container, Inventory inv, Component title) {
        // ✨ v2: 自動檢測尺寸並繪製背景
        super(container, inv, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        // 0. 研究鎖定遮罩
        root.add(new ResearchLockWidget(0, 0, imageWidth, imageHeight, "mana_crafting_table"));

        // 魔力條 (11, 19) - 7x47
        root.add(new ManaBarWidget(11, 19,
            menu::getManaStored,
            () -> ManaCraftingTableBlockEntity.MAX_MANA
        ).setSize(7, 47).setDrawBackground(false));

        // 升級按鈕 (150, 5)
        root.add(new ButtonWidget(150, 5, 18, 18, UPGRADE_BUTTON_TEXTURE, 18, 18, btn -> {
            ManaCraftingTableBlockEntity be = this.menu.getBlockEntity();
            if (be == null) return;
            OpenUpgradeGuiPacket.sendToServer(be.getBlockPos());
        }).setTooltip(() -> List.of(Component.translatable("screen.koniava.upgrade_button.tooltip"))));
    }

    // ✨ v2: renderBg 完全刪除，AutoSizedModularScreen 會自動處理
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        if (menu.shouldShowMissingResearchWarning()) {
            Component warning = Component.translatable("gui.koniava.mana_crafting.missing_research");
            int x = (imageWidth - font.width(warning)) / 2;
            graphics.drawString(font, warning, x, 6, 0xAA2E1F, false);
        }
    }
}
