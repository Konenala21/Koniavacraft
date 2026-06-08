package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
        // 玩家移動客戶端權威：伺服器端玩家不在這裡推/設 onGround（會跟客戶端打架 → desync）。只客戶端的玩家做。
        if (e instanceof Player && !e.level().isClientSide) return;

        // 跟船走 + 撞牆 已整合進 EntityShipCollisionMixin 的 applyContraptionMovement（相對運動碰撞）。
        // 這裡用「防穿安全網」(snapToDeckSurface,照 Create savePlayerFromClipping)當 catch-all:每-tick 碰撞
        // 漏抓(漂移/角落/部分方塊)時往下射線找甲板、把穿下去的拉回表面。raycast 不像盒推會產生阻力。
        AABB search = e.getBoundingBox().inflate(0.5);
        for (ShipEntity ship : e.level().getEntitiesOfClass(ShipEntity.class, search)) {
            if (ship.getContraption() == null) continue;
            boolean snapped = ship.snapToDeckSurface(e);
            // 船方塊不在世界裡，vanilla 不一定把站甲板的實體判 onGround → 被當在空中會飄。在 tick 後明確設(照 Create)。
            if (snapped || ship.isSupporting(e)) {
                e.setOnGround(true);
                e.fallDistance = 0;
            }
        }
    }
}
