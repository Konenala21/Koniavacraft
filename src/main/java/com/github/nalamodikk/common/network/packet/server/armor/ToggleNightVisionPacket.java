package com.github.nalamodikk.common.network.packet.server.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyHelmetItem;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.register.ModDataComponents;
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

public record ToggleNightVisionPacket() implements CustomPacketPayload {

    public static final Type<ToggleNightVisionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "toggle_night_vision"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleNightVisionPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleNightVisionPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ToggleNightVisionPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!(helmet.getItem() instanceof ManaAlloyHelmetItem)) return;
            boolean hasUpgrade = false;
            for (ItemStack upg : ManaArmorItem.getData(helmet).upgrades().values()) {
                if (upg.getItem() instanceof HelmetUpgradeItem hu
                        && hu.getBehavior() == HelmetUpgradeBehavior.NIGHT_VISION) {
                    hasUpgrade = true;
                    break;
                }
            }
            if (!hasUpgrade) return;
            boolean current = Boolean.TRUE.equals(helmet.get(ModDataComponents.NIGHT_VISION_ACTIVE));
            helmet.set(ModDataComponents.NIGHT_VISION_ACTIVE, !current);
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ToggleNightVisionPacket::handle);
    }

    public static void send() {
        PacketDistributor.sendToServer(new ToggleNightVisionPacket());
    }
}
