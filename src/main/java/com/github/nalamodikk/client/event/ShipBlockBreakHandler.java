package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.ship.ShipBreakBlockPacket;
import com.github.nalamodikk.common.network.packet.server.ship.ShipInteractPacket;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import com.github.nalamodikk.client.screen.ship.ShipControlScreen;
import com.github.nalamodikk.space.ship.ShipCoreBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ChestBlock;
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
        if (!player.getAbilities().instabuild) return; // 生存:交給 ShipMiningHandler 按住挖(裂紋進度),這裡只負責創造瞬破
        BlockPos local = ship.getAimedLocalBlock(player); // 跟準心/外框同一個 pick
        if (local == null) return;
        ship.clientPredictBreak(local); // 本地先移除,免等封包來回的延遲
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
        // 不檢查 isParked:開門/開箱/機器/上船座位 飛行中也能用,只有「放方塊編輯」要停船(interactWithPick 內部自己 gate)。
        if (!player.level().isClientSide) return;
        ShipEntity.Pick pick = ship.clientPick(player);
        if (pick == null) return;
        // 右鍵核心(主手、非潛行) → 開控制台 GUI(油門滑桿/燃料/引擎)
        var ci = ship.getContraption() != null ? ship.getContraption().getBlocks().get(pick.local()) : null;
        if (event.getHand() == InteractionHand.MAIN_HAND && !player.isSecondaryUseActive()
                && ci != null && ci.state().getBlock() instanceof ShipCoreBlock) {
            Minecraft.getInstance().setScreen(new ShipControlScreen(ship));
            event.setCanceled(true);
            return;
        }
        InteractionResult result = ship.interactWithPick(player, event.getHand(), pick);
        if (!result.consumesAction()) return; // PASS = 不是船互動 → 讓 vanilla 處理(吃東西等)
        PacketDistributor.sendToServer(new ShipInteractPacket(ship.getId(), pick.local(),
                pick.face().get3DDataValue(), pick.hitLocal(), event.getHand() == InteractionHand.OFF_HAND));
        event.setCanceled(true);
        event.setCancellationResult(result);
        // 開箱動畫由 server 權威訊號(ShipChestLidPacket)驅動 ShipChestAnimator，不再 client 端猜，見 ShipEntity.onChestOpened。
    }
}
