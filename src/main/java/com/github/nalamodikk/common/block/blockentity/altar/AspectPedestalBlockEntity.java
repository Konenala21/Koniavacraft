package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.common.multiblock.AbstractMultiblockPartBlockEntity;
import com.github.nalamodikk.common.multiblock.api.IMultiblockController;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class AspectPedestalBlockEntity extends AbstractMultiblockPartBlockEntity {

    private ItemStack heldItem = ItemStack.EMPTY;

    public AspectPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASPECT_PEDESTAL_BE.get(), pos, state);
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    // 客戶端樂觀更新，避免 BER 在封包到達前顯示舊物品
    public void setHeldItemClientSide(ItemStack stack) {
        if (level != null && level.isClientSide()) {
            heldItem = stack;
        }
    }

    public ItemStack insertItem(ItemStack stack) {
        if (!heldItem.isEmpty()) return stack;
        heldItem = stack.copyWithCount(1);
        stack.shrink(1);
        setChanged();
        notifyBlockUpdate();
        return stack;
    }

    public ItemStack extractItem() {
        if (heldItem.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = heldItem.copy();
        heldItem = ItemStack.EMPTY;
        setChanged();
        notifyBlockUpdate();
        return result;
    }

    public void consumeItem() {
        heldItem = ItemStack.EMPTY;
        setChanged();
        notifyBlockUpdate();
    }

    public IItemHandler getItemHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() { return 1; }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return slot == 0 ? heldItem : ItemStack.EMPTY;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (slot != 0 || stack.isEmpty() || !heldItem.isEmpty()) return stack;
                if (!simulate) {
                    heldItem = stack.copyWithCount(1);
                    setChanged();
                    notifyBlockUpdate();
                }
                ItemStack remainder = stack.copy();
                remainder.shrink(1);
                return remainder;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != 0 || heldItem.isEmpty() || amount <= 0) return ItemStack.EMPTY;
                ItemStack result = heldItem.copyWithCount(1);
                if (!simulate) {
                    heldItem = ItemStack.EMPTY;
                    setChanged();
                    notifyBlockUpdate();
                }
                return result;
            }

            @Override
            public int getSlotLimit(int slot) { return 1; }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot == 0 && !stack.isEmpty();
            }
        };
    }

    private void notifyBlockUpdate() {
        if (level == null || level.isClientSide()) return;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        // 直接廣播 BE 資料封包，確保客戶端即時收到物品狀態
        if (level instanceof ServerLevel serverLevel) {
            Packet<ClientGamePacketListener> packet = getUpdatePacket();
            if (packet != null) {
                serverLevel.getPlayers(p -> p.distanceToSqr(worldPosition.getX(),
                        worldPosition.getY(), worldPosition.getZ()) < 64 * 64)
                        .forEach(p -> p.connection.send(packet));
            }
        }
    }

    @Override
    protected void onAddedToController(IMultiblockController controller) {
        notifyBlockUpdate();
    }

    @Override
    protected void onRemovedFromController() {
        notifyBlockUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!heldItem.isEmpty()) {
            tag.put("HeldItem", heldItem.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heldItem = tag.contains("HeldItem")
                ? ItemStack.parseOptional(registries, tag.getCompound("HeldItem"))
                : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
