package com.github.nalamodikk.common.network.packet.server.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.research.skill.SkillEncoding;
import com.github.nalamodikk.research.skill.StoredSkill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/**
 * C2S: cycle the selected skill on the held spell-core wand to the next stored
 * skill. The index lives on the core item (nested inside the wand's WandCoreData),
 * so we rewrite the core stack and re-pack it. Server-authoritative.
 */
public record SwitchSkillPacket() implements CustomPacketPayload {

    public static final SwitchSkillPacket INSTANCE = new SwitchSkillPacket();

    public static final Type<SwitchSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "switch_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchSkillPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SwitchSkillPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            ItemStack wand = findWand(player);
            if (wand.isEmpty()) return;

            WandCoreData data = wand.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
            if (!data.hasCore()) return;

            ItemStack core = data.core();
            List<StoredSkill> skills = SkillEncoding.getSkills(core);
            if (skills.size() <= 1) return; // nothing to cycle through

            int index = core.getOrDefault(ModDataComponents.SELECTED_SKILL_INDEX, 0);
            index = (index + 1) % skills.size();

            ItemStack newCore = core.copy();
            newCore.set(ModDataComponents.SELECTED_SKILL_INDEX, index);
            wand.set(ModDataComponents.WAND_CORE_DATA, data.withCore(newCore));

            player.displayClientMessage(
                    Component.translatable("message.koniava.skill.switched", skills.get(index).name()), true);
        });
    }

    private static ItemStack findWand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof WandRodItem) return held;
        }
        return ItemStack.EMPTY;
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, SwitchSkillPacket::handle);
    }

    public static void send() {
        PacketDistributor.sendToServer(INSTANCE);
    }
}
