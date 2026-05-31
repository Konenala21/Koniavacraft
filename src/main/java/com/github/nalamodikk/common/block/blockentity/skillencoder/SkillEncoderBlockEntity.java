package com.github.nalamodikk.common.block.blockentity.skillencoder;

import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the single spell core being edited at the Skill Core Encoding Bench.
 * No ticking, no mana: encoding is a one-shot write driven by the screen, so the
 * block entity only needs to keep the core item and open the menu.
 */
public class SkillEncoderBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CORE_SLOT = 0;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Only wand cores can carry skills; the encode step validates the rest.
            return stack.getItem() instanceof IWandCore;
        }
    };

    public SkillEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SKILL_ENCODER_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public ItemStack getCore() {
        return items.getStackInSlot(CORE_SLOT);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.skill_encoder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SkillEncoderMenu(containerId, playerInventory, this);
    }
}
