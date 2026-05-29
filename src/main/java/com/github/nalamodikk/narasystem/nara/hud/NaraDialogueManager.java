package com.github.nalamodikk.narasystem.nara.hud;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.github.nalamodikk.narasystem.nara.hud.NaraSoundHelper;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@OnlyIn(Dist.CLIENT)
public class NaraDialogueManager {

    public enum PortraitState { HIDDEN, REVEALING, SHOWN }
    public enum DialogueState { INACTIVE, TYPING, WAITING, CHOOSING }

    private static final int CHARS_PER_TICK = 1;
    private static final int REVEAL_DURATION_TICKS = 20;

    private static final Queue<NaraDialogueLine> lineQueue = new ArrayDeque<>();
    private static NaraDialogueLine currentLine = null;
    private static DialogueState dialogueState = DialogueState.INACTIVE;
    private static List<NaraDialogueLine> pendingDialogue = null;
    private static PortraitState portraitState = PortraitState.HIDDEN;
    private static boolean overlayOnScreen = false;

    private static Component displayName = Component.translatable("nara.hud.name.unknown");
    private static boolean portraitMad = false;
    private static int madCooldown = 0;
    private static int charIndex = 0;
    private static int choiceTimerTicks = 0;
    private static int revealTicks = 0;
    private static int selectedChoiceIndex = 0;
    private static boolean charAddedThisTick = false;

    public static void startDialogue(List<NaraDialogueLine> lines) {
        if (dialogueState != DialogueState.INACTIVE) {
            pendingDialogue = lines;
            return;
        }
        lineQueue.clear();
        lineQueue.addAll(lines);
        charIndex = 0;
        advanceLine();
    }

    public static void startDialogueImmediate(List<NaraDialogueLine> lines) {
        pendingDialogue = null;
        lineQueue.clear();
        lineQueue.addAll(lines);
        charIndex = 0;
        advanceLine();
    }

    public static void tick() {
        charAddedThisTick = false;
        if (portraitState == PortraitState.REVEALING) {
            revealTicks++;
            if (revealTicks >= REVEAL_DURATION_TICKS) {
                portraitState = PortraitState.SHOWN;
                revealTicks = 0;
            }
        }

        if (currentLine == null) return;

        switch (dialogueState) {
            case TYPING -> {
                String full = currentLine.text().getString();
                if (charIndex < full.length()) {
                    charIndex = Math.min(charIndex + CHARS_PER_TICK, full.length());
                    charAddedThisTick = true;
                } else if (NaraSoundHelper.isPlaying()) {
                    // 文字打完但配音還在播：停在 typing 完成狀態，不要切到 WAITING
                    // 否則玩家會看到「下一句」提示就點下去 → advanceLine 把配音吃掉
                } else {
                    dialogueState = currentLine.choices().isEmpty()
                            ? DialogueState.WAITING
                            : DialogueState.CHOOSING;
                    choiceTimerTicks = 0;
                }
            }
            case CHOOSING -> {
                if (currentLine.choiceTimeoutTicks() > 0) {
                    choiceTimerTicks++;
                    if (choiceTimerTicks >= currentLine.choiceTimeoutTicks()) {
                        if (currentLine.onTimeout() != null) currentLine.onTimeout().run();
                        // 只在 onTimeout 沒有自己改變狀態時才 advance
                        if (dialogueState == DialogueState.CHOOSING) {
                            advanceLine();
                        }
                    }
                }
            }
            default -> {}
        }
    }

    public static void onPlayerClick() {
        if (dialogueState == DialogueState.TYPING) {
            // 跳到全文（state machine 會在配音播完才切到 WAITING，所以這個點擊只是把字補齊）
            charIndex = currentLine.text().getString().length();
        } else if (dialogueState == DialogueState.WAITING) {
            advanceLine();
        }
    }

