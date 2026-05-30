package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.common.config.ModClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 vanilla 音效設定畫面追加 Koniavacraft 自家的音量條：
 *   - Boss 戰鬥 BGM
 *   - 娜拉配音
 *
 * 注入點選在 vanilla `soundDevice()` 呼叫之前 → 我們的滑桿緊跟在原版 source volume 後、SoundDevice 之前
 * 用 addSmall（兩兩配對一列）→ 跟 vanilla source volumes 同款排版
 * Label key 用 options.koniava.* 對齊 vanilla options.* 命名
 */
@Mixin(SoundOptionsScreen.class)
public class SoundOptionsScreenMixin {

    @Inject(
            method = "addOptions",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;soundDevice()Lnet/minecraft/client/OptionInstance;",
                    shift = At.Shift.BEFORE)
    )
    private void koniava$addKoniavaSoundSliders(CallbackInfo ci) {
        var list = ((OptionsSubScreenAccessor) (Object) this).koniava_getOptionsList();

        OptionInstance<Double> bossMusic = makeVolumeSlider(
                "options.koniava.boss_music",
                ModClientConfig.INSTANCE.bossMusicVolume.get(),
                value -> ModClientConfig.INSTANCE.bossMusicVolume.set(value));

        OptionInstance<Double> naraVoice = makeVolumeSlider(
                "options.koniava.nara_voice",
                ModClientConfig.INSTANCE.naraVoiceVolume.get(),
                value -> ModClientConfig.INSTANCE.naraVoiceVolume.set(value));

        // 兩兩配對 → 跟 vanilla source volumes 同樣的 addSmall 排版
        list.addSmall(new OptionInstance[]{ bossMusic, naraVoice });

        // 「重載聲音」按鈕（螢幕右上）：只重啟 SoundEngine（重建 OpenAL），不碰 texture/model。
        // 救 OS 切換音訊裝置後 OpenAL 卡死、聲音消失的情況（比 F3+T 輕，不用重開遊戲）。
        Screen screen = (Screen) (Object) this;
        int bw = 110, bh = 20;
        Button reloadBtn = Button.builder(
                        Component.translatable("gui.koniava.reload_sound"),
                        b -> Minecraft.getInstance().getSoundManager().reload())
                .bounds(screen.width - bw - 6, 6, bw, bh)
                .tooltip(Tooltip.create(Component.translatable("gui.koniava.reload_sound.tooltip")))
                .build();
        ((ScreenAccessor) (Object) this).koniava_addRenderableWidget(reloadBtn);
    }

    @org.spongepowered.asm.mixin.Unique
    private static OptionInstance<Double> makeVolumeSlider(String labelKey, double initial,
                                                            java.util.function.Consumer<Double> setter) {
        return new OptionInstance<>(
                labelKey,
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(
                        caption,
                        Component.literal((int)(value * 100) + "%")),
                OptionInstance.UnitDouble.INSTANCE,
                initial,
                value -> setter.accept(value)
        );
    }
}
