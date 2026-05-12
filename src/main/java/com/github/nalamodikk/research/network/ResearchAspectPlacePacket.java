package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableMenu;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Sent client → server each time the player places or removes an aspect on the research grid.
 *
 * On place or remove: saves cell to BlockEntity + damages ink quill 1-5 durability.
 */
public record ResearchAspectPlacePacket(
        BlockPos tablePos,
        ResourceLocation researchId,
        int q,
        int r,
        Optional<ResourceLocation> aspectId)
        implements CustomPacketPayload {

    public static final Type<ResearchAspectPlacePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "research_aspect_place"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ResearchAspectPlacePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,                                        ResearchAspectPlacePacket::tablePos,
                    ResourceLocation.STREAM_CODEC,                                ResearchAspectPlacePacket::researchId,
                    ByteBufCodecs.VAR_INT,                                        ResearchAspectPlacePacket::q,
                    ByteBufCodecs.VAR_INT,                                        ResearchAspectPlacePacket::r,
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),        ResearchAspectPlacePacket::aspectId,
                    ResearchAspectPlacePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendToServer(BlockPos tablePos, ResourceLocation researchId,
                                    int q, int r, @Nullable Aspect aspect) {
        PacketDistributor.sendToServer(new ResearchAspectPlacePacket(
                tablePos, researchId, q, r,
                Optional.ofNullable(aspect == null ? null : aspect.getId())
        ));
    }

    public static void handle(ResearchAspectPlacePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            BlockEntity be = player.serverLevel().getBlockEntity(packet.tablePos());
            if (!(be instanceof ResearchTableBlockEntity table)) return;

            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            String key = packet.q() + "," + packet.r();
            String previousAspectId = table.getSavedPlacements().get(key);
            ResourceLocation newAspectId = packet.aspectId().orElse(null);

            // Persist cell state
            if (newAspectId != null) {
                Aspect aspect = com.github.nalamodikk.research.aspect.ModAspects.get(newAspectId);
                if (aspect == null) {
                    player.sendSystemMessage(Component.translatable("message.koniava.synthesis.invalid_aspect"));
                    return;
                }
                if (knowledge.getAspectCount(newAspectId) <= 0) {
                    player.sendSystemMessage(Component.translatable("message.koniava.research.not_enough_aspect",
                            aspect.getName()));
                    return;
                }
            }

            if (previousAspectId != null) {
                refundAspect(knowledge, ResourceLocation.parse(previousAspectId));
            }

            if (newAspectId != null) {
                consumeAspect(knowledge, newAspectId);
            }

            table.saveCellPlacement(packet.researchId(), packet.q(), packet.r(), newAspectId);
            ResearchSavedData.get(player.serverLevel()).setDirty();
            AspectSyncPacket.sendTo(player);

            // Every click on the hex puzzle consumes quill durability, including cancel/removal.
            ItemStack quill = table.getInventory().getStackInSlot(ResearchTableBlockEntity.QUILL_SLOT);
            if (!quill.isEmpty()) {
                int damage = 1 + RandomGenerator.getDefault().nextInt(5);
                int nextDamage = quill.getDamageValue() + damage;
                if (nextDamage >= quill.getMaxDamage()) {
                    KoniavacraftMod.LOGGER.debug("Ink quill broke at {}", packet.tablePos());
                    table.getInventory().setStackInSlot(ResearchTableBlockEntity.QUILL_SLOT, ItemStack.EMPTY);
                } else {
                    ItemStack updated = quill.copy();
                    updated.setDamageValue(nextDamage);
                    table.getInventory().setStackInSlot(ResearchTableBlockEntity.QUILL_SLOT, updated);
                }
                table.setChanged();
                // Sync quill damage to client immediately after hurting
                player.serverLevel().sendBlockUpdated(packet.tablePos(),
                        player.serverLevel().getBlockState(packet.tablePos()),
                        player.serverLevel().getBlockState(packet.tablePos()), 3);
                if (player.containerMenu instanceof ResearchTableMenu menu && menu.getBlockEntity() == table) {
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }

    private static void consumeAspect(PlayerKnowledge knowledge, ResourceLocation aspectId) {
        int current = knowledge.getAspectCount(aspectId);
        if (current > 0) {
            knowledge.setAspectAmount(aspectId, current - 1);
        }
    }

    private static void refundAspect(PlayerKnowledge knowledge, ResourceLocation aspectId) {
        int current = knowledge.getAspectCount(aspectId);
        knowledge.setAspectAmount(aspectId, current + 1);
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ResearchAspectPlacePacket::handle);
    }
}
