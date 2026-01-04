package com.github.nalamodikk.common.network.packet.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.screen.block.shared.UpgradeMenu;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

public record OpenUpgradeGuiPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<OpenUpgradeGuiPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "open_upgrade_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenUpgradeGuiPacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsLibrary.BLOCK_POS,
                    OpenUpgradeGuiPacket::pos,
                    OpenUpgradeGuiPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, OpenUpgradeGuiPacket::handle);
    }

    public static void handle(OpenUpgradeGuiPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();

            if (!level.isLoaded(packet.pos())) return;

            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof IUpgradeableMachine machine) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new UpgradeMenu(id, inv, machine.getUpgradeInventory(), machine),
                        Component.translatable("screen.koniava.upgrade.title")

                ), buf -> buf.writeBlockPos(packet.pos()));
            }
            LOGGER.info("[OpenUpgradeGuiPacket] Opening upgrade GUI at {}", packet.pos());

        });
    }

    public static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new OpenUpgradeGuiPacket(pos));
    }
}
