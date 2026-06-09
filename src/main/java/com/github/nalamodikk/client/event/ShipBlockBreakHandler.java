package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.ship.ShipBreakBlockPacket;
import com.github.nalamodikk.common.network.packet.server.ship.ShipInteractPacket;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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

    /**
     * 右鍵互動停著的飛船(放方塊 / 開容器 / 上船座位 / 切換門)。client 算準心 pick,先在本地跑一次 interactWithPick
     * (所有 server 效果都有 isClientSide 守衛 → 無副作用,只看會不會互動)。會互動才發 ShipInteractPacket 給 server
     * 真的做、並取消 vanilla(避免 double + server 用延遲位置 raycast)。不互動(例如手持食物指到船身)就放行 vanilla。
     */
    @SubscribeEvent
    public static void onInteractShip(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof ShipEntity ship)) return;
        Player player = event.getEntity();
        if (!player.level().isClientSide || !ship.isParked()) return;
        ShipEntity.Pick pick = ship.clientPick(player);
        if (pick == null) return;
        InteractionResult result = ship.interactWithPick(player, event.getHand(), pick);
        if (!result.consumesAction()) return; // PASS = 不是船互動 → 讓 vanilla 處理(吃東西等)
        PacketDistributor.sendToServer(new ShipInteractPacket(ship.getId(), pick.local(),
                pick.face().get3DDataValue(), pick.hitLocal(), event.getHand() == InteractionHand.OFF_HAND));
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
