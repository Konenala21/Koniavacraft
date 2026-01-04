package com.github.nalamodikk.common.screen.block.shared;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionBatchUpdatePacket;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 🎛️ 通用配置介面
 *
 * 實作智能延遲同步系統：
 * - 按鈕點擊立即更新客戶端顯示（視覺反饋）
 * - 300ms 防抖動延遲後批次發送封包
 * - 關閉介面時立即同步所有未保存的變更
 */
public class UniversalConfigScreen extends AbstractContainerScreen<UniversalConfigMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/universal_config.png");
    private static final ResourceLocation BUTTON_TEXTURE_INPUT = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_input.png");
    private static final ResourceLocation BUTTON_TEXTURE_OUTPUT = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_output.png");
    private static final ResourceLocation BUTTON_TEXTURE_BOTH = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_both.png");
    private static final ResourceLocation BUTTON_TEXTURE_DISABLED = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_disabled.png");
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;

    // ⏱️ 延遲同步參數（300ms）
    private static final long SYNC_DELAY_MS = 300;

    // 🔧 靜態 Executor，所有介面實例共用
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "UniversalConfigScreen-Sync");
        thread.setDaemon(true);
        return thread;
    });

    private final EnumMap<Direction, TooltipButton> directionButtonMap = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, IOHandlerUtils.IOType> currentIOMap = new EnumMap<>(Direction.class);
    private final BlockEntity blockEntity;

    // ⏱️ 延遲同步狀態
    private ScheduledFuture<?> pendingSyncTask = null;
    private final Set<Direction> dirtyDirections = new HashSet<>();
    private final EnumMap<Direction, IOHandlerUtils.IOType> originalIOMap = new EnumMap<>(Direction.class);

    public UniversalConfigScreen(UniversalConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;

        // 初始化配置映射
        for (Direction direction : Direction.values()) {
            IOHandlerUtils.IOType type = menu.getIOType(direction);
            currentIOMap.put(direction, type);
            originalIOMap.put(direction, type);
        }
    }

    @Override
    protected void init() {
        super.init();
        int baseX = (this.width - this.imageWidth) / 2 + this.imageWidth / 2 - BUTTON_WIDTH / 2;
        int baseY = (this.height - this.imageHeight) / 2 + this.imageHeight / 2 - BUTTON_HEIGHT / 2;

        updateCurrentConfigFromMenu();
        initDirectionButtons(baseX, baseY);
        updateAllButtonTooltipsAndTextures();
    }

    private void initDirectionButtons(int baseX, int baseY) {
        Direction facing = Direction.NORTH;
        BlockState state = blockEntity.getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        EnumMap<Direction, int[]> directionOffsets = new EnumMap<>(Direction.class);

        Direction front = facing;
        Direction back = front.getOpposite();
        Direction left = front.getCounterClockWise();
        Direction right = front.getClockWise();
        Direction up = Direction.UP;
        Direction down = Direction.DOWN;

        int adjustedBaseY = (this.height - this.imageHeight) / 2 + this.imageHeight / 2 - BUTTON_HEIGHT / 2 - 10;

        directionOffsets.put(Direction.UP,     new int[]{0, -50});
        directionOffsets.put(Direction.DOWN,   new int[]{0, 30});
        directionOffsets.put(front,            new int[]{0, -20});
        directionOffsets.put(back,             new int[]{0, 60});
        directionOffsets.put(left,             new int[]{-60, 0});
        directionOffsets.put(right,            new int[]{60, 0});

        directionButtonMap.clear();

        for (Direction direction : Direction.values()) {
            if (directionOffsets.containsKey(direction)) {
                int[] offset = directionOffsets.get(direction);
                int buttonX = baseX + offset[0];
                int buttonY = adjustedBaseY + offset[1];

                IOHandlerUtils.IOType type = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);

                ResourceLocation currentTexture = switch (type) {
                    case INPUT -> BUTTON_TEXTURE_INPUT;
                    case OUTPUT -> BUTTON_TEXTURE_OUTPUT;
                    case BOTH -> BUTTON_TEXTURE_BOTH;
                    case DISABLED -> BUTTON_TEXTURE_DISABLED;
                };

                Component label = Component.translatable("direction.koniava." + direction.getName());

                TooltipButton button = new TooltipButton(
                        buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        label,
                        currentTexture, 20, 20,
                        btn -> onDirectionConfigButtonClick(direction),
                        () -> Collections.singletonList(getTooltipText(direction))
                );

                directionButtonMap.put(direction, button);
                this.addRenderableWidget(button);
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateCurrentConfigFromMenu();
    }

    private void updateCurrentConfigFromMenu() {
        try {
            boolean hasChanges = false;

            for (Direction direction : Direction.values()) {
                IOHandlerUtils.IOType syncedType = menu.getIOType(direction);
                IOHandlerUtils.IOType currentDisplayed = currentIOMap.get(direction);

                if (syncedType != currentDisplayed && !dirtyDirections.contains(direction)) {
                    currentIOMap.put(direction, syncedType);
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                updateAllButtonTooltipsAndTextures();
            }
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("Error updating config from menu: {}", e.getMessage(), e);
        }
    }

    private void updateAllButtonTooltipsAndTextures() {
        for (Direction direction : Direction.values()) {
            TooltipButton button = directionButtonMap.get(direction);
            if (button != null) {
                updateButtonTooltip(button, direction);
                updateButtonTexture(button, direction);
            }
        }
    }

    /**
     * 🎯 按鈕點擊處理（核心優化點）
     *
     * 流程：
     * 1. 立即更新客戶端顯示（視覺反饋）
     * 2. 標記為 dirty
     * 3. 取消之前的延遲任務
     * 4. 安排新的延遲同步任務（300ms 後）
     */
    private void onDirectionConfigButtonClick(Direction direction) {
        try {
            if (!(blockEntity instanceof IConfigurableBlock)) {
                return;
            }

            // 1️⃣ 立即切換到下一個狀態（純客戶端，無延遲）
            IOHandlerUtils.IOType current = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
            IOHandlerUtils.IOType next = IOHandlerUtils.nextIOType(current);

            currentIOMap.put(direction, next);

            // 2️⃣ 立即更新按鈕顯示（視覺反饋）
            TooltipButton button = directionButtonMap.get(direction);
            if (button != null) {
                updateButtonTexture(button, direction);
                updateButtonTooltip(button, direction);
            }

            // 3️⃣ 標記為需要同步
            dirtyDirections.add(direction);

            // 4️⃣ 取消之前的延遲任務
            if (pendingSyncTask != null && !pendingSyncTask.isDone()) {
                pendingSyncTask.cancel(false);
            }

            // 5️⃣ 安排新的延遲同步任務（300ms 後）
            pendingSyncTask = scheduler.schedule(() -> {
                // 在主線程執行網路操作
                Minecraft.getInstance().execute(this::syncDirtyDirections);
            }, SYNC_DELAY_MS, TimeUnit.MILLISECONDS);

            // 6️⃣ 顯示操作提示（可選）
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                        "message.koniava.config_button_clicked",
                        direction.getName(),
                        Component.translatable("mode.koniava." + next.name().toLowerCase())
                ), true);
            }

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.debug("[Client] Button clicked: {} → {} (sync scheduled)", direction, next);
            }

        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("Error handling direction config button click: {}", e.getMessage(), e);
        }
    }

    /**
     * 📤 同步所有 dirty 的配置到伺服器
     */
    private void syncDirtyDirections() {
        if (dirtyDirections.isEmpty()) {
            return;
        }

        try {
            // 構建批次更新映射
            Map<Direction, IOHandlerUtils.IOType> updatedConfig = new HashMap<>();
            for (Direction direction : dirtyDirections) {
                IOHandlerUtils.IOType newType = currentIOMap.get(direction);
                updatedConfig.put(direction, newType);
            }

            // 發送批次更新封包
            PacketDistributor.sendToServer(new ConfigDirectionBatchUpdatePacket(
                    blockEntity.getBlockPos(),
                    updatedConfig
            ));

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.info("[Client] 📤 Synced {} dirty directions: {}",
                        dirtyDirections.size(), dirtyDirections);
            }

            // 清除 dirty 標記
            dirtyDirections.clear();

        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("Error syncing dirty directions: {}", e.getMessage(), e);
        }
    }

    /**
     * 🚪 關閉介面時的處理
     */
    @Override
    public void onClose() {
        // 1️⃣ 取消待處理的延遲任務
        if (pendingSyncTask != null && !pendingSyncTask.isDone()) {
            pendingSyncTask.cancel(false);
        }

        // 2️⃣ 立即同步所有未保存的變更
        syncDirtyDirections();

        // 3️⃣ 如果還有與原始狀態不同的配置，也要同步
        syncRemainingChanges();

        super.onClose();
    }

    /**
     * 🔄 同步所有與原始狀態不同的配置
     */
    private void syncRemainingChanges() {
        Map<Direction, IOHandlerUtils.IOType> changedConfig = new HashMap<>();

        for (Direction direction : Direction.values()) {
            IOHandlerUtils.IOType original = originalIOMap.get(direction);
            IOHandlerUtils.IOType current = currentIOMap.get(direction);

            if (original != current) {
                changedConfig.put(direction, current);
            }
        }

        if (!changedConfig.isEmpty()) {
            PacketDistributor.sendToServer(new ConfigDirectionBatchUpdatePacket(
                    blockEntity.getBlockPos(),
                    changedConfig
            ));

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.info("[Client] 🔄 Synced {} remaining changes on close", changedConfig.size());
            }
        }
    }

    private void updateButtonTooltip(TooltipButton button, Direction direction) {
        button.setTooltip(null);
        directionButtonMap.put(direction, button);
    }

    private void updateButtonTexture(TooltipButton button, Direction direction) {
        IOHandlerUtils.IOType type = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        ResourceLocation newTexture = switch (type) {
            case INPUT -> BUTTON_TEXTURE_INPUT;
            case OUTPUT -> BUTTON_TEXTURE_OUTPUT;
            case BOTH -> BUTTON_TEXTURE_BOTH;
            case DISABLED -> BUTTON_TEXTURE_DISABLED;
        };
        button.setTexture(newTexture, 20, 20);
    }

    private MutableComponent getTooltipText(Direction direction) {
        IOHandlerUtils.IOType type = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        String configType = type.name().toLowerCase();

        Component localizedDirection = Component.translatable("direction.koniava." + direction.getName());
        Component modeText = Component.translatable("screen.koniava." + configType);

        MutableComponent tooltip = Component.translatable("screen.koniava.configure_side.full", localizedDirection, modeText);

        try {
            if (Screen.hasShiftDown()) {
                tooltip.append("\n")
                        .append(Component.translatable("screen.koniava.debug_world_direction", direction.getName()));
            } else {
                tooltip.append("\n")
                        .append(Component.translatable("screen.koniava.hold_shift"));
            }
        } catch (Exception e) {
            tooltip.append("\n")
                    .append(Component.translatable("screen.koniava.hold_shift"));
            KoniavacraftMod.LOGGER.debug("Could not check shift key state: {}", e.getMessage());
        }

        return tooltip;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
        renderButtonTooltips(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
    }

    private boolean renderButtonTooltips(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        for (Map.Entry<Direction, TooltipButton> entry : directionButtonMap.entrySet()) {
            Direction direction = entry.getKey();
            TooltipButton button = entry.getValue();

            if (button.isMouseOver(mouseX, mouseY)) {
                MutableComponent tooltip = getTooltipText(direction);
                List<FormattedCharSequence> formatted = Minecraft.getInstance().font.split(tooltip, 200);
                pGuiGraphics.renderTooltip(Minecraft.getInstance().font, formatted, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }
}
