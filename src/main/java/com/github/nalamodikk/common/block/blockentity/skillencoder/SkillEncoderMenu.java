package com.github.nalamodikk.common.block.blockentity.skillencoder;

import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SkillEncoderMenu extends AbstractContainerMenu {

    public static final int INV_X = 50;
    public static final int CORE_SLOT_X = 207;
    public static final int CORE_SLOT_Y = 11;
    private static final int INV_MAIN_Y = 171;
    private static final int INV_HOTBAR_Y = 229;

    private final SkillEncoderBlockEntity blockEntity;

    public SkillEncoderMenu(int containerId, Inventory playerInventory, SkillEncoderBlockEntity blockEntity) {
        super(ModMenuTypes.SKILL_ENCODER_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        this.addSlot(new SlotItemHandler(blockEntity.getItems(), SkillEncoderBlockEntity.CORE_SLOT,
                CORE_SLOT_X, CORE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof IWandCore;
            }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INV_X + col * 18, INV_MAIN_Y + row * 18));

        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, INV_HOTBAR_Y));
    }

    public SkillEncoderBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public ItemStack getCore() {
        return blockEntity.getCore();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == 0) {
            // core slot -> player inventory
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // player inventory -> core slot (cores only)
            if (stack.getItem() instanceof IWandCore) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (com.github.nalamodikk.space.ship.ShipShadowManager.isShadowBE(blockEntity)) return true; // 飛船影子機器：跨維度放行
        return blockEntity.getLevel() != null
                && Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(player.position()) < 64.0;
    }
}
