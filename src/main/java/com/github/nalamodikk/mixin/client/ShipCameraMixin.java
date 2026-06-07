package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 駕駛飛船時三視角：F5 在第一人稱 / 船後追蹤 / 船旁上方觀察 之間循環（vanilla F5 已循環這三種相機類型，
 * 這裡只在第三人稱時把相機重定位成「相對飛船」而非相對玩家）。遠距離觀察交給望遠鏡，這裡不拉太遠。
 * 注意：沒做相機對地形的 clip，停在地面附近可能穿牆；飛行中沒問題。
 */
@Mixin(Camera.class)
public abstract class ShipCameraMixin {

    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow public abstract Vector3f getLookVector();

    @Inject(method = "setup", at = @At("TAIL"))
    private void koniava$shipCamera(BlockGetter level, Entity entity, boolean detached, boolean mirror,
                                    float partialTick, CallbackInfo ci) {
        if (!detached) return; // 第一人稱不動
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        if (ship.getControllingPassenger() != mc.player) return;

        // 用 partialTick 插值位置（跟船渲染同一套），否則相機用 tick 原始位置、船用插值位置 → 每幀錯位抽搐
        double ix = Mth.lerp(partialTick, ship.xOld, ship.getX());
        double iy = Mth.lerp(partialTick, ship.yOld, ship.getY());
        double iz = Mth.lerp(partialTick, ship.zOld, ship.getZ());
        Vec3 center = new Vec3(ix, iy + 1.5, iz); // 船中心略上（插值）
        Vector3f l = getLookVector();
        Vec3 look = new Vec3(l.x, l.y, l.z);
        Vec3 pos;
        if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            pos = center.add(0, 4, 0).subtract(look.scale(8.0)); // 觀察：船旁上方俯瞰
        } else {
            pos = center.subtract(look.scale(7.0)).add(0, 1.0, 0); // 追蹤：船後方
        }
        setPosition(pos);
    }
}
