package com.github.nalamodikk.narasystem.nara.network.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.hud.NaraDialogueLine;
import com.github.nalamodikk.narasystem.nara.hud.NaraDialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/** S2C：玩家在鏡中世界死亡後，娜拉出聲嘲諷。 */
public record NaraTauntPacket() implements CustomPacketPayload {

    public static final NaraTauntPacket INSTANCE = new NaraTauntPacket();

    public static final Type<NaraTauntPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_taunt"));

    public static final StreamCodec<FriendlyByteBuf, NaraTauntPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, NaraTauntPacket::handle);
    }

    private static void handle(NaraTauntPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            NaraDialogueManager.setPortraitShown();
            NaraDialogueManager.startDialogueImmediate(List.of(
                    NaraDialogueLine.simple(Component.translatable("nara.dialogue.void_mirror.death_taunt"))
            ));
        });
    }
}
