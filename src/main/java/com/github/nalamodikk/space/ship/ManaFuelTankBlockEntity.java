package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 飛船魔力燃料槽：儲存魔力當燃料，引擎飛行時抽取。接船上的魔力網路補給（INPUT capability，見 ModCapabilities）。
 * 多槽 = 總容量大 = 飛得遠。真身在影子維度 tick，飛行消耗由 ShipEntity 直接抽各燃料槽的 storage。
 */
public class ManaFuelTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000;

    private final ManaStorage manaStorage = new ManaStorage(CAPACITY, this::setChanged);

    public ManaFuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_FUEL_TANK_BE.get(), pos, state);
    }

    public ManaStorage getManaStorage() {
        return manaStorage;
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
