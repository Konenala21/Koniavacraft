package com.github.nalamodikk.client.screenAPI.layout;

import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;

import java.util.List;

/**
 * FlexBox 风格的布局管理器
 *
 * 功能：
 * - 自动计算子组件的位置
 * - 支持横向/纵向排列
 * - 支持间距和内边距
 * - 支持对齐方式
 *
 * 参考 LDLib2 的 Yoga FlexBox 布局概念
 *
 * 使用示例：
 * <pre>{@code
 * Panel panel = new Panel(0, 0, 200, 100);
 * panel.setLayout(new FlexLayout()
 *     .direction(FlexLayout.Direction.ROW)
 *     .gap(5)
 *     .padding(10)
 *     .align(FlexLayout.Align.CENTER)
 * );
 * panel.add(new Widget1());
 * panel.add(new Widget2());
 * // Widget 位置会自动计算
 * }</pre>
 */
public class FlexLayout {

    /**
     * 布局方向
     */
    public enum Direction {
        /** 横向排列（从左到右） */
        ROW,
        /** 纵向排列（从上到下） */
        COLUMN
    }

    /**
     * 对齐方式
     */
    public enum Align {
        /** 起始位置对齐 */
        START,
        /** 居中对齐 */
        CENTER,
        /** 结束位置对齐 */
        END,
        /** 拉伸填充（暂不实现） */
        STRETCH
    }

    /**
     * 主轴对齐（Justify Content）
     */
    public enum Justify {
        /** 起始对齐 */
        START,
        /** 居中对齐 */
        CENTER,
        /** 结束对齐 */
        END,
        /** 两端对齐，组件间均匀分布 */
        SPACE_BETWEEN,
        /** 周围均匀分布 */
        SPACE_AROUND
    }

    // === 布局属性 ===
    private Direction direction = Direction.ROW;
    private Align align = Align.START;
    private Justify justify = Justify.START;
    private int gap = 0;
    private int paddingTop = 0;
    private int paddingRight = 0;
    private int paddingBottom = 0;
    private int paddingLeft = 0;

    // === 链式配置方法 ===

    public FlexLayout direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    public FlexLayout align(Align align) {
        this.align = align;
        return this;
    }

    public FlexLayout justify(Justify justify) {
        this.justify = justify;
        return this;
    }

    public FlexLayout gap(int gap) {
        this.gap = gap;
        return this;
    }

    public FlexLayout padding(int padding) {
        this.paddingTop = padding;
        this.paddingRight = padding;
        this.paddingBottom = padding;
        this.paddingLeft = padding;
        return this;
    }

    public FlexLayout paddingVertical(int padding) {
        this.paddingTop = padding;
        this.paddingBottom = padding;
        return this;
    }

    public FlexLayout paddingHorizontal(int padding) {
        this.paddingLeft = padding;
        this.paddingRight = padding;
        return this;
    }

    public FlexLayout paddingTop(int padding) {
        this.paddingTop = padding;
        return this;
    }

    public FlexLayout paddingRight(int padding) {
        this.paddingRight = padding;
        return this;
    }

    public FlexLayout paddingBottom(int padding) {
        this.paddingBottom = padding;
        return this;
    }

    public FlexLayout paddingLeft(int padding) {
        this.paddingLeft = padding;
        return this;
    }

    // === 布局计算 ===

    /**
     * 应用布局到子组件列表
     *
     * @param children 子组件列表
     * @param containerWidth 容器宽度
     * @param containerHeight 容器高度
     */
    public void apply(List<AbstractWidget> children, int containerWidth, int containerHeight) {
        if (children.isEmpty()) return;

        if (direction == Direction.ROW) {
            applyRowLayout(children, containerWidth, containerHeight);
        } else {
            applyColumnLayout(children, containerWidth, containerHeight);
        }
    }

    /**
     * 横向布局
     */
    private void applyRowLayout(List<AbstractWidget> children, int containerWidth, int containerHeight) {
        // 计算可用空间
        int availableWidth = containerWidth - paddingLeft - paddingRight;
        int availableHeight = containerHeight - paddingTop - paddingBottom;

        // 计算总宽度
        int totalWidth = 0;
        for (AbstractWidget child : children) {
            totalWidth += child.getWidth();
        }
        totalWidth += gap * (children.size() - 1);

        // 计算起始位置（根据 justify）
        int startX = paddingLeft;
        int extraSpace = availableWidth - totalWidth;

        switch (justify) {
            case CENTER -> startX += extraSpace / 2;
            case END -> startX += extraSpace;
            case SPACE_BETWEEN -> {
                // gap 会在循环中动态计算
            }
            case SPACE_AROUND -> {
                startX += extraSpace / (children.size() + 1);
            }
        }

        // 动态 gap（用于 SPACE_BETWEEN）
        int dynamicGap = gap;
        if (justify == Justify.SPACE_BETWEEN && children.size() > 1) {
            dynamicGap = extraSpace / (children.size() - 1);
        } else if (justify == Justify.SPACE_AROUND && children.size() > 0) {
            dynamicGap = extraSpace / (children.size() + 1);
        }

        // 排列子组件
        int currentX = startX;
        for (AbstractWidget child : children) {
            child.setX(currentX);

            // 计算 Y 坐标（根据 align）
            switch (align) {
                case START -> child.setY(paddingTop);
                case CENTER -> child.setY(paddingTop + (availableHeight - child.getHeight()) / 2);
                case END -> child.setY(paddingTop + availableHeight - child.getHeight());
                case STRETCH -> {
                    child.setY(paddingTop);
                    // 暂不实现高度拉伸
                }
            }

            currentX += child.getWidth() + dynamicGap;
        }
    }

    /**
     * 纵向布局
     */
    private void applyColumnLayout(List<AbstractWidget> children, int containerWidth, int containerHeight) {
        // 计算可用空间
        int availableWidth = containerWidth - paddingLeft - paddingRight;
        int availableHeight = containerHeight - paddingTop - paddingBottom;

        // 计算总高度
        int totalHeight = 0;
        for (AbstractWidget child : children) {
            totalHeight += child.getHeight();
        }
        totalHeight += gap * (children.size() - 1);

        // 计算起始位置
        int startY = paddingTop;
        int extraSpace = availableHeight - totalHeight;

        switch (justify) {
            case CENTER -> startY += extraSpace / 2;
            case END -> startY += extraSpace;
            case SPACE_BETWEEN -> {
                // gap 会在循环中动态计算
            }
            case SPACE_AROUND -> {
                startY += extraSpace / (children.size() + 1);
            }
        }

        // 动态 gap
        int dynamicGap = gap;
        if (justify == Justify.SPACE_BETWEEN && children.size() > 1) {
            dynamicGap = extraSpace / (children.size() - 1);
        } else if (justify == Justify.SPACE_AROUND && children.size() > 0) {
            dynamicGap = extraSpace / (children.size() + 1);
        }

        // 排列子组件
        int currentY = startY;
        for (AbstractWidget child : children) {
            child.setY(currentY);

            // 计算 X 坐标
            switch (align) {
                case START -> child.setX(paddingLeft);
                case CENTER -> child.setX(paddingLeft + (availableWidth - child.getWidth()) / 2);
                case END -> child.setX(paddingLeft + availableWidth - child.getWidth());
                case STRETCH -> {
                    child.setX(paddingLeft);
                    // 暂不实现宽度拉伸
                }
            }

            currentY += child.getHeight() + dynamicGap;
        }
    }
}
