package com.github.nalamodikk.narasystem.nara.hud;

import net.minecraft.network.chat.Component;

import java.util.List;

public record NaraDialogueLine(
        Component text,
        List<NaraChoice> choices,
        int choiceTimeoutTicks,    // 0 = no timeout, just wait for click
        Runnable onTimeout,        // null = just advance
        boolean revealPortrait     // true = reveal portrait after this line
) {
    public static NaraDialogueLine simple(Component text) {
        return new NaraDialogueLine(text, List.of(), 0, null, false);
    }

    public static NaraDialogueLine withChoices(Component text, List<NaraChoice> choices, int timeoutTicks, Runnable onTimeout) {
        return new NaraDialogueLine(text, choices, timeoutTicks, onTimeout, false);
    }

    public static NaraDialogueLine reveal(Component text) {
        return new NaraDialogueLine(text, List.of(), 0, null, true);
    }
}
