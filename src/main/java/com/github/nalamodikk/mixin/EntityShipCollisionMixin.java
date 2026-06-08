package com.github.nalamodikk.mixin;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 讓外部實體（玩家/生物）能站上飛船甲板、撞到船的方塊牆。
 * Entity.collide 已把 desired 移動依「世界方塊」解算；這裡在它回傳後，再依附近飛船的方塊（在實體
 * local 框做逐軸 AABB 碰撞）進一步限制，回傳兩者交集。船方塊不在世界裡所以原本撞不到，這個補上。
 * 站著的船即可站/走；移動中的船「跟著走」由 carry handler 另外處理。
 */
@Mixin(Entity.class)
public class EntityShipCollisionMixin {

    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void koniava$collideWithShips(Vec3 desired, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ShipEntity) return; // 船自己不走這套（船的碰撞是 resolveTerrain）
        // 玩家移動是客戶端權威：只在客戶端對自己的玩家算碰撞。伺服器端再算一次會跟客戶端打架（server input =
        // client output，又把站好的玩家往上/外推）→ 客戶端站甲板、伺服器彈出去 → desync。Create 也跳過 server 玩家。
        if (self instanceof Player && !self.level().isClientSide) return;

        Vec3 motion = cir.getReturnValue();
        AABB box = self.getBoundingBox();
        AABB search = box.expandTowards(motion).inflate(1.0);
        List<ShipEntity> ships = self.level().getEntitiesOfClass(ShipEntity.class, search);
        if (ships.isEmpty()) return;

        Vec3 m = motion;
        for (ShipEntity ship : ships) {
            if (self.getVehicle() == ship) continue; // 自己駕駛/乘坐的船不互撞（由 positionRider 擺位）
            if (ship.getContraption() == null) continue;
            // 相對運動碰撞：跟船走 + 撞牆擋住 合一（取代舊的 restrictMotion + 另外 carry）
            m = ship.applyContraptionMovement(self, m);
        }
        if (m.x != motion.x || m.y != motion.y || m.z != motion.z) cir.setReturnValue(m);
    }
}
