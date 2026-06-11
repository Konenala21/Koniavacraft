package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 在飛船裡(頭上有船方塊遮著)時不畫下雨特效。船方塊不在真實世界、雨依真實世界高度圖算 → 預設會穿過船
 * 在艙內下雨。攔 renderSnowAndRain:鏡頭實體被船遮蔽就整個取消這幀的雨繪製。露天甲板(沒遮)照常下。
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererRainMixin {

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void koniava$noRainUnderShip(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null || mc.level == null) return;
        List<ShipEntity> near = mc.level.getEntitiesOfClass(ShipEntity.class, cam.getBoundingBox().inflate(2));
        for (ShipEntity s : near) {
            if (s.shelters(cam.getX(), cam.getY() + cam.getEyeHeight(), cam.getZ())) {
                ci.cancel();
                return;
            }
        }
    }
}
