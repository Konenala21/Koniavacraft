package com.github.nalamodikk.common.network.packet.server.turret;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * C2S: install or remove a turret upgrade from a held Floating Turret.
 *
 * inventorySlot = -1: remove from turret slot back to inventory
 * inventorySlot >= 0: install from this player inventory slot into turret slot
 */
public record TurretUpgradeSwapPacket(InteractionHand hand, int slot, int inventorySlot)
        implements CustomPacketPayload {

    public static final Type<TurretUpgradeSwapPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "turret_upgrade_swap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TurretUpgradeSwapPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL.map(
                            b -> b ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                            h -> h == InteractionHand.MAIN_HAND),
                    TurretUpgradeSwapPacket::hand,
                    ByteBufCodecs.VAR_INT, TurretUpgradeSwapPacket::slot,
                    ByteBufCodecs.VAR_INT, TurretUpgradeSwapPacket::inventorySlot,
                    TurretUpgradeSwapPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TurretUpgradeSwapPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            ItemStack turret = player.getItemInHand(packet.hand());
            if (!(turret.getItem() instanceof FloatingTurretItem item)) return;

            EquipmentUpgradeData data = FloatingTurretItem.getData(turret);

            if (packet.inventorySlot() == -1) {
                removeFrom(player, turret, data, packet.slot());
            } else {
                installTo(player, item, turret, data, packet.slot(), packet.inventorySlot());
            }
        });
    }

    private static void removeFrom(ServerPlayer player, ItemStack turret, EquipmentUpgradeData data, int slot) {
        ItemStack current = data.getUpgrade(slot);
        if (current.isEmpty()) return;
        ItemStack toReturn = current.copy();
        EquipmentUpgradeData updated = data.withUpgrade(slot, ItemStack.EMPTY);
        if (!player.addItem(toReturn)) player.drop(toReturn, false);
        FloatingTurretItem.setData(turret, updated);
        FloatingTurretItem.recalculateMaxMana(turret);
    }

    private static void installTo(ServerPlayer player, FloatingTurretItem item, ItemStack turret,
                                  EquipmentUpgradeData data, int turretSlot, int invSlot) {
        if (turretSlot < 0 || turretSlot >= item.getMaxUpgradeSlots()) return;
        if (invSlot < 0 || invSlot >= player.getInventory().getContainerSize()) return;
        ItemStack fromInv = player.getInventory().getItem(invSlot);
        if (fromInv.isEmpty() || !(fromInv.getItem() instanceof TurretUpgradeItem incoming)) return;

        // 拒絕重複的同類升級（自動瞄準/激光那類互斥之後在此擴充）
        for (var entry : data.upgrades().entrySet()) {
            if (entry.getKey() == turretSlot) continue;
            if (entry.getValue().getItem() instanceof TurretUpgradeItem existing
                    && existing.getBehavior() == incoming.getBehavior()) {
                return;
            }
        }

        ItemStack current = data.getUpgrade(turretSlot);
        if (!current.isEmpty() && !player.addItem(current.copy())) {
            player.drop(current.copy(), false);
        }
        EquipmentUpgradeData updated = data.withUpgrade(turretSlot, fromInv.copyWithCount(1));
        fromInv.shrink(1);
        FloatingTurretItem.setData(turret, updated);
        FloatingTurretItem.recalculateMaxMana(turret);
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, TurretUpgradeSwapPacket::handle);
    }

    public static void sendInstall(InteractionHand hand, int slot, int inventorySlot) {
        PacketDistributor.sendToServer(new TurretUpgradeSwapPacket(hand, slot, inventorySlot));
    }

    public static void sendRemove(InteractionHand hand, int slot) {
        PacketDistributor.sendToServer(new TurretUpgradeSwapPacket(hand, slot, -1));
    }
}
