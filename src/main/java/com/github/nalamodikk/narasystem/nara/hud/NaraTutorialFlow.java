package com.github.nalamodikk.narasystem.nara.hud;

import net.minecraft.network.chat.Component;

import java.util.List;

public class NaraTutorialFlow {

    public static final String RESEARCH_TABLE = "research_table";

    public static void start(String tutorialId) {
        switch (tutorialId) {
            case RESEARCH_TABLE -> startResearchTable();
        }
    }

    private static void startResearchTable() {
        NaraDialogueManager.setPortraitShown();
        NaraDialogueManager.startDialogue(List.of(
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line1")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line2")),
                NaraDialogueLine.simple(
                        Component.translatable("nara.dialogue.tutorial.research_table.line3")),
                NaraDialogueLine.withChoices(
                        Component.translatable("nara.dialogue.tutorial.research_table.line4"),
                        List.of(new NaraChoice(
                                Component.translatable("nara.dialogue.tutorial.research_table.confirm"),
                                () -> {})),
                        0, null)
        ));
    }

    private NaraTutorialFlow() {}
}
