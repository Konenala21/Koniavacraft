package com.github.nalamodikk.research.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableMenu;
import com.github.nalamodikk.common.item.research.InkQuillItem;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

import java.util.List;
import java.util.random.RandomGenerator;

public record AspectSynthesisPacket(BlockPos tablePos, ResourceLocation aspect1, ResourceLocation aspect2) implements CustomPacketPayload {

    public static final Type<AspectSynthesisPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "aspect_synthesis"));

    public static final StreamCodec<FriendlyByteBuf, AspectSynthesisPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.tablePos);
                buf.writeResourceLocation(packet.aspect1);
                buf.writeResourceLocation(packet.aspect2);
            },
            buf -> new AspectSynthesisPacket(buf.readBlockPos(), buf.readResourceLocation(), buf.readResourceLocation())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToServer(BlockPos tablePos, ResourceLocation first, ResourceLocation second) {
        PacketDistributor.sendToServer(new AspectSynthesisPacket(tablePos, first, second));
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.blockPosition().closerThan(tablePos, 8.0)) {
                return;
            }

            Aspect first = ModAspects.get(aspect1);
            Aspect second = ModAspects.get(aspect2);
            if (first == null || second == null) {
                player.sendSystemMessage(Component.translatable("message.koniava.synthesis.invalid_aspect"));
                return;
            }

            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            if (!canUse(knowledge, first) || !canUse(knowledge, second)) {
                player.sendSystemMessage(Component.translatable("message.koniava.synthesis.insufficient_knowledge"));
                return;
            }

            Aspect result = findResult(first, second);
            if (result == null) {
                player.sendSystemMessage(Component.translatable("message.koniava.synthesis.no_recipe"));
                return;
            }

            BlockEntity blockEntity = player.serverLevel().getBlockEntity(tablePos);
            if (!(blockEntity instanceof ResearchTableBlockEntity table)) {
                player.sendSystemMessage(Component.translatable("message.koniava.synthesis.invalid_table"));
                return;
            }
            if (!damageQuill(table, player)) {
                player.sendSystemMessage(Component.translatable("message.koniava.synthesis.missing_quill"));
                return;
            }

            consumeAspect(knowledge, first);
            consumeAspect(knowledge, second);
            boolean newlyDiscovered = knowledge.discoverAspect(result, 1);
            ResearchSavedData.get(player.serverLevel()).setDirty();
            AspectSyncPacket.sendTo(player);
            player.sendSystemMessage(Component.translatable("message.koniava.synthesis.success", result.getName()));
            if (newlyDiscovered) {
                player.sendSystemMessage(Component.translatable("research.koniava.aspect.discovered", result.getName()));
            }
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, AspectSynthesisPacket::handle);
    }

    private static boolean canUse(PlayerKnowledge knowledge, Aspect aspect) {
        return knowledge.getAspectCount(aspect.getId()) > 0;
    }

    private static void consumeAspect(PlayerKnowledge knowledge, Aspect aspect) {
        int current = knowledge.getAspectCount(aspect.getId());
        if (current > 0) {
            knowledge.setAspectAmount(aspect.getId(), current - 1);
        }
    }

    private static boolean damageQuill(ResearchTableBlockEntity table, ServerPlayer player) {
        ItemStack quill = table.getInventory().getStackInSlot(ResearchTableBlockEntity.QUILL_SLOT);
        if (quill.isEmpty() || !(quill.getItem() instanceof InkQuillItem)) {
            return false;
        }
        if (InkQuillItem.isOutOfInk(quill)) {
            player.sendSystemMessage(Component.translatable("item.koniava.ink_quill.out_of_ink_prompt"));
            return false;
        }

        int damage = 1 + RandomGenerator.getDefault().nextInt(5);
        ItemStack updated = quill.copy();
        boolean justLocked = InkQuillItem.applyDamage(updated, damage);
        table.getInventory().setStackInSlot(ResearchTableBlockEntity.QUILL_SLOT, updated);
        if (justLocked) {
            player.sendSystemMessage(Component.translatable("item.koniava.ink_quill.just_locked"));
        }
        table.setChanged();
        if (table.getLevel() != null && !table.getLevel().isClientSide()) {
            table.getLevel().sendBlockUpdated(table.getBlockPos(), table.getBlockState(), table.getBlockState(), 3);
        }
        if (player.containerMenu instanceof ResearchTableMenu menu && menu.getBlockEntity() == table) {
            player.containerMenu.broadcastChanges();
        }
        return true;
    }

    @Nullable
    private static Aspect findResult(Aspect first, Aspect second) {
        for (Aspect aspect : ModAspects.all()) {
            List<Aspect> components = aspect.getComponents();
            if (!aspect.isPrimary() && components.size() >= 2
                    && ((components.get(0).equals(first) && components.get(1).equals(second))
                    || (components.get(0).equals(second) && components.get(1).equals(first)))) {
                return aspect;
            }
        }
        return null;
    }
}
