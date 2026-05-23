package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.template.ResearchRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Sent client → server when the player completes a research puzzle.
 * Server validates, saves to {@link ResearchSavedData}, clears the
 * Research Table slots, and notifies the player.
 */
public record ResearchCompletePacket(ResourceLocation researchId, BlockPos tablePos)
        implements CustomPacketPayload {

    public static final Type<ResearchCompletePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "research_complete"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ResearchCompletePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    ResearchCompletePacket::researchId,
                    BlockPos.STREAM_CODEC,
                    ResearchCompletePacket::tablePos,
                    ResearchCompletePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendToServer(ResourceLocation researchId, BlockPos tablePos) {
        PacketDistributor.sendToServer(new ResearchCompletePacket(researchId, tablePos));
    }

    public static void handle(ResearchCompletePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.researchId() == null) return;

            if (net.minecraft.world.phys.Vec3.atCenterOf(packet.tablePos())
                    .distanceToSqr(player.position()) > 64.0) return;

            var template = ResearchRegistry.get(packet.researchId());
            if (template.isEmpty()) {
                KoniavacraftMod.LOGGER.warn("ResearchCompletePacket: unknown research {}",
                        packet.researchId());
                return;
            }

            ServerLevel level = player.serverLevel();

            var knowledge = ResearchSavedData.get(level).getOrCreate(player.getUUID());
            if (knowledge.hasCompleted(packet.researchId())) return;

            if (!template.get().isAvailableTo(knowledge)) return;

            KoniavacraftMod.LOGGER.info("[Research] {} solved puzzle: {}",
                    player.getName().getString(), packet.researchId());

            // Transform note → completed scroll, consume quill charge.
            // Knowledge is saved later when the player right-clicks the scroll.
            if (level.getBlockEntity(packet.tablePos()) instanceof ResearchTableBlockEntity table) {
                if (!table.isNoteOwner(player.getUUID())) {
                    player.sendSystemMessage(Component.translatable("message.koniava.research.table_in_use"));
                    return;
                }
                table.onResearchComplete(player);
            }

            // Puzzle-solved hint (not the full completion message — that fires on scroll use)
            player.sendSystemMessage(Component.translatable("research.koniava.puzzle_solved")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ResearchCompletePacket::handle);
    }
}
