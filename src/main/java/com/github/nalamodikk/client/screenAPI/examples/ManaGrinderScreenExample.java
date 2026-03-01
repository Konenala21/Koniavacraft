package com.github.nalamodikk.client.screenAPI.examples;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ArrowProgressWidget;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.github.nalamodikk.client.screenAPI.layout.FlexLayout;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ⚙️ 粉碎机 GUI 界面示例
 *
 * 展示如何使用新的功能：
 * 1. AutoSizedModularScreen - 自动检测图片尺寸
 * 2. FlexLayout - 自动排列 Widget
 *
 * 这是一个参考示例，展示两种使用方式。
 */
public class ManaGrinderScreenExample extends AutoSizedModularScreen<ManaGrinderMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_grinder_gui.png");

    // ============================================
    // 使用方式 1: 自動檢測 PNG 尺寸（推薦）
    // ============================================
    public ManaGrinderScreenExample(ManaGrinderMenu menu, Inventory inv, Component title) {
        // ✨ PNG 是多大，GUI 就是多大（自動檢測）
        super(menu, inv, title, TEXTURE);

        // 如果你的 PNG 是 256×256，但只想用左上角 176×222 区域：
        // super(menu, inv, title, TEXTURE, 176, 222);
    }

    // ============================================
    // 构建 GUI - 示例 1: 不使用布局（手动坐标）
    // ============================================
    protected void buildGuiManual(Panel root) {
        // 传统方式：手动设置坐标
        root.add(new ArrowProgressWidget(79, 35,
            menu::getProgress,
            menu::getMaxProgress
        ));

        root.add(new ManaBarWidget(9, 17,
            menu::getCurrentMana,
            menu::getMaxMana
        ));
    }

    // ============================================
    // 构建 GUI - 示例 2: 使用 FlexLayout（自动布局）
    // ============================================
    @Override
    protected void buildGui(Panel root) {
        // 方案 A: 整个 GUI 使用横向布局
        buildGuiWithRowLayout(root);

        // 方案 B: 使用嵌套布局（更灵活）
        // buildGuiWithNestedLayout(root);
    }

    /**
     * 方案 A: 简单的横向布局
     */
    private void buildGuiWithRowLayout(Panel root) {
        // 设置 root 面板的布局
        root.setLayout(new FlexLayout()
            .direction(FlexLayout.Direction.ROW)  // 横向排列
            .align(FlexLayout.Align.CENTER)       // 垂直居中
            .gap(10)                              // 间距 10px
            .padding(20)                          // 内边距 20px
        );

        // ✨ 添加组件时不需要设置坐标！
        root.add(new ManaBarWidget(0, 0,
            menu::getCurrentMana,
            menu::getMaxMana
        ).setSize(14, 50));  // 只需设置大小

        root.add(new ArrowProgressWidget(0, 0,
            menu::getProgress,
            menu::getMaxProgress
        ));  // 会自动放在魔力条右边
    }

    /**
     * 方案 B: 嵌套布局（更复杂的场景）
     */
    private void buildGuiWithNestedLayout(Panel root) {
        // 创建左侧面板（纵向排列）
        Panel leftPanel = new Panel(10, 10, 30, 200);
        leftPanel.setLayout(new FlexLayout()
            .direction(FlexLayout.Direction.COLUMN)
            .align(FlexLayout.Align.CENTER)
            .gap(5)
        );

        leftPanel.add(new ManaBarWidget(0, 0,
            menu::getCurrentMana,
            menu::getMaxMana
        ).setSize(14, 50));

        // 创建中间面板（横向排列）
        Panel centerPanel = new Panel(50, 30, 100, 50);
        centerPanel.setLayout(new FlexLayout()
            .direction(FlexLayout.Direction.ROW)
            .justify(FlexLayout.Justify.CENTER)
            .align(FlexLayout.Align.CENTER)
        );

        centerPanel.add(new ArrowProgressWidget(0, 0,
            menu::getProgress,
            menu::getMaxProgress
        ));

        // 添加到 root
        root.add(leftPanel);
        root.add(centerPanel);
    }

    // ============================================
    // 其他示例：FlexLayout 的高级用法
    // ============================================

    /**
     * 示例 3: 均匀分布（Space Between）
     */
    @SuppressWarnings("unused")
    private void exampleSpaceBetween(Panel root) {
        root.setLayout(new FlexLayout()
            .direction(FlexLayout.Direction.ROW)
            .justify(FlexLayout.Justify.SPACE_BETWEEN)  // 两端对齐，中间均匀分布
        );

        // Widget1 会在最左边
        // Widget2 会在中间
        // Widget3 会在最右边
    }

    /**
     * 示例 4: 不同的内边距
     */
    @SuppressWarnings("unused")
    private void examplePadding(Panel root) {
        root.setLayout(new FlexLayout()
            .paddingTop(10)
            .paddingBottom(20)
            .paddingLeft(5)
            .paddingRight(5)
        );
    }

    /**
     * 示例 5: 纵向居中排列
     */
    @SuppressWarnings("unused")
    private void exampleColumnCenter(Panel root) {
        root.setLayout(new FlexLayout()
            .direction(FlexLayout.Direction.COLUMN)
            .align(FlexLayout.Align.CENTER)     // 水平居中
            .justify(FlexLayout.Justify.CENTER)  // 垂直居中
            .gap(10)
        );
    }
}
