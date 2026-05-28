package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.client.cinematic.BossDeathCameraManager;
import com.github.nalamodikk.client.cinematic.VoidMirrorIntroManager;
import com.github.nalamodikk.client.renderer.altar.AltarCameraController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerT6Mixin {

    // Suppress mouse turning while T6 altar camera is locked.
    // This prevents the player from accidentally rotating away from the magic circle.
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void koniava$suppressTurnForT6(double deltaTime, CallbackInfo ci) {
        if (AltarCameraController.isCameraLocked()
                || VoidMirrorIntroManager.isActive()
                || BossDeathCameraManager.isActive()) {
            ci.cancel();
        }
    }
}