    public static void selectChoice(int index) {
        if (dialogueState != DialogueState.CHOOSING) return;
        if (currentLine == null) return;
        List<NaraChoice> choices = currentLine.choices();
        if (index < 0 || index >= choices.size()) return;
        choices.get(index).onSelect().run();
        advanceLine();
    }

    public static void confirmSelectedChoice() {
        selectChoice(selectedChoiceIndex);
    }

    public static void scrollChoice(int delta) {
        if (dialogueState != DialogueState.CHOOSING) return;
        if (currentLine == null) return;
        int size = currentLine.choices().size();
        if (size == 0) return;
        selectedChoiceIndex = (selectedChoiceIndex + delta + size) % size;
    }

    public static int getSelectedChoiceIndex() { return selectedChoiceIndex; }

    private static void advanceLine() {
        NaraSoundHelper.stopCurrent();
        if (lineQueue.isEmpty()) {
            currentLine = null;
            dialogueState = DialogueState.INACTIVE;
            overlayOnScreen = false;
            if (pendingDialogue != null) {
                List<NaraDialogueLine> next = pendingDialogue;
                pendingDialogue = null;
                lineQueue.addAll(next);
                charIndex = 0;
                advanceLine();
            }
            return;
        }
        currentLine = lineQueue.poll();
        charIndex = 0;
        selectedChoiceIndex = 0;
        dialogueState = DialogueState.TYPING;

        if (currentLine.onStart() != null) currentLine.onStart().run();

        if (currentLine.revealPortrait()) {
            portraitState = PortraitState.REVEALING;
            revealTicks = 0;
            displayName = Component.translatable("nara.hud.name");
        }
    }

    public static void close() {
        NaraSoundHelper.stopCurrent();
        lineQueue.clear();
        pendingDialogue = null;
        currentLine = null;
        dialogueState = DialogueState.INACTIVE;
        choiceTimerTicks = 0;
        selectedChoiceIndex = 0;
        overlayOnScreen = false;
        NaraGuiHighlight.hide();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static boolean isActive() { return dialogueState != DialogueState.INACTIVE; }
    public static boolean isOverlayOnScreen() { return overlayOnScreen; }
    public static void setOverlayOnScreen(boolean value) { overlayOnScreen = value; }
    public static void setGuiHighlight(float relX, float relY, int size) { NaraGuiHighlight.show(relX, relY, size); }
    public static void clearGuiHighlight() { NaraGuiHighlight.hide(); }
    public static DialogueState getDialogueState() { return dialogueState; }
    public static PortraitState getPortraitState() { return portraitState; }
    public static Component getDisplayName() { return displayName; }
    public static int getRevealTicks() { return revealTicks; }
    public static int getRevealDuration() { return REVEAL_DURATION_TICKS; }

    public static String getShownText() {
        if (currentLine == null) return "";
        String full = currentLine.text().getString();
        return full.substring(0, Math.min(charIndex, full.length()));
    }

    public static List<NaraChoice> getCurrentChoices() {
        if (currentLine == null) return List.of();
        return currentLine.choices();
    }

    public static int getChoiceTimerTicks() { return choiceTimerTicks; }
    public static int getChoiceTimeoutTicks() {
        if (currentLine == null) return 0;
        return currentLine.choiceTimeoutTicks();
    }

    public static void setPortraitHidden() { portraitState = PortraitState.HIDDEN; displayName = Component.translatable("nara.hud.name.unknown"); portraitMad = false; madCooldown = 0; }
    public static void setPortraitShown() {
        portraitState = PortraitState.SHOWN;
        displayName = Component.translatable("nara.hud.name");
        if (madCooldown > 0 && --madCooldown == 0) portraitMad = false;
    }
    public static void setPortraitMad() { portraitState = PortraitState.SHOWN; displayName = Component.translatable("nara.hud.name"); portraitMad = true; madCooldown = 3; }
    public static boolean isPortraitMad() { return portraitMad; }
    public static boolean wasCharAddedThisTick() { return charAddedThisTick; }
}
