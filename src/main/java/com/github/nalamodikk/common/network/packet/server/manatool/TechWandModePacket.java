package com.github.nalamodikk.common.network.packet.server.manatool;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record TechWandModePacket(BasicTechWandItem.TechWandMode mode) implements CustomPacketPayload {

    public static final Type<TechWandModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "tech_wand_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TechWandModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(255)
                            .mapStream(buf -> buf)
                            .map(s -> Enum.valueOf(BasicTechWandItem.TechWandMode.class, s), BasicTechWandItem.TechWandMode::name),
                    TechWandModePacket::mode,
                    TechWandModePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TechWandModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();

                if (stack.getItem() instanceof BasicTechWandItem wand) {
                    wand.setMode(stack, packet.mode());

                    player.displayClientMessage(Component.translatable(
                            "message.koniava.mode_changed",
                            Component.translatable("mode.koniava." + packet.mode().getSerializedName())
                    ), true);
                }
            }
        });
    }



    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, TechWandModePacket::handle);
    }

    public static void sendToServer(BasicTechWandItem.TechWandMode mode) {
        PacketDistributor.sendToServer(new TechWandModePacket(mode));
    }
}
