package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 站在飛船甲板上的實體跟著船一起移動/旋轉（carry）。
 * 站立靠 {@link EntityShipCollisionMixin} 的碰撞撐住，但船會動，光靠碰撞人會滑掉；這裡每 tick
 * 把站在船上的實體依船的位移+繞船心轉角一起帶走。兩側都跑：本地玩家在 client 端被帶走（相機跟著），
 * 生物在 server 端被帶走、client 插值。乘客（坐在座位上）由 positionRider 擺位，不在此。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ShipDeckCarryHandler {

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e instanceof ShipEntity) return;
        if (e.isPassenger()) return; // 坐在座位上的乘客不 carry（positionRider 處理）

        // 跟船走 + 撞牆 已整合進 EntityShipCollisionMixin 的 applyContraptionMovement（相對運動碰撞）。
        // 這裡留 resolveOverlap 當保險（深陷時推出）+ 站甲板時明確設 onGround/清 fallDistance（照 Create）。
        AABB search = e.getBoundingBox().inflate(0.5);
        for (ShipEntity ship : e.level().getEntitiesOfClass(ShipEntity.class, search)) {
            if (ship.getContraption() == null) continue;
            ship.resolveOverlap(e);
            // 船方塊不在世界裡，vanilla 不一定把站甲板的實體判成 onGround → 被當「在空中」會飄/有阻力。
            // 像 Create 一樣在 tick 後明確設(此時 vanilla move 已跑完，不會被蓋)。
            if (ship.isSupporting(e)) {
                e.setOnGround(true);
                e.fallDistance = 0;
            }
        }
    }
}
