package com.github.nalamodikk.narasystem.nara.hud;

import com.github.nalamodikk.narasystem.nara.network.client.NaraAngryPacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraCreeperPunishPacket;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class NaraFirstLoginFlow {

    private static final int BASE_TIMEOUT  = 200; // 10秒
    private static final int TIMEOUT_STEP  = 16;  // 每次縮短約0.8秒
    private static final int MIN_TIMEOUT   = 40;  // 最短2秒
    private static final int MAX_IGNORES   = 10;

    // 累積忽略次數，跨 replay 保留，重新登入重置
    private static int ignoreCount = 0;

    public static void start(boolean isSteveModel) {
        String honorificKey = isSteveModel
                ? "nara.dialogue.first_login.honorific.sir"
                : "nara.dialogue.first_login.honorific.miss";

        NaraDialogueManager.setPortraitHidden();

        var line1 = NaraDialogueLine.withChoices(
                Component.translatable("nara.dialogue.first_login.line1", Component.translatable(honorificKey)),
                List.of(
                        new NaraChoice(Component.translatable("nara.dialogue.first_login.choice1a"), () -> {}),
                        new NaraChoice(Component.translatable("nara.dialogue.first_login.choice1b"), () -> {})
                ),
                0, null
        );

        var line2 = NaraDialogueLine.simple(Component.translatable("nara.dialogue.first_login.line2"));
        var line3 = NaraDialogueLine.simple(Component.translatable("nara.dialogue.first_login.line3"));

        var line4 = NaraDialogueLine.withChoices(
                Component.translatable("nara.dialogue.first_login.line4"),
                List.of(new NaraChoice(Component.translatable("nara.dialogue.first_login.choice4a"), () -> {})),
                currentTimeout(),
                NaraFirstLoginFlow::onIgnoreTimeout
        );

        var line5 = NaraDialogueLine.reveal(Component.translatable("nara.dialogue.first_login.line5"));

        var line6 = NaraDialogueLine.withChoices(
                Component.translatable("nara.dialogue.first_login.line6"),
                List.of(new NaraChoice(Component.translatable("nara.dialogue.first_login.choice6a"), () -> {})),
                currentTimeout(),
                () -> {}
        ).withOnStart(NaraWatchHighlight::show);

        var line7 = NaraDialogueLine.simple(Component.translatable("nara.dialogue.first_login.line7"));
        var line8 = NaraDialogueLine.simple(Component.translatable("nara.dialogue.first_login.line8"));

        NaraDialogueManager.startDialogue(List.of(line1, line2, line3, line4, line5, line6, line7, line8));
    }

    public static int currentTimeout() {
        return Math.max(MIN_TIMEOUT, BASE_TIMEOUT - ignoreCount * TIMEOUT_STEP);
    }

    public static void onIgnoreTimeout() {
        ignoreCount++;
        NaraDialogueManager.close();

        if (ignoreCount >= MAX_IGNORES) {
            triggerAngryDialogue();
        } else {
            PacketDistributor.sendToServer(new NaraCreeperPunishPacket());
        }
    }

    private static void triggerAngryDialogue() {
        NaraDialogueManager.setPortraitMad();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(Component.translatable("nara.dialogue.angry.line1"))
        ));
        NaraAngryPacket.send();
    }

    public static void resetIgnoreCount() {
        ignoreCount = 0;
    }
}
