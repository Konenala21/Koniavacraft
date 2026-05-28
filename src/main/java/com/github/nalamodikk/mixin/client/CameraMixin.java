package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.client.cinematic.BossDeathCameraManager;
import com.github.nalamodikk.client.cinematic.Phase2TransitionManager;
import com.github.nalamodikk.client.cinematic.VoidMirrorIntroManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void koniava$voidMirrorIntroCamera(BlockGetter level, Entity entity, boolean detached,
                                               boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (VoidMirrorIntroManager.isActive()) {
            VoidMirrorIntroManager.CameraPose pose = VoidMirrorIntroManager.getCameraPose(partialTick);
            if (pose != null) {
                this.setRotation(pose.yaw(), pose.pitch());
                this.setPosition(pose.x(), pose.y(), pose.z());
            }
            return;
        }
        if (Phase2TransitionManager.isActive()) {
            Phase2TransitionManager.CameraPose pose = Phase2TransitionManager.getCameraPose(partialTick);
            if (pose != null) {
                this.setRotation(pose.yaw(), pose.pitch());
                this.setPosition(pose.x(), pose.y(), pose.z());
            }
            return;
        }
        if (BossDeathCameraManager.isActive()) {
            BossDeathCameraManager.CameraPose pose = BossDeathCameraManager.getCameraPose(partialTick);
            if (pose != null) {
                this.setRotation(pose.yaw(), pose.pitch());
                this.setPosition(pose.x(), pose.y(), pose.z());
            }
        }
    }
}
