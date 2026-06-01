package com.github.nalamodikk.common.network.packet.client.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.hud.ClientSkillCooldowns;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C: tells the casting player a skill slot just started cooling down, so the
 * client HUD can show the remaining time. The handler only runs client-side, so
 * the reference to {@link ClientSkillCooldowns} never loads on a dedicated server.
 */
public record SkillCooldownPacket(int slot, int cooldown, int gcd) implements CustomPacketPayload {

    public static final Type<SkillCooldownPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "skill_cooldown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillCooldownPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SkillCooldownPacket::slot,
                    ByteBufCodecs.VAR_INT, SkillCooldownPacket::cooldown,
                    ByteBufCodecs.VAR_INT, SkillCooldownPacket::gcd,
                    SkillCooldownPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SkillCooldownPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSkillCooldowns.onCast(packet.slot(), packet.cooldown(), packet.gcd()));
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, SkillCooldownPacket::handle);
    }

    public static void send(ServerPlayer player, int slot, int cooldown, int gcd) {
        PacketDistributor.sendToPlayer(player, new SkillCooldownPacket(slot, cooldown, gcd));
    }
}
