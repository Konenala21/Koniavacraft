package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.resources.language.LanguageManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the full resource-pack reload triggered by a language change with a
 * language-only reload.  LanguageManager.onResourceManagerReload() already calls
 * I18n.setLanguage(), Language.inject(), and the font-bidi callback — everything
 * that actually needs to change when the player picks a new language.  Textures,
 * block-models, sounds, and particle definitions do not need to be reloaded.
 *
 * Compatibility: priority=1100 (runs after default-priority=1000 mods).
 * If another mod already cancelled the reload we respect that and skip.
 * If our own reload throws we log a warning and fall through to the full reload.
 */
@Mixin(value = LanguageSelectScreen.class, priority = 1100)
public class LanguageSelectScreenMixin {

    @Shadow @Final LanguageManager languageManager;

    @Inject(
        method = "onDone",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;"
        ),
        cancellable = true
    )
    private void koniava_fastLanguageReload(CallbackInfo ci) {
        // Respect any higher-priority mod that already handled this.
        if (ci.isCancelled()) return;

        Minecraft mc = Minecraft.getInstance();
        try {
            // Synchronous, lightweight: reads lang JSON and patches I18n + Language + font bidi.
            this.languageManager.onResourceManagerReload(mc.getResourceManager());
            KoniavacraftMod.LOGGER.info(
                "[Koniava] Language switched to '{}' via fast reload (textures/models not reloaded).",
                this.languageManager.getSelected());
            ci.cancel(); // Skip Minecraft.reloadResourcePacks()
        } catch (Exception e) {
            // Safety net: if anything goes wrong let vanilla do the full reload.
            KoniavacraftMod.LOGGER.warn(
                "[Koniava] Fast language reload failed, falling back to full resource reload: {}",
                e.getMessage());
        }
    }
}
