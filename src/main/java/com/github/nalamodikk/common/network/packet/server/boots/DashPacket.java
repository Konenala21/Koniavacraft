package com.github.nalamodikk.common.network.packet.server.boots;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record DashPacket() implements CustomPacketPayload {

    public static final Type<DashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "dash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DashPacket> STREAM_CODEC =
            StreamCodec.unit(new DashPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DashPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (!(boots.getItem() instanceof ManaSprintBootsItem)) return;
            ManaSprintBootsItem.performDash(player, boots);
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, DashPacket::handle);
    }

    public static void send() {
        PacketDistributor.sendToServer(new DashPacket());
    }
}
