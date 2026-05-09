package com.github.nalamodikk.common.block.blockentity.research;

import com.github.nalamodikk.common.item.research.CompletedResearchItem;
import com.github.nalamodikk.common.item.research.InkQuillItem;
import com.github.nalamodikk.common.item.research.ResearchNoteItem;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResearchTableBlockEntity extends BlockEntity {

    public static final int NOTE_SLOT  = 0;
    public static final int QUILL_SLOT = 1;

    // Grid persistence: "q,r" → aspect ResourceLocation string
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
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return switch (slot) {
                case NOTE_SLOT  -> stack.getItem() instanceof ResearchNoteItem;
                case QUILL_SLOT -> stack.getItem() instanceof InkQuillItem;
                default -> false;
            };
        }
    };

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE_BE.get(), pos, state);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public ItemStackHandler getInventory() { return inventory; }

    /** Returns the researchId stored in the note currently in slot 0, or null. */
    @Nullable
    public ResourceLocation getCurrentResearchId() {
        var note = inventory.getStackInSlot(NOTE_SLOT);
        return note.isEmpty() ? null : ResearchNoteItem.getResearchId(note);
    }

    /** True when both a valid research note and an ink quill are present. */
    public boolean isReadyToResearch() {
        var note  = inventory.getStackInSlot(NOTE_SLOT);
        var quill = inventory.getStackInSlot(QUILL_SLOT);
        if (note.isEmpty() || quill.isEmpty()) return false;
        return ResearchNoteItem.getResearchId(note) != null
                && quill.getItem() instanceof InkQuillItem;
    }

    /** Consume one quill charge, transform the note into a completed scroll, and return it. */
    public void onResearchComplete(@Nullable net.minecraft.world.entity.player.Player player) {
        var quill = inventory.getStackInSlot(QUILL_SLOT);
        if (!quill.isEmpty() && player != null) {
            InkQuillItem.useCharge(quill, player);
            if (quill.isEmpty()) inventory.setStackInSlot(QUILL_SLOT, net.minecraft.world.item.ItemStack.EMPTY);
        }

        // Transform note → completed research scroll and give back to player
        var note = inventory.getStackInSlot(NOTE_SLOT);
        if (!note.isEmpty() && player != null) {
            net.minecraft.resources.ResourceLocation researchId = ResearchNoteItem.getResearchId(note);
            if (researchId != null) {
                var completed = CompletedResearchItem.forResearch(researchId);
                if (!player.getInventory().add(completed)) {
                    player.drop(completed, false);
                }
            }
        }

        inventory.setStackInSlot(NOTE_SLOT, net.minecraft.world.item.ItemStack.EMPTY);
        clearGridState();
        setChanged();
    }

    // ── Grid persistence ──────────────────────────────────────────────────────

    /**
     * Called by the server packet handler each time a cell is placed or removed.
     * If the researchId changed, the old placements are discarded first.
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
    public ResourceLocation getActiveResearchId() { return activeResearchId; }

    public Map<String, String> getSavedPlacements() {
        return Collections.unmodifiableMap(savedPlacements);
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

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
                CompoundTag p = tag.getCompound("Placements");
                p.getAllKeys().forEach(k -> savedPlacements.put(k, p.getString(k)));
            }
        }
    }

    // ── Sync ─────────────────────────────────────────────────────────────────

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
