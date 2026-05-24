package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.common.config.ModClientConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundOptionsScreen.class)
public class SoundOptionsScreenMixin {

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void koniava_addNaraVolumeSlider(CallbackInfo ci) {
        OptionInstance<Double> naraVolume = new OptionInstance<>(
                "koniava.config.nara.voiceVolume",
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(
                        caption,
                        Component.translatable("options.percent_value", (int)(value * 100))),
                OptionInstance.UnitDouble.INSTANCE,
                ModClientConfig.INSTANCE.naraVoiceVolume.get(),
                value -> ModClientConfig.INSTANCE.naraVoiceVolume.set(value)
        );
        ((OptionsSubScreenAccessor)(Object)this).koniava_getOptionsList().addBig(naraVolume);
    }
}
