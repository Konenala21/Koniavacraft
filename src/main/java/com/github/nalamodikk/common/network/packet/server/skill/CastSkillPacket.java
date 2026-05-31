package com.github.nalamodikk.common.network.packet.server.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.skill.SkillContext;
import com.github.nalamodikk.research.skill.SkillEffect;
import com.github.nalamodikk.research.skill.SkillRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;

/**
 * C2S: the player asks to cast the given skill.
 *
 * The server resolves the skill from {@link SkillRegistry}, checks and consumes
 * the player's aspects ({@link PlayerKnowledge}), then runs the effect. All
 * authority is server-side; the client only requests.
 */
public record CastSkillPacket(ResourceLocation skillId) implements CustomPacketPayload {

    public static final Type<CastSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "cast_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSkillPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, CastSkillPacket::skillId,
                    CastSkillPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CastSkillPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            SkillEffect skill = SkillRegistry.get(packet.skillId());
            if (skill == null) return;

            ResearchSavedData data = ResearchSavedData.get(level);
            PlayerKnowledge knowledge = data.getOrCreate(player.getUUID());

            // 1) check every aspect is affordable before spending anything
            for (Map.Entry<ResourceLocation, Integer> entry : skill.cost().entrySet()) {
                if (knowledge.getAspectCount(entry.getKey()) < entry.getValue()) {
                    player.displayClientMessage(
                            Component.translatable("message.koniava.skill.not_enough_aspects"), true);
                    return;
                }
            }

            // 2) consume
            for (Map.Entry<ResourceLocation, Integer> entry : skill.cost().entrySet()) {
                Aspect aspect = ModAspects.get(entry.getKey());
                if (aspect != null) knowledge.discoverAspect(aspect, -entry.getValue());
            }
            data.setDirty();

            // 3) run the effect
            skill.execute(new SkillContext(level, player));
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, CastSkillPacket::handle);
    }

    public static void send(ResourceLocation skillId) {
        PacketDistributor.sendToServer(new CastSkillPacket(skillId));
    }
}
