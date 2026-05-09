package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.research.aspect.Aspect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Sent client → server each time the player places or removes an aspect on the research grid.
 *
 * On place (aspectId present): saves cell to BlockEntity + damages ink quill 3-5 durability.
 * On remove (aspectId empty):  saves removal to BlockEntity, no quill damage.
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

            var be = player.serverLevel().getBlockEntity(packet.tablePos());
            if (!(be instanceof ResearchTableBlockEntity table)) return;

            // Persist cell state
            table.saveCellPlacement(
                    packet.researchId(), packet.q(), packet.r(),
                    packet.aspectId().orElse(null)
            );

            // Damage quill only when placing, not when removing
            if (packet.aspectId().isPresent()) {
                ItemStack quill = table.getInventory().getStackInSlot(ResearchTableBlockEntity.QUILL_SLOT);
                if (!quill.isEmpty()) {
                    int damage = 3 + RandomGenerator.getDefault().nextInt(3);
                    quill.hurtAndBreak(damage, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                    if (quill.isEmpty()) {
                        table.getInventory().setStackInSlot(ResearchTableBlockEntity.QUILL_SLOT, ItemStack.EMPTY);
                        KoniavacraftMod.LOGGER.debug("Ink quill broke at {}", packet.tablePos());
                    }
                    table.setChanged();
                    // Sync quill damage to client immediately after hurting
                    var level = player.serverLevel();
                    level.sendBlockUpdated(packet.tablePos(),
                            level.getBlockState(packet.tablePos()),
                            level.getBlockState(packet.tablePos()), 3);
                }
            }
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ResearchAspectPlacePacket::handle);
    }
}
