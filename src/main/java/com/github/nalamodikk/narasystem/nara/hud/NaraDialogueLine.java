package com.github.nalamodikk.narasystem.nara.hud;

import net.minecraft.network.chat.Component;

import java.util.List;

public record NaraDialogueLine(
        Component text,
        List<NaraChoice> choices,
        int choiceTimeoutTicks,
        Runnable onTimeout,
        Runnable onStart,
        boolean revealPortrait
) {
    public static NaraDialogueLine simple(Component text) {
        return new NaraDialogueLine(text, List.of(), 0, null, null, false);
    }

    public static NaraDialogueLine withChoices(Component text, List<NaraChoice> choices, int timeoutTicks, Runnable onTimeout) {
        return new NaraDialogueLine(text, choices, timeoutTicks, onTimeout, null, false);
    }

    public static NaraDialogueLine reveal(Component text) {
        return new NaraDialogueLine(text, List.of(), 0, null, null, true);
    }

    public NaraDialogueLine withOnStart(Runnable start) {
        return new NaraDialogueLine(text, choices, choiceTimeoutTicks, onTimeout, start, revealPortrait);
    }
}
