package com.github.nalamodikk.common.network.packet.server.wand;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.common.item.wand.upgrade.IWandUpgrade;
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

import java.util.ArrayList;
import java.util.List;

/**
 * C2S: install or remove a core/upgrade from the wand.
 *
 * slot = -1: core slot
 * slot = 0-3: upgrade slots
 * inventorySlot = -1: remove from wand slot back to inventory
 * inventorySlot >= 0: install from this player inventory slot into wand slot
 */
public record WandCoreSwapPacket(InteractionHand hand, int slot, int inventorySlot)
        implements CustomPacketPayload {

    public static final Type<WandCoreSwapPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "wand_core_swap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WandCoreSwapPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL.map(
                            b -> b ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                            h -> h == InteractionHand.MAIN_HAND),
                    WandCoreSwapPacket::hand,
                    ByteBufCodecs.VAR_INT, WandCoreSwapPacket::slot,
                    ByteBufCodecs.VAR_INT, WandCoreSwapPacket::inventorySlot,
                    WandCoreSwapPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(WandCoreSwapPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            ItemStack wand = player.getItemInHand(packet.hand());
            if (!(wand.getItem() instanceof WandRodItem)) return;

            WandCoreData data = WandRodItem.getData(wand);

            if (packet.inventorySlot() == -1) {
                // Remove from wand slot → give to player
                removeFromWand(player, wand, data, packet.slot());
            } else {
                // Install from inventory slot → wand slot
                installToWand(player, wand, data, packet.slot(), packet.inventorySlot());
            }
        });
    }

    private static void removeFromWand(ServerPlayer player, ItemStack wand, WandCoreData data, int slot) {
        ItemStack toReturn;
        WandCoreData updated;

        if (slot == -1) {
            toReturn = data.core().copy();
            updated = data.withCore(ItemStack.EMPTY);
        } else {
            ItemStack current = data.getUpgrade(slot);
            if (current.isEmpty()) return;
            toReturn = current.copy();
            updated = data.withUpgrade(slot, ItemStack.EMPTY);
        }

        if (!toReturn.isEmpty()) {
            if (!player.addItem(toReturn)) {
                player.drop(toReturn, false);
            }
            WandRodItem.setData(wand, updated);
        }
    }

    private static void installToWand(ServerPlayer player, ItemStack wand, WandCoreData data,
                                       int wandSlot, int invSlot) {
        if (invSlot < 0 || invSlot >= player.getInventory().getContainerSize()) return;
        ItemStack fromInv = player.getInventory().getItem(invSlot);
        if (fromInv.isEmpty()) return;

        if (wandSlot == -1) {
            if (!(fromInv.getItem() instanceof IWandCore)) return;
            // Swap: give current core back if any
            ItemStack current = data.core();
            if (!current.isEmpty() && !player.addItem(current.copy())) {
                player.drop(current.copy(), false);
            }
            WandCoreData updated = data.withCore(fromInv.copyWithCount(1));
            fromInv.shrink(1);
            WandRodItem.setData(wand, updated);
        } else {
            if (!(fromInv.getItem() instanceof IWandUpgrade)) return;
            ItemStack current = data.getUpgrade(wandSlot);
            if (!current.isEmpty() && !player.addItem(current.copy())) {
                player.drop(current.copy(), false);
            }
            WandCoreData updated = data.withUpgrade(wandSlot, fromInv.copyWithCount(1));
            fromInv.shrink(1);
            WandRodItem.setData(wand, updated);
        }
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, WandCoreSwapPacket::handle);
    }

    public static void sendInstall(InteractionHand hand, int slot, int inventorySlot) {
        PacketDistributor.sendToServer(new WandCoreSwapPacket(hand, slot, inventorySlot));
    }

    public static void sendRemove(InteractionHand hand, int slot) {
        PacketDistributor.sendToServer(new WandCoreSwapPacket(hand, slot, -1));
    }
}
