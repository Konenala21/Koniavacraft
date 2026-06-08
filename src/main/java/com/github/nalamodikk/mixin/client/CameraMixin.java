package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.client.cinematic.BossDeathCameraManager;
import com.github.nalamodikk.client.cinematic.Phase2TransitionManager;
import com.github.nalamodikk.client.cinematic.VoidMirrorIntroManager;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract Entity getEntity();

    /** 第三人稱距離依船大小拉遠：駕駛大船時鏡頭往後退到看得見整艘。小船(<~5 格)維持原樣。 */
    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private float koniava$shipCameraZoom(float maxZoom) {
        Entity entity = getEntity();
        if (entity != null && entity.getVehicle() instanceof ShipEntity ship && ship.getContraption() != null) {
            AABB b = ship.getContraption().bounds();
            double size = Math.max(Math.max(b.getXsize(), b.getYsize()), b.getZsize());
            float factor = (float) Mth.clamp(size / 5.0, 1.0, 8.0);
            return maxZoom * factor;
        }
        return maxZoom;
    }

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
