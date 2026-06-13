package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.ship.ShipBreakBlockPacket;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 飛船方塊的「原版式挖掘」:按住左鍵對著停著的飛船方塊,用 vanilla 破壞速度(方塊硬度 + 玩家工具)累積進度,
 * 挖滿才送 ShipBreakBlockPacket;過程中 ShipEntityRenderer 用 vanilla 裂紋貼圖畫進度。
 * 創造模式的瞬破由 ShipBlockBreakHandler 處理,這裡只做生存挖掘。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipMiningHandler {
    private ShipMiningHandler() {}

    private static ShipEntity ship;
    private static BlockPos local;
    private static float progress;
    private static int swingCd;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null
                || mc.player.getAbilities().instabuild          // 創造:瞬破走 ShipBlockBreakHandler
                || !mc.options.keyAttack.isDown()                // 沒按住左鍵
                || !(mc.hitResult instanceof EntityHitResult ehr)
                || !(ehr.getEntity() instanceof ShipEntity s) || !s.isParked()) {
            reset(); return;
        }
        BlockPos aimed = s.getAimedLocalBlock(mc.player);
        if (aimed == null || aimed.equals(BlockPos.ZERO)) { reset(); return; } // 核心(0,0,0)不可挖
        var info = s.getContraption() != null ? s.getContraption().getBlocks().get(aimed) : null;
        if (info == null) { reset(); return; }
        if (s != ship || !aimed.equals(local)) { ship = s; local = aimed.immutable(); progress = 0f; swingCd = 0; } // 換目標歸零

        BlockState state = info.state();
        progress += state.getDestroyProgress(mc.player, mc.level, aimed); // vanilla 破壞速度(硬度/工具),pos 只取硬度不讀該格
        if (--swingCd <= 0) { mc.player.swing(InteractionHand.MAIN_HAND); swingCd = 4; } // 揮手動畫
        if (progress >= 1.0f) {
            PacketDistributor.sendToServer(new ShipBreakBlockPacket(s.getId(), aimed));
            s.clientPredictBreak(aimed); // 本地即時消失(配合預測)
            reset();
        }
    }

    /** 給渲染器:這艘船正在挖的 local 格,沒在挖回 null。 */
    public static BlockPos minedLocal(ShipEntity s) { return s == ship ? local : null; }

    /** 裂紋階數 0~9,沒在挖回 -1。 */
    public static int crackStage(ShipEntity s) {
        if (s != ship || local == null || progress <= 0f) return -1;
        return Math.min(9, (int) (progress * 10f));
    }

    private static void reset() { ship = null; local = null; progress = 0f; swingCd = 0; }
}
