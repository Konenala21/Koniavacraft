package com.github.nalamodikk.common.block.blockentity.research;

import com.github.nalamodikk.common.item.research.InkQuillItem;
import com.github.nalamodikk.common.item.research.ResearchNoteItem;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ResearchTableMenu extends AbstractContainerMenu {

    private final ResearchTableBlockEntity blockEntity;

    // ── Server-side constructor ───────────────────────────────────────────────

    public ResearchTableMenu(int id, Inventory playerInv, ResearchTableBlockEntity be) {
        super(ModMenuTypes.RESEARCH_TABLE_MENU.get(), id);
        this.blockEntity = be;

        var inv = be.getInventory();

        // Slot 0 — Research Note (left slot, 16×16 item centred in 23×23 interior at texture x=44,y=30)
        this.addSlot(new SlotItemHandler(inv, ResearchTableBlockEntity.NOTE_SLOT, 47, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ResearchNoteItem;
            }
        });

        // Slot 1 — Ink Quill (right slot, interior at texture x=113,y=30)
        this.addSlot(new SlotItemHandler(inv, ResearchTableBlockEntity.QUILL_SLOT, 116, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof InkQuillItem;
            }
        });

        // Player inventory (3 rows) — aligned to texture: first slot content at x=9, y=99
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 9 + col * 18, 99 + row * 18));
            }
        }
        // Hotbar — texture hotbar content at y=157
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 9 + col * 18, 157));
        }
    }

    // ── Client-side constructor (reads BlockPos from buf) ─────────────────────

    public ResearchTableMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf));
    }

    private static ResearchTableBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof ResearchTableBlockEntity rt)) {
            throw new IllegalStateException("Expected ResearchTableBlockEntity at " + pos);
        }
        return rt;
    }

    // ── Accessors for Screen ──────────────────────────────────────────────────

    public ResearchTableBlockEntity getBlockEntity() { return blockEntity; }

    public boolean isReadyToResearch() { return blockEntity.isReadyToResearch(); }

    // ── Shift-click ───────────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy  = stack.copy();

        if (index < 2) {
            // From machine slots → player inventory
            if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // From player inventory → machine slots
            if (!this.moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
