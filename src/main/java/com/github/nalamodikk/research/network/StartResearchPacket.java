package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.research.ResearchNoteItem;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.template.ResearchRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client → server: player clicked an available research node in {@link com.github.nalamodikk.research.client.NaraWatchScreen}.
 * Server validates eligibility and hands the player a {@link ResearchNoteItem}.
 */
public record StartResearchPacket(ResourceLocation researchId)
        implements CustomPacketPayload {

    public static final Type<StartResearchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "start_research"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, StartResearchPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, StartResearchPacket::researchId,
                    StartResearchPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendToServer(ResourceLocation researchId) {
        PacketDistributor.sendToServer(new StartResearchPacket(researchId));
    }

    public static void handle(StartResearchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            var template = ResearchRegistry.get(packet.researchId()).orElse(null);
            if (template == null) return;

            var data      = ResearchSavedData.get(player.serverLevel());
            var knowledge = data.getOrCreate(player.getUUID());

            // Already completed — don't give another note
            if (knowledge.hasCompleted(packet.researchId())) {
                player.sendSystemMessage(Component.translatable(
                        "research.koniava.already_completed",
                        Component.translatable(template.getTitleKey())));
                return;
            }

            // Already have the note in inventory — don't give a duplicate
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (packet.researchId().equals(ResearchNoteItem.getResearchId(inv.getItem(i)))) {
                    player.sendSystemMessage(Component.translatable(
                            "research.koniava.already_have_note",
                            Component.translatable(template.getTitleKey())));
                    return;
                }
            }

            // Check prerequisites and tier (aspect requirement skipped for note pickup)
            if (knowledge.getCurrentTier() < template.getTier()) return;
            for (ResourceLocation prereq : template.getPrerequisites()) {
                if (!knowledge.hasCompleted(prereq)) return;
            }

            // Give research note
            ItemStack note = ResearchNoteItem.forResearch(packet.researchId());
            if (!player.getInventory().add(note)) {
                player.drop(note, false);
            }
            player.sendSystemMessage(Component.translatable(
                    "research.koniava.note_received",
                    Component.translatable(template.getTitleKey())));
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, StartResearchPacket::handle);
    }
}
