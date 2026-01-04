package com.github.nalamodikk.client.screenAPI.framework;

import com.github.nalamodikk.client.screenAPI.utils.TextureInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 自动尺寸的模块化界面基类
 *
 * 特点：
 * 1. 自动检测纹理尺寸 - 不需要手动设置 imageWidth/imageHeight
 * 2. 自动绘制背景 - 不需要覆写 renderBg
 * 3. 支持 FlexLayout - 子组件可以使用自动布局
 *
 * 使用示例：
 * <pre>{@code
 * public class MyScreen extends AutoSizedModularScreen<MyMenu> {
 *     private static final ResourceLocation TEXTURE = rl("textures/gui/my_gui.png");
 *
 *     public MyScreen(MyMenu menu, Inventory inv, Component title) {
 *         // 自动检测 PNG 尺寸
 *         super(menu, inv, title, TEXTURE);
 *
 *         // 或者手动指定 GUI 尺寸（从 PNG 的一部分截取）
 *         // super(menu, inv, title, TEXTURE, 176, 222);
 *     }
 *
 *     @Override
 *     protected void buildGui(Panel root) {
 *         // 使用 FlexLayout 自动排列
 *         root.setLayout(new FlexLayout()
 *             .direction(FlexLayout.Direction.ROW)
 *             .gap(5)
 *             .padding(10)
 *         );
 *
 *         root.add(new ManaBarWidget(...));
 *         root.add(new ArrowProgressWidget(...));
 *         // Widget 位置会自动计算！
 *     }
 * }
 * }</pre>
 */
public abstract class AutoSizedModularScreen<T extends AbstractContainerMenu>
        extends ModularScreen<T> {

    protected final ResourceLocation backgroundTexture;
    protected final int textureWidth;
    protected final int textureHeight;
    protected final int uOffset;
    protected final int vOffset;

    /**
     * 自动检测纹理尺寸
     *
     * @param texture 背景纹理（会自动检测 PNG 尺寸作为 GUI 大小）
     */
    public AutoSizedModularScreen(T menu, Inventory playerInventory, Component title,
                                   ResourceLocation texture) {
        super(menu, playerInventory, title);

        this.backgroundTexture = texture;

        // 🎯 自动检测纹理尺寸
        TextureInfo.Size size = TextureInfo.getSize(texture);
        this.imageWidth = size.width();
        this.imageHeight = size.height();
        this.textureWidth = size.width();
        this.textureHeight = size.height();
        this.uOffset = 0;
        this.vOffset = 0;
    }

    /**
     * 手动指定 GUI 尺寸（从 PNG 中截取部分区域）
     *
     * @param texture 背景纹理
     * @param guiWidth GUI 显示宽度
     * @param guiHeight GUI 显示高度
     */
    public AutoSizedModularScreen(T menu, Inventory playerInventory, Component title,
                                   ResourceLocation texture,
                                   int guiWidth, int guiHeight) {
        super(menu, playerInventory, title);

        this.backgroundTexture = texture;
        this.imageWidth = guiWidth;
        this.imageHeight = guiHeight;

        // 自动检测 PNG 总尺寸
        TextureInfo.Size size = TextureInfo.getSize(texture);
        this.textureWidth = size.width();
        this.textureHeight = size.height();
        this.uOffset = 0;
        this.vOffset = 0;
    }

    /**
     * 完整构造函数 - 从 PNG 指定位置截取
     *
     * @param texture 背景纹理
     * @param guiWidth GUI 显示宽度
     * @param guiHeight GUI 显示高度
     * @param uOffset UV 起点 X（在 PNG 中的位置）
     * @param vOffset UV 起点 Y（在 PNG 中的位置）
     * @param texWidth PNG 总宽度（可选，默认自动检测）
     * @param texHeight PNG 总高度（可选，默认自动检测）
     */
    public AutoSizedModularScreen(T menu, Inventory playerInventory, Component title,
                                   ResourceLocation texture,
                                   int guiWidth, int guiHeight,
                                   int uOffset, int vOffset,
                                   int texWidth, int texHeight) {
        super(menu, playerInventory, title);

        this.backgroundTexture = texture;
        this.imageWidth = guiWidth;
        this.imageHeight = guiHeight;
        this.uOffset = uOffset;
        this.vOffset = vOffset;

        // 如果指定了 0，则自动检测
        if (texWidth <= 0 || texHeight <= 0) {
            TextureInfo.Size size = TextureInfo.getSize(texture);
            this.textureWidth = size.width();
            this.textureHeight = size.height();
        } else {
            this.textureWidth = texWidth;
            this.textureHeight = texHeight;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // ✨ 自动绘制背景图
        graphics.blit(backgroundTexture,
            this.leftPos, this.topPos,
            uOffset, vOffset,
            this.imageWidth, this.imageHeight,
            textureWidth, textureHeight
        );

        // 绘制 Widget
        super.renderBg(graphics, partialTick, mouseX, mouseY);
    }
}
