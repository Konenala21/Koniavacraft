package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.ship.ShipBreakBlockPacket;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 玩家左鍵攻擊停著的飛船時,client 算出準心瞄準的 local 方塊、發 ShipBreakBlockPacket 給 server 挖那一格。
 * 不取消攻擊:ShipEntity.hurt 已是 no-op(不會再 server raycast 挖),所以不會雙挖。挖哪格跟準心/外框一致。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipBlockBreakHandler {

    private ShipBlockBreakHandler() {}

    @SubscribeEvent
    public static void onAttackShip(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ShipEntity ship)) return;
        Player player = event.getEntity();
        if (!player.level().isClientSide || !ship.isParked()) return;
        BlockPos local = ship.getAimedLocalBlock(player); // 跟準心/外框同一個 pick
        if (local == null) return;
        PacketDistributor.sendToServer(new ShipBreakBlockPacket(ship.getId(), local));
    }
}
