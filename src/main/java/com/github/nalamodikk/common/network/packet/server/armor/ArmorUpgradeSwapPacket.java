package com.github.nalamodikk.common.network.packet.server.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeItem;
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
 * C2S: install or remove an upgrade from a ManaArmorItem (helmet / chestplate / leggings).
 * Boots use BootsUpgradeSwapPacket.
 *
 * equipmentSlot: HEAD / CHEST / LEGS encoded as ordinal byte
 * slot: upgrade slot index
 * inventorySlot: -1 = remove, >= 0 = install from this inventory slot
 */
public record ArmorUpgradeSwapPacket(EquipmentSlot equipmentSlot, int slot, int inventorySlot)
        implements CustomPacketPayload {

    public static final Type<ArmorUpgradeSwapPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "armor_upgrade_swap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorUpgradeSwapPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE.map(
                            b -> EquipmentSlot.values()[b],
                            s -> (byte) s.ordinal()),
                    ArmorUpgradeSwapPacket::equipmentSlot,
                    ByteBufCodecs.VAR_INT, ArmorUpgradeSwapPacket::slot,
                    ByteBufCodecs.VAR_INT, ArmorUpgradeSwapPacket::inventorySlot,
                    ArmorUpgradeSwapPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ArmorUpgradeSwapPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack armor = player.getItemBySlot(packet.equipmentSlot());
            if (!(armor.getItem() instanceof ManaArmorItem armorItem)) return;

            EquipmentUpgradeData data = ManaArmorItem.getData(armor);

            if (packet.inventorySlot() == -1) {
                removeFromArmor(player, armor, armorItem, data, packet.slot());
            } else {
                installToArmor(player, armor, armorItem, data, packet.slot(), packet.inventorySlot());
            }
        });
    }

    private static void removeFromArmor(ServerPlayer player, ItemStack armor,
                                         ManaArmorItem armorItem, EquipmentUpgradeData data, int slot) {
        ItemStack current = data.getUpgrade(slot);
        if (current.isEmpty()) return;
        ItemStack toReturn = current.copy();
        if (!player.addItem(toReturn)) player.drop(toReturn, false);
        ManaArmorItem.setData(armor, data.withUpgrade(slot, ItemStack.EMPTY));
        armorItem.recalculateMaxMana(armor);
    }

    private static void installToArmor(ServerPlayer player, ItemStack armor,
                                        ManaArmorItem armorItem, EquipmentUpgradeData data,
                                        int slot, int invSlot) {
        if (invSlot < 0 || invSlot >= player.getInventory().getContainerSize()) return;
        ItemStack fromInv = player.getInventory().getItem(invSlot);
        if (fromInv.isEmpty()) return;
        if (!armorItem.isValidUpgradeItem(fromInv)) return;
        if (slot < 0 || slot >= armorItem.getMaxUpgradeSlots()) return;

        // One-per-type enforcement
        String newKey = armorItem.getUpgradeBehaviorKey(fromInv);
        for (int i = 0; i < armorItem.getMaxUpgradeSlots(); i++) {
            if (i == slot) continue;
            ItemStack existing = data.getUpgrade(i);
            if (!existing.isEmpty() && armorItem.getUpgradeBehaviorKey(existing).equals(newKey)) return;
        }

        // 護盾兩型互斥：已裝一種護盾就不能再裝另一種
        if (fromInv.getItem() instanceof ChestplateUpgradeItem incoming && incoming.getBehavior().isShield()) {
            for (int i = 0; i < armorItem.getMaxUpgradeSlots(); i++) {
                if (i == slot) continue;
                ItemStack existing = data.getUpgrade(i);
                if (existing.getItem() instanceof ChestplateUpgradeItem cu
                        && cu.getBehavior().isShield()
                        && cu.getBehavior() != incoming.getBehavior()) {
                    return;
                }
            }
        }

        ItemStack current = data.getUpgrade(slot);
        if (!current.isEmpty() && !player.addItem(current.copy())) {
            player.drop(current.copy(), false);
        }
        ManaArmorItem.setData(armor, data.withUpgrade(slot, fromInv.copyWithCount(1)));
        fromInv.shrink(1);
        armorItem.recalculateMaxMana(armor);
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ArmorUpgradeSwapPacket::handle);
    }

    public static void sendInstall(EquipmentSlot slot, int upgradeSlot, int inventorySlot) {
        PacketDistributor.sendToServer(new ArmorUpgradeSwapPacket(slot, upgradeSlot, inventorySlot));
    }

    public static void sendRemove(EquipmentSlot slot, int upgradeSlot) {
        PacketDistributor.sendToServer(new ArmorUpgradeSwapPacket(slot, upgradeSlot, -1));
    }
}
