package com.github.nalamodikk.narasystem.nara.network.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.hud.NaraChoice;
import com.github.nalamodikk.narasystem.nara.hud.NaraDialogueManager;
import com.github.nalamodikk.narasystem.nara.hud.NaraDialogueLine;
import com.github.nalamodikk.narasystem.nara.hud.NaraFirstLoginFlow;
import com.github.nalamodikk.narasystem.nara.hud.NaraSoundHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public record NaraStartDialoguePacket() implements CustomPacketPayload {

    public static final Type<NaraStartDialoguePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_start_dialogue"));

    public static final StreamCodec<FriendlyByteBuf, NaraStartDialoguePacket> STREAM_CODEC =
            StreamCodec.unit(new NaraStartDialoguePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, NaraStartDialoguePacket::handle);
        registrar.playToClient(Punishment.TYPE, Punishment.STREAM_CODEC, Punishment::handle);
    }

    private static void handle(NaraStartDialoguePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            boolean isSteveModel = mc.player.getSkin().model() == PlayerSkin.Model.WIDE;
            NaraFirstLoginFlow.start(isSteveModel);
        });
    }

    // 懲罰對話（苦力怕彩蛋後）
    public record Punishment() implements CustomPacketPayload {

        public static final Type<Punishment> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "nara_punishment_dialogue"));

        public static final StreamCodec<FriendlyByteBuf, Punishment> STREAM_CODEC =
                StreamCodec.unit(new Punishment());

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        private static void handle(Punishment packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                NaraDialogueManager.setPortraitShown();
                NaraDialogueManager.startDialogue(List.of(
                        NaraDialogueLine.simple(Component.translatable("nara.dialogue.punishment.line1"))
                                .withOnStart(() -> NaraSoundHelper.play("punishment", "line1")),
                        NaraDialogueLine.withChoices(
                                Component.translatable("nara.dialogue.punishment.line2"),
                                List.of(new NaraChoice(Component.translatable("nara.dialogue.punishment.choice_sorry"), () -> {})),
                                NaraFirstLoginFlow.currentTimeout(), NaraFirstLoginFlow::onIgnoreTimeout
                        ).withOnStart(() -> NaraSoundHelper.play("punishment", "line2")),
                        NaraDialogueLine.simple(Component.translatable("nara.dialogue.punishment.forgiven"))
                                .withOnStart(() -> NaraSoundHelper.play("punishment", "forgiven")),
                        NaraDialogueLine.simple(Component.translatable("nara.dialogue.first_login.line8"))
                                .withOnStart(() -> NaraSoundHelper.play("first_login", "line8"))
                ));
            });
        }
    }
}
