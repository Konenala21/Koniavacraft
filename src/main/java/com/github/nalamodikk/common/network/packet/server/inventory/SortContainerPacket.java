package com.github.nalamodikk.common.network.packet.server.inventory;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.inventory.sort.InventorySorter;
import com.github.nalamodikk.common.inventory.sort.SortMode;
import com.github.nalamodikk.common.inventory.sort.SortTarget;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.stream.Collectors;

public record SortContainerPacket(SortMode mode, SortTarget target) implements CustomPacketPayload {

    public static final Type<SortContainerPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "sort_container"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SortContainerPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeEnum(pkt.mode());
                buf.writeEnum(pkt.target());
            },
            buf -> new SortContainerPacket(
                buf.readEnum(SortMode.class),
                buf.readEnum(SortTarget.class)
            )
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SortContainerPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            AbstractContainerMenu menu = player.containerMenu;
            if (menu instanceof InventoryMenu) return;

            Inventory playerInv = player.getInventory();
            List<Slot> targetSlots;

            if (packet.target() == SortTarget.CONTAINER) {
                targetSlots = menu.slots.stream()
                    .filter(slot -> slot.container != playerInv)
                    .collect(Collectors.toList());
            } else {
                targetSlots = menu.slots.stream()
                    .filter(slot -> slot.container == playerInv && slot.getContainerSlot() >= 9)
                    .collect(Collectors.toList());
            }

            if (targetSlots.isEmpty()) return;
            InventorySorter.sort(targetSlots, packet.mode());
            menu.broadcastChanges();
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, SortContainerPacket::handle);
    }

    public static void sendToServer(SortMode mode, SortTarget target) {
        PacketDistributor.sendToServer(new SortContainerPacket(mode, target));
    }
}
