package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 飛船魔力燃料槽：儲存魔力當燃料，引擎飛行時抽取。接船上的魔力網路補給（INPUT capability，見 ModCapabilities）。
 * 多槽 = 總容量大 = 飛得遠。真身在影子維度 tick，飛行消耗由 ShipEntity 直接抽各燃料槽的 storage。
 * 魔力同步給 client 畫液位 BER：船上的槽靠 ShipEntity 鏡射真身 NBT；世界裡的槽靠 getUpdateTag + 節流 sendBlockUpdated。
 */
public class ManaFuelTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000;

    private final ManaStorage manaStorage = new ManaStorage(CAPACITY, this::onManaChanged);
    private int lastSyncedMana = 0;

    public ManaFuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_FUEL_TANK_BE.get(), pos, state);
    }

    public ManaStorage getManaStorage() {
        return manaStorage;
    }

    /** 魔力變動：標記要存檔，並在世界裡(非影子)變化夠大時節流推一次 client 更新給 BER。 */
    private void onManaChanged() {
        setChanged();
        if (level != null && !level.isClientSide
                && Math.abs(manaStorage.getManaStored() - lastSyncedMana) >= CAPACITY / 40) {
            lastSyncedMana = manaStorage.getManaStored();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries); // 帶魔力給 client 畫液位
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Mana", manaStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Mana")) manaStorage.deserializeNBT(registries, tag.getCompound("Mana"));
    }
}
