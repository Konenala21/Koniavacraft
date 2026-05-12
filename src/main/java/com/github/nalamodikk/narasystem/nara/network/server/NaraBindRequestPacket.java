package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.NaraWatchItem;
import com.github.nalamodikk.narasystem.nara.util.NaraHelper;
import com.github.nalamodikk.register.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

public record NaraBindRequestPacket(boolean bind) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<NaraBindRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_bind_request"));

    public static final StreamCodec<FriendlyByteBuf, NaraBindRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, NaraBindRequestPacket::bind,
                    NaraBindRequestPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, NaraBindRequestPacket::handle);
    }

    public static void handle(NaraBindRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player sender = context.player();
            if (!(sender instanceof ServerPlayer serverPlayer)) {
                return;
            }

            NaraHelper.setBound(serverPlayer, packet.bind());
            PacketDistributor.sendToPlayer(serverPlayer, new NaraSyncPacket(packet.bind()));
            LOGGER.debug("Updated Nara bind state: {}", packet.bind());

            if (packet.bind()) {
                giveWatchIfMissing(serverPlayer);
            }
        });
    }

    private static void giveWatchIfMissing(ServerPlayer serverPlayer) {
        Inventory inventory = serverPlayer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof NaraWatchItem) {
                return;
            }
        }

        ItemStack watch = new ItemStack(ModItems.NARA_WATCH.get());
        if (!inventory.add(watch)) {
            serverPlayer.drop(watch, false);
        }
    }

    public static void send(boolean bind) {
        PacketDistributor.sendToServer(new NaraBindRequestPacket(bind));
    }
}
