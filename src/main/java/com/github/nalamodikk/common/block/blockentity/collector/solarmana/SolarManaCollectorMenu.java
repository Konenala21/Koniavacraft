
package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.common.block.blockentity.collector.solarmana.sync.SolarCollectorSyncHelper;
import com.github.nalamodikk.register.ModMenuTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class SolarManaCollectorMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final SolarManaCollectorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final SolarCollectorSyncHelper syncHelper;

    // 🎯 伺服器端構造函數
    public SolarManaCollectorMenu(int id, Inventory inv, SolarManaCollectorBlockEntity blockEntity) {
        super(ModMenuTypes.SOLAR_MANA_COLLECTOR_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.syncHelper = blockEntity.getSyncHelper();

        // 🔧 關鍵修復：強制立即同步最新狀態
        this.syncHelper.syncFrom(blockEntity);
        this.addDataSlots(syncHelper.getContainerData());

        addPlayerInventorySlots(inv, 8, 84);
        addPlayerHotbarSlots(inv, 8, 142);

//        LOGGER.debug("🎮 伺服器端 Menu 創建完成，同步狀態: generating={}", blockEntity.isCurrentlyGenerating());
    }

    // 🔧 客戶端構造函數
    public SolarManaCollectorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.SOLAR_MANA_COLLECTOR_MENU.get(), id);

        // 📍 獲取 BlockEntity
        this.blockEntity = (SolarManaCollectorBlockEntity) inv.player.level()
                .getBlockEntity(buf.readBlockPos());

        if (this.blockEntity == null) {
            throw new IllegalStateException("BlockEntity is null on client!");
        }

        this.access = ContainerLevelAccess.create(inv.player.level(), blockEntity.getBlockPos());
        this.syncHelper = blockEntity.getSyncHelper();

        // 🔧 客戶端：重置同步狀態並添加數據槽位
        syncHelper.resetSyncState();
        this.addDataSlots(syncHelper.getContainerData());

        addPlayerInventorySlots(inv, 8, 84);
        addPlayerHotbarSlots(inv, 8, 142);

        // 🔍 調試
//        LOGGER.debug("🎮 客戶端 Menu 創建完成");
    }

    public void addPlayerInventorySlots(Inventory inv, int startX, int startY) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9;
                int x = startX + col * 18;
                int y = startY + row * 18;
                this.addSlot(new Slot(inv, index, x, y));
            }
        }
    }

    public void addPlayerHotbarSlots(Inventory inv, int startX, int startY) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, startX + col * 18, startY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    public SolarManaCollectorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getManaStored() {
        return syncHelper.getManaStored();
    }

    public int getMaxMana() {
        return syncHelper.getMaxMana();
    }

    public boolean isGenerating() {
        return syncHelper.isGenerating();
    }


    public boolean isDaytime() {
        return syncHelper.isDaytime();
    }

    public boolean isOverworld() {
        return syncHelper.isOverworld();
    }

    public boolean hasSkyLight() {
        return syncHelper.hasSkyLight();
    }

    public boolean isOpenToSky() {
        return syncHelper.isOpenToSky();
    }

    public boolean isRaining() {
        return syncHelper.isRaining();
    }

    public boolean isThundering() {
        return syncHelper.isThundering();
    }

    // 🔧 修復的 getter 方法
    public int getSpeedLevel() {
        return syncHelper.getSpeedLevel();
    }

    public int getEfficiencyLevel() {
        return syncHelper.getEfficiencyLevel();
    }

    // 🆕 檢查同步狀態
    public boolean hasValidUpgradeData() {
        return syncHelper.hasValidUpgradeData();
    }
}
