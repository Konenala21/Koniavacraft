package com.github.nalamodikk.common.network.packet.server.player.gui;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.screen.player.ExtraEquipmentMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record OpenExtraEquipmentPacket() implements CustomPacketPayload {

    public static final Type<OpenExtraEquipmentPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "open_extra_equipment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenExtraEquipmentPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenExtraEquipmentPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenExtraEquipmentPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new ExtraEquipmentMenu(id, inv),
                        Component.translatable("screen.koniava.inventory.extra_equipment")
                ));
            }
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, OpenExtraEquipmentPacket::handle);
    }


    public static void sendToServer() {
        PacketDistributor.sendToServer(new OpenExtraEquipmentPacket());
    }
}

