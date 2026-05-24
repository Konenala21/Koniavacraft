package com.github.nalamodikk.narasystem.nara.hud;

import com.github.nalamodikk.common.config.ModClientConfig;
import com.github.nalamodikk.register.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;

@OnlyIn(Dist.CLIENT)
public class NaraSoundHelper {

    private static final String FALLBACK_LOCALE = "en";
    private static SimpleSoundInstance currentSound = null;

    public static void play(String group, String line) {
        stopCurrent();
        String locale = resolveLocale();
        String key = locale + "." + group + "." + line;
        DeferredHolder<SoundEvent, SoundEvent> holder = ModSounds.NARA.get(key);
        if (holder == null && !locale.equals(FALLBACK_LOCALE)) {
            holder = ModSounds.NARA.get(FALLBACK_LOCALE + "." + group + "." + line);
        }
        if (holder == null) return;
        float vol = 0.25f * (float) ModClientConfig.INSTANCE.naraVoiceVolume.get();
        currentSound = SimpleSoundInstance.forUI(holder.get(), 1.0f, vol);
        Minecraft.getInstance().getSoundManager().play(currentSound);
    }

    public static void stopCurrent() {
        if (currentSound != null) {
            Minecraft.getInstance().getSoundManager().stop(currentSound);
            currentSound = null;
        }
    }

    private static String resolveLocale() {
        String lang = Minecraft.getInstance().options.languageCode;
        return ModSounds.NARA.containsKey(lang + ".first_login.line1") ? lang : FALLBACK_LOCALE;
    }
}
