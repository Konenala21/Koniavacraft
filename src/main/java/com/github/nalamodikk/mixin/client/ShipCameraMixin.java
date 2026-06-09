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
    @Shadow public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("TAIL"))
    private void koniava$shipCamera(BlockGetter level, Entity entity, boolean detached, boolean mirror,
                                    float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) return;
        if (ship.getControllingPassenger() != mc.player) return;

        if (!detached) {
            // 第一人稱：vanilla 把眼睛放在「座位 + 世界上 × eyeHeight」。船傾斜時座位的「上」是斜的，世界上的眼睛
            // 就浮在斜座位正上方而偏位。改成沿「船的上」偏移 eyeHeight → 眼睛待在座艙裡跟著斜，不偏離座位。
            Vector3f up = ship.orientation(partialTick).transform(new Vector3f(0f, 1f, 0f));
            if (Math.abs(up.x) > 1.0e-4 || Math.abs(up.z) > 1.0e-4) { // 只在傾斜時動
                double eyeH = mc.player.getEyeHeight();
                setPosition(getPosition().add(up.x * eyeH, (up.y - 1.0) * eyeH, up.z * eyeH));
            }
            return;
        }

        // 用 partialTick 插值位置（跟船渲染同一套），否則相機用 tick 原始位置、船用插值位置 → 每幀錯位抽搐
        double ix = Mth.lerp(partialTick, ship.xOld, ship.getX());
        double iy = Mth.lerp(partialTick, ship.yOld, ship.getY());
        double iz = Mth.lerp(partialTick, ship.zOld, ship.getZ());
        // 鏡頭距離依船大小拉遠：小船維持原本 7/8 格，大船退到看得見整艘。
        double size = 6.0; // 最長邊：中心抬高用
        double diag = 6.0; // 對角線：鏡頭距離用(船俯仰/側翻後對角線朝鏡頭、比最長邊長,不夠遠就超出畫面)
        if (ship.getContraption() != null) {
            var b = ship.getContraption().bounds();
            size = Math.max(Math.max(b.getXsize(), b.getYsize()), b.getZsize());
            diag = Math.sqrt(b.getXsize() * b.getXsize() + b.getYsize() * b.getYsize() + b.getZsize() * b.getZsize());
        }
        double back  = Mth.clamp(diag * 0.8, 7.0, 70.0);
        double front = Mth.clamp(diag * 0.9, 8.0, 80.0);
        Vec3 center = new Vec3(ix, iy + 1.5 + size * 0.15, iz); // 船中心略上（大船抬高，框得住）
        Vector3f l = getLookVector();
        Vec3 look = new Vec3(l.x, l.y, l.z);
        Vec3 pos;
        if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            pos = center.add(0, back * 0.5, 0).subtract(look.scale(front)); // 觀察：船旁上方俯瞰
        } else {
            pos = center.subtract(look.scale(back)).add(0, 1.0, 0); // 追蹤：船後方
        }
        setPosition(pos);
    }
}
