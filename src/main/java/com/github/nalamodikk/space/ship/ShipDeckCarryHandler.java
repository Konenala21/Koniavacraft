package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

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

        AABB search = e.getBoundingBox().inflate(0.5);
        List<ShipEntity> ships = e.level().getEntitiesOfClass(ShipEntity.class, search);
        if (ships.isEmpty()) return;
        for (ShipEntity ship : ships) {
            if (ship.getContraption() == null) continue;
            if (ship.isSupporting(e)) {
                ship.carry(e);
                break; // 一次只被一艘船帶
            }
        }
    }
}
