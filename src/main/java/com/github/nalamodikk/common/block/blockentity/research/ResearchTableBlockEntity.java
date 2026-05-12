package com.github.nalamodikk.common.block.blockentity.research;

import com.github.nalamodikk.common.item.research.CompletedResearchItem;
import com.github.nalamodikk.common.item.research.InkQuillItem;
import com.github.nalamodikk.common.item.research.ResearchNoteItem;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResearchTableBlockEntity extends BlockEntity {

    public static final int NOTE_SLOT = 0;
    public static final int QUILL_SLOT = 1;

    @Nullable private ResourceLocation activeResearchId;
    private final Map<String, String> savedPlacements = new LinkedHashMap<>();

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case NOTE_SLOT -> stack.getItem() instanceof ResearchNoteItem;
                case QUILL_SLOT -> stack.getItem() instanceof InkQuillItem;
                default -> false;
            };
        }
    };

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Nullable
    public ResourceLocation getCurrentResearchId() {
        ItemStack note = inventory.getStackInSlot(NOTE_SLOT);
        return note.isEmpty() ? null : ResearchNoteItem.getResearchId(note);
    }

    public boolean isReadyToResearch() {
        ItemStack note = inventory.getStackInSlot(NOTE_SLOT);
        ItemStack quill = inventory.getStackInSlot(QUILL_SLOT);
        if (note.isEmpty() || quill.isEmpty()) {
            return false;
        }
        return ResearchNoteItem.getResearchId(note) != null
                && quill.getItem() instanceof InkQuillItem;
    }

    public void onResearchComplete(@Nullable Player player) {
        ItemStack quill = inventory.getStackInSlot(QUILL_SLOT);
        if (!quill.isEmpty() && player != null) {
            quill.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            inventory.setStackInSlot(QUILL_SLOT, quill);
        }

        ItemStack note = inventory.getStackInSlot(NOTE_SLOT);
        if (!note.isEmpty() && player != null) {
            ResourceLocation researchId = ResearchNoteItem.getResearchId(note);
            if (researchId != null) {
                ItemStack completed = CompletedResearchItem.forResearch(researchId);
                if (!player.getInventory().add(completed)) {
                    player.drop(completed, false);
                }
            }
        }

        inventory.setStackInSlot(NOTE_SLOT, ItemStack.EMPTY);
        clearGridState();
        setChanged();
    }

    /**
     * Saves a grid placement under the active research. When the research id changes,
     * old placements are discarded to avoid leaking puzzle state between notes.
     */
    public void saveCellPlacement(ResourceLocation researchId, int q, int r,
                                  @Nullable ResourceLocation aspectId) {
        if (!researchId.equals(activeResearchId)) {
            activeResearchId = researchId;
            savedPlacements.clear();
        }
        String key = q + "," + r;
        if (aspectId == null) {
            savedPlacements.remove(key);
        } else {
            savedPlacements.put(key, aspectId.toString());
        }
        setChanged();
        sync();
    }

    public void clearGridState() {
        activeResearchId = null;
        savedPlacements.clear();
        sync();
    }

    @Nullable
    public ResourceLocation getActiveResearchId() {
        return activeResearchId;
    }

    public Map<String, String> getSavedPlacements() {
        return Collections.unmodifiableMap(savedPlacements);
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (activeResearchId != null) {
            tag.putString("ActiveResearch", activeResearchId.toString());
            CompoundTag placements = new CompoundTag();
            savedPlacements.forEach(placements::putString);
            tag.put("Placements", placements);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        savedPlacements.clear();
        activeResearchId = null;
        if (tag.contains("ActiveResearch")) {
            activeResearchId = ResourceLocation.parse(tag.getString("ActiveResearch"));
            if (tag.contains("Placements")) {
                CompoundTag placements = tag.getCompound("Placements");
                placements.getAllKeys().forEach(key -> savedPlacements.put(key, placements.getString(key)));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
