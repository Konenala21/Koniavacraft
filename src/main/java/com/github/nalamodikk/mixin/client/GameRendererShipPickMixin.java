package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * 大實體 pick 盲區補強：vanilla 的準心實體 pick 用「玩家 reach 附近的小搜尋盒」找實體，但飛船很大卻只登記在
 * 中心那一格 section，玩家走到離中心遠的甲板就找不到船來點(同碰撞的 EntityShipCollisionMixin 盲區)。
 * 這裡在 GameRenderer.pick 跑完後，用放大搜尋盒(inflate 48)再對附近飛船的 bb 做一次射線；若命中且比 vanilla
 * 既有 hitResult 更近，就覆蓋成船的 EntityHitResult，讓準心打得到船方塊(後續 interactAt→pickLocal 決定哪一格)。
 */
@Mixin(GameRenderer.class)
public class GameRendererShipPickMixin {

    @Inject(method = "pick", at = @At("TAIL"))
    private void koniava$pickShipFarFromCentre(float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null || mc.player == null || mc.level == null) return;

        double reach = mc.player.blockInteractionRange();
        Vec3 eye = cam.getEyePosition(partialTicks);
        Vec3 look = cam.getViewVector(partialTicks);
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);

        // vanilla 已算出的命中距離(沒命中就用 reach)；只在飛船比它更近時才覆蓋。
        double bestDistSqr = (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS)
                ? mc.hitResult.getLocation().distanceToSqr(eye) : reach * reach;

        AABB search = cam.getBoundingBox().expandTowards(look.scale(reach));
        ShipEntity best = null;
        Vec3 bestHit = null;
        for (ShipEntity ship : ShipEntity.nearbyShips(mc.level, search)) {
            if (ship == cam.getVehicle()) continue;
            Optional<Vec3> clip = ship.getBoundingBox().clip(eye, end);
            if (clip.isEmpty()) continue;
            double d = clip.get().distanceToSqr(eye);
            if (d < bestDistSqr) {
                bestDistSqr = d;
                best = ship;
                bestHit = clip.get();
            }
        }
        if (best != null) {
            mc.hitResult = new EntityHitResult(best, bestHit);
            mc.crosshairPickEntity = best;
        }
    }
}
