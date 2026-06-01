package com.github.nalamodikk.common.network.packet.server.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.research.skill.SkillCasting;
import com.github.nalamodikk.research.skill.SkillEffect;
import com.github.nalamodikk.research.skill.SkillEncoding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * C2S: the player asks to cast the given skill.
 *
 * The server resolves the selected skill encoded on the held wand's spell core,
 * applies the dual cost model (aspect gate + wand mana), then runs the effect.
 * All authority is server-side; the client only requests.
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
            if (!(player.level() instanceof ServerLevel)) return;

            ItemStack wand = findCastingWand(player);
            if (wand.isEmpty()) return;

            // Only the core's selected player-authored skill can be cast. A core
            // with no encoded skill (or no core) does nothing: the encoding bench
            // is the sole source of skills, gating play through it by design.
            SkillEffect skill = SkillEncoding.resolveCastSkill(wand);
            if (skill == null) return;

            // Which core slot is selected (for per-slot cooldown); 0 for demo fallback.
            WandCoreData data = wand.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
            int slot = data.hasCore() ? data.core().getOrDefault(ModDataComponents.SELECTED_SKILL_INDEX, 0) : 0;

            // Dual cost model: aspects gate, wand mana is consumed. See SkillCasting.
            SkillCasting.tryCast(player, wand, skill, slot);
        });
    }

    /** The wand the player is holding (main hand preferred) that can cast. */
    private static ItemStack findCastingWand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof WandRodItem) return held;
        }
        return ItemStack.EMPTY;
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, CastSkillPacket::handle);
    }

    public static void send(ResourceLocation skillId) {
        PacketDistributor.sendToServer(new CastSkillPacket(skillId));
    }
}
