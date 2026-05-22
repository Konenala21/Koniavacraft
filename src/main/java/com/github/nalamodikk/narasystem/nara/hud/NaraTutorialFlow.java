package com.github.nalamodikk.narasystem.nara.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.github.nalamodikk.narasystem.nara.hud.NaraSoundHelper;

import java.util.List;

public class NaraTutorialFlow {

    public static final String RESEARCH_TABLE = "research_table";
    public static final String ALTAR_T6       = "altar_t6";

    private static boolean altarT6Shown = false;

    public static void resetSessionFlags() {
        altarT6Shown = false;
    }

    public static void start(String tutorialId) {
        switch (tutorialId) {
            case RESEARCH_TABLE -> startResearchTable();
            case ALTAR_T6       -> startAltarT6();
        }
    }

    private static void startResearchTable() {
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line1"))
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line1")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line2"))
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line2")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line3"))
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line3")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.research_table.line4"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.research_table.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("research_table", "line4"))
        ));
    }

    private static void startAltarT6() {
        if (altarT6Shown) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        altarT6Shown = true;
        Component playerName = mc.player.getDisplayName();
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.altar_t6.line1", playerName))
                        .withOnStart(() -> NaraSoundHelper.play("altar_t6", "line1")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.altar_t6.line2"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.altar_t6.confirm"),
                                () -> {})),
                        0, null)
                        .withOnStart(() -> NaraSoundHelper.play("altar_t6", "line2"))
        ));
    }

    private NaraTutorialFlow() {}
}
