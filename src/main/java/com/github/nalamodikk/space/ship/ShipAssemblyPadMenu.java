package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * 組裝台菜單：沒有物品槽，只用 ContainerData 把掃描結果（方塊數/核心數/狀態）同步給 client。
 * 掃描按鈕在 Screen 端送 ShipScanPacket 到 server。
 */
public class ShipAssemblyPadMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final BlockPos pos;
    private final ContainerLevelAccess access;

    // server 端
    public ShipAssemblyPadMenu(int id, Inventory inv, ShipAssemblyPadBlockEntity be) {
        super(ModMenuTypes.SHIP_ASSEMBLY_PAD_MENU.get(), id);
        this.data = be.getData();
        this.pos = be.getBlockPos();
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        addDataSlots(data);
    }

    // client 端（從 buf 讀 pos，data 用本地空殼，靠 dataslots 同步填值）
    public ShipAssemblyPadMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.SHIP_ASSEMBLY_PAD_MENU.get(), id);
        this.data = new SimpleContainerData(6);
        this.pos = buf.readBlockPos();
        this.access = ContainerLevelAccess.NULL;
        addDataSlots(data);
    }

    public BlockPos getPadPos() { return pos; }
    public int getBlockCount() { return data.get(ShipAssemblyPadBlockEntity.DATA_COUNT); }
    public int getCoreCount()  { return data.get(ShipAssemblyPadBlockEntity.DATA_CORES); }
    public int getStatus()     { return data.get(ShipAssemblyPadBlockEntity.DATA_STATUS); }
    public int getBoxW()       { return data.get(ShipAssemblyPadBlockEntity.DATA_BOX_W); }
    public int getBoxH()       { return data.get(ShipAssemblyPadBlockEntity.DATA_BOX_H); }
    public int getBoxD()       { return data.get(ShipAssemblyPadBlockEntity.DATA_BOX_D); }

    @Override
    public boolean stillValid(Player player) {
        return access == ContainerLevelAccess.NULL
                || stillValid(access, player, ModBlocks.SHIP_ASSEMBLY_PAD.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
