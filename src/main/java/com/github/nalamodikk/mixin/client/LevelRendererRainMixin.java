package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在飛船裡(頭上有船方塊遮著)時不畫下雨特效。船方塊不在真實世界、雨依真實世界高度圖算 → 預設會穿過船
 * 在艙內下雨。攔 renderSnowAndRain:實際鏡頭被船遮蔽就整個取消這幀的雨繪製。露天甲板(沒遮)照常下。
 *
 * 用「實際鏡頭位置」(getMainCamera)不是玩家位置:第三人稱鏡頭在船外時照常下雨,而不是被玩家被遮就整片取消。
 * 搜尋範圍放大(64):大船的實體 bb 小/在中心,小範圍會偶爾漏找到船 → 那幀算沒遮 → 雨閃一下。
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererRainMixin {

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void koniava$noRainUnderShip(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        AABB box = new AABB(cam.x, cam.y, cam.z, cam.x, cam.y, cam.z).inflate(64);
        for (ShipEntity s : mc.level.getEntitiesOfClass(ShipEntity.class, box)) {
            if (s.shelters(cam.x, cam.y, cam.z)) {
                ci.cancel();
                return;
            }
        }
    }
}
