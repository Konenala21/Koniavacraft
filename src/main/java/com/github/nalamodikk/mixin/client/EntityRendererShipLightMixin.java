package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.client.renderer.entity.ShipLight;
import com.github.nalamodikk.client.renderer.entity.ShipMeshCache;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 讓「在飛船上」的實體(駕駛/站甲板的玩家)被船自己的光照亮:站火把旁會亮、封閉暗艙裡會暗。
 * 船上方塊在隱藏維度、不在真實世界,vanilla 算實體亮度時用真實世界的光 → 跟船體光對不上。
 * 攔 getPackedLightCoords 的回傳:找到實體所在的船 → 用船烤好的 ShipLight(跟 mesh/BER 同一份)在
 * 該 local 位置的光,sky 用船的(暗艙才會暗)、block 取 max(保留真實世界附近的火把,通常 0)。
 * rider(駕駛)直接拿載具;本機玩家走甲板用小範圍搜尋找船(只對 1 個玩家做,便宜)。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererShipLightMixin {

    @Inject(method = "getPackedLightCoords", at = @At("RETURN"), cancellable = true)
    private void koniava$shipLight(Entity entity, float partialTick, CallbackInfoReturnable<Integer> cir) {
        ShipEntity ship = entity.getVehicle() instanceof ShipEntity s ? s : null;
        Minecraft mc = Minecraft.getInstance();
        if (ship == null && entity == mc.player && mc.level != null) {
            List<ShipEntity> near = mc.level.getEntitiesOfClass(ShipEntity.class, entity.getBoundingBox().inflate(2));
            if (!near.isEmpty()) ship = near.get(0);
        }
        if (ship == null || ship.getContraption() == null) return;
        if (!(ship.getMeshCache() instanceof ShipMeshCache cache)) return;
        ShipLight sl = cache.getShipLight();
        if (sl == null) return;

        Vec3 lp = ship.worldToLocalPoint(entity.getX(), entity.getY() + 0.5, entity.getZ());
        if (!ship.getContraption().bounds().inflate(1).contains(lp.x, lp.y, lp.z)) return; // 不在船範圍 → 不套
        BlockPos local = BlockPos.containing(lp);

        int vanilla = cir.getReturnValue();
        cir.setReturnValue(LightTexture.pack(Math.max(LightTexture.block(vanilla), sl.block(local)), sl.sky(local)));
    }
}
