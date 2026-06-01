package com.github.nalamodikk.common.network.packet.server.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.skillencoder.SkillEncoderMenu;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.AspectSyncPacket;
import com.github.nalamodikk.research.skill.SkillEncoding;
import com.github.nalamodikk.research.skill.StoredSkill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Optional;

/**
 * C2S: encode a player-authored skill onto the spell core sitting in the open
 * Skill Core Encoding Bench. The server is authoritative: it validates the recipe
 * against the role dictionary and the player's aspect ownership before writing.
 */
public record EncodeSkillPacket(String name, ResourceLocation carrier,
                                List<ResourceLocation> effects, List<ResourceLocation> modifiers,
                                Optional<ResourceLocation> icon, int slot) implements CustomPacketPayload {

    public static final Type<EncodeSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "encode_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodeSkillPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, EncodeSkillPacket::name,
                    ResourceLocation.STREAM_CODEC, EncodeSkillPacket::carrier,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), EncodeSkillPacket::effects,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), EncodeSkillPacket::modifiers,
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), EncodeSkillPacket::icon,
                    ByteBufCodecs.VAR_INT, EncodeSkillPacket::slot,
                    EncodeSkillPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EncodeSkillPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;
            if (!(player.containerMenu instanceof SkillEncoderMenu menu)) return;

            ItemStack core = menu.getCore();
            if (core.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("message.koniava.encode.no_core"), true);
                return;
            }

            String name = packet.name().isBlank()
                    ? Component.translatable("gui.koniava.skill_encoder.unnamed").getString()
                    : packet.name();
            StoredSkill skill = new StoredSkill(name, packet.carrier(), packet.effects(),
                    packet.modifiers(), packet.icon());

            if (!SkillEncoding.isValidRecipe(skill)) {
                player.displayClientMessage(
                        Component.translatable("message.koniava.encode.invalid_recipe"), true);
                return;
            }

            ResearchSavedData data = ResearchSavedData.get(level);
            PlayerKnowledge knowledge = data.getOrCreate(player.getUUID());
            // Aspects are the material for authoring a skill: encoding consumes them
            // (casting later only spends mana). Verify enough, write, then consume + sync.
            if (!SkillEncoding.canAfford(knowledge, skill)) {
                player.displayClientMessage(
                        Component.translatable("message.koniava.encode.not_owned"), true);
                return;
            }

            SkillEncoding.setSlot(core, packet.slot(), skill);
            SkillEncoding.consumeAspects(knowledge, skill);
            data.setDirty();
            AspectSyncPacket.sendTo(player);
            menu.getBlockEntity().setChanged();
            player.displayClientMessage(
                    Component.translatable("message.koniava.encode.success", name), true);
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, EncodeSkillPacket::handle);
    }

    public static void send(String name, ResourceLocation carrier,
                            List<ResourceLocation> effects, List<ResourceLocation> modifiers,
                            Optional<ResourceLocation> icon, int slot) {
        PacketDistributor.sendToServer(new EncodeSkillPacket(name, carrier, effects, modifiers, icon, slot));
    }
}
