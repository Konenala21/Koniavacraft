package com.github.nalamodikk.common.network.packet.server.boots;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * C2S: install or remove an upgrade from boots.
 *
 * slot = 0-4: upgrade slot index
 * inventorySlot = -1: remove from boots slot back to inventory
 * inventorySlot >= 0: install from this inventory slot into boots slot
 */
public record BootsUpgradeSwapPacket(int slot, int inventorySlot) implements CustomPacketPayload {

    public static final Type<BootsUpgradeSwapPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "boots_upgrade_swap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BootsUpgradeSwapPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BootsUpgradeSwapPacket::slot,
                    ByteBufCodecs.VAR_INT, BootsUpgradeSwapPacket::inventorySlot,
                    BootsUpgradeSwapPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BootsUpgradeSwapPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (!(boots.getItem() instanceof ManaSprintBootsItem bootsItem)) return;

            EquipmentUpgradeData data = ManaArmorItem.getData(boots);

            if (packet.inventorySlot() == -1) {
                removeFromBoots(player, boots, data, packet.slot(), bootsItem);
            } else {
                installToBoots(player, boots, data, packet.slot(), packet.inventorySlot(), bootsItem);
            }
        });
    }

    private static void removeFromBoots(ServerPlayer player, ItemStack boots, EquipmentUpgradeData data,
                                         int slot, ManaSprintBootsItem bootsItem) {
        ItemStack current = data.getUpgrade(slot);
        if (current.isEmpty()) return;
        ItemStack toReturn = current.copy();
        if (!player.addItem(toReturn)) player.drop(toReturn, false);
        ManaArmorItem.setData(boots, data.withUpgrade(slot, ItemStack.EMPTY));
        bootsItem.recalculateMaxMana(boots);
    }

    private static void installToBoots(ServerPlayer player, ItemStack boots, EquipmentUpgradeData data,
                                        int slot, int invSlot, ManaSprintBootsItem bootsItem) {
        if (invSlot < 0 || invSlot >= player.getInventory().getContainerSize()) return;
        ItemStack fromInv = player.getInventory().getItem(invSlot);
        if (fromInv.isEmpty()) return;
        if (!bootsItem.isValidUpgradeItem(fromInv)) return;
        if (slot < 0 || slot >= bootsItem.getMaxUpgradeSlots()) return;

        String newKey = bootsItem.getUpgradeBehaviorKey(fromInv);
        for (int i = 0; i < bootsItem.getMaxUpgradeSlots(); i++) {
            if (i == slot) continue;
            ItemStack existing = data.getUpgrade(i);
            if (!existing.isEmpty() && bootsItem.getUpgradeBehaviorKey(existing).equals(newKey)) return;
        }

        ItemStack current = data.getUpgrade(slot);
        if (!current.isEmpty() && !player.addItem(current.copy())) {
            player.drop(current.copy(), false);
        }
        ManaArmorItem.setData(boots, data.withUpgrade(slot, fromInv.copyWithCount(1)));
        fromInv.shrink(1);
        bootsItem.recalculateMaxMana(boots);
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, BootsUpgradeSwapPacket::handle);
    }

    public static void sendInstall(int slot, int inventorySlot) {
        PacketDistributor.sendToServer(new BootsUpgradeSwapPacket(slot, inventorySlot));
    }

    public static void sendRemove(int slot) {
        PacketDistributor.sendToServer(new BootsUpgradeSwapPacket(slot, -1));
    }
}
