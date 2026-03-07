package com.github.nalamodikk.register.client;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DeveloperModeConfirmScreen extends Screen {
    public static final String REQUIRED_PHRASE = "I_AM_DEVELOPER";

    private final Screen parentScreen;
    private final Consumer<Boolean> callback;

    private boolean step1Confirmed;
    private boolean step2Confirmed;
    private boolean resolved;
    private EditBox phraseInput;
    private Button confirmButton;

    public DeveloperModeConfirmScreen(Screen parentScreen, Consumer<Boolean> callback) {
        super(Component.translatable("screen.koniava.dev_mode_confirm.title"));
        this.parentScreen = parentScreen;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        CycleButton<Boolean> step1Button = CycleButton.onOffBuilder(false)
                .create(
                        centerX - 150,
                        startY,
                        300,
                        20,
                        Component.translatable("screen.koniava.dev_mode_confirm.step1"),
                        (button, value) -> {
                            this.step1Confirmed = value;
                            updateConfirmState();
                        }
                );
        this.addRenderableWidget(step1Button);

        CycleButton<Boolean> step2Button = CycleButton.onOffBuilder(false)
                .create(
                        centerX - 150,
                        startY + 24,
                        300,
                        20,
                        Component.translatable("screen.koniava.dev_mode_confirm.step2"),
                        (button, value) -> {
                            this.step2Confirmed = value;
                            updateConfirmState();
                        }
                );
        this.addRenderableWidget(step2Button);

        this.phraseInput = new EditBox(
                this.font,
                centerX - 150,
                startY + 52,
                300,
                20,
                Component.translatable("screen.koniava.dev_mode_confirm.phrase_label")
        );
        this.phraseInput.setHint(Component.translatable("screen.koniava.dev_mode_confirm.phrase_hint"));
        this.phraseInput.setResponder(value -> updateConfirmState());
        this.addRenderableWidget(this.phraseInput);

        this.confirmButton = Button.builder(
                        Component.translatable("screen.koniava.dev_mode_confirm.confirm"),
                        button -> {
                            this.resolved = true;
                            this.callback.accept(true);
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(this.parentScreen);
                            }
                        }
                )
                .bounds(centerX - 150, startY + 80, 146, 20)
                .build();
        this.confirmButton.active = false;
        this.addRenderableWidget(this.confirmButton);

        Button cancelButton = Button.builder(
                        Component.translatable("screen.koniava.dev_mode_confirm.cancel"),
                        button -> {
                            this.resolved = true;
                            this.callback.accept(false);
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(this.parentScreen);
                            }
                        }
                )
                .bounds(centerX + 4, startY + 80, 146, 20)
                .build();
        this.addRenderableWidget(cancelButton);
    }

    private void updateConfirmState() {
        if (this.confirmButton == null || this.phraseInput == null) {
            return;
        }

        boolean phraseMatched = REQUIRED_PHRASE.equals(this.phraseInput.getValue().trim());
        this.confirmButton.active = this.step1Confirmed && this.step2Confirmed && phraseMatched;
    }

    @Override
    public void onClose() {
        if (!this.resolved) {
            this.callback.accept(false);
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 96, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.koniava.dev_mode_confirm.warning", REQUIRED_PHRASE),
                this.width / 2,
                this.height / 2 - 84,
                0xFFAA00
        );
    }
}
