package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.NaraWatchItem;
import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.util.NaraHelper;
import com.github.nalamodikk.register.ModItems;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.template.ResearchRegistry;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
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

            if (packet.bind() && NaraHelper.isBound(serverPlayer)) return;

            NaraHelper.setBound(serverPlayer, packet.bind());
            PacketDistributor.sendToPlayer(serverPlayer, new NaraSyncPacket(packet.bind()));
            LOGGER.debug("Updated Nara bind state: {}", packet.bind());

            if (packet.bind()) {
                giveWatchIfMissing(serverPlayer);
                // 成就
                NaraServerEvents.grantWelcomeAdvancement(serverPlayer);
                // 煙火三發
                spawnWelcomeFireworks(serverPlayer);
                // 固有研究：研究基礎（解鎖研究台合成）
                grantInnateResearch(serverPlayer);
                // 皮膚 model 由 client 端自己讀
                PacketDistributor.sendToPlayer(serverPlayer, new NaraStartDialoguePacket());
            }
        });
    }

    public static void spawnWelcomeFireworksPublic(ServerPlayer player) { spawnWelcomeFireworks(player); }

    private static void spawnWelcomeFireworks(ServerPlayer player) {
        var level = player.serverLevel();
        // 青色 + 粉紅爆炸
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.BURST,
                IntList.of(0x00FFCC, 0xFF69B4),
                IntList.of(),
                false, false
        );
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        fireworkStack.set(DataComponents.FIREWORKS, new Fireworks(1, java.util.List.of(explosion)));

        for (int i = 0; i < 3; i++) {
            double ox = (i - 1) * 2.0;
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    level, player.getX() + ox, player.getY() + 2, player.getZ(),
                    fireworkStack.copy());
            level.addFreshEntity(rocket);
        }
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

    private static void grantInnateResearch(ServerPlayer player) {
        var researchId = ResearchRegistry.RESEARCH_BASICS.getId();
        var savedData = ResearchSavedData.get(player.serverLevel());
        boolean isNew = savedData.completeResearch(player.getUUID(), researchId);
        if (isNew) {
            ResearchRegistry.get(researchId).ifPresent(t -> {
                var toAward = player.serverLevel().getRecipeManager().getRecipes()
                        .stream().filter(h -> t.getUnlockedRecipes().contains(h.id())).toList();
                if (!toAward.isEmpty()) player.awardRecipes(toAward);
            });
            savedData.setDirty();
            KnowledgeSyncPacket.sendTo(player);
        }
    }

    public static void send(boolean bind) {
        PacketDistributor.sendToServer(new NaraBindRequestPacket(bind));
    }
}
