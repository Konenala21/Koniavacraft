package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 船上箱子開蓋動畫。船的箱子真身在影子維度、視覺船的 render BE 不被 tick，所以開箱預設沒有開蓋動畫。
 * 玩家在船上開箱時記下那一格，client 每 tick 對該 render BE 的箱子推開蓋（triggerEvent + lidAnimateTick），
 * ChestRenderer 讀 getOpenNess 就會畫出開合。容器畫面關了就收蓋，收完清掉。一次追一個箱子（常見情境）。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipChestAnimator {
    private ShipChestAnimator() {}

    private static ShipEntity ship;
    private static BlockPos local;
    private static boolean wantOpen;
    private static boolean screenWasOpen; // 容器畫面開過了沒（避免畫面還沒開就被當成關掉而提早收蓋）
    private static int waitTicks;          // 開蓋後等容器畫面出現的 tick 數（超時=誤觸發，自動收蓋）
    private static int closeGrace;         // 畫面關了連續幾 tick（重開選單瞬間會閃關 1 tick，要寬限幾 tick 才真收蓋，否則快速重點蓋子會拍動）

    /** 玩家在船上開了某格箱子 → 開始開蓋動畫。 */
    public static void notifyOpened(ShipEntity s, BlockPos l) {
        ship = s;
        local = l.immutable();
        wantOpen = true;
        screenWasOpen = false;
        waitTicks = 0;
        closeGrace = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ship == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (ship.isRemoved() || mc.level == null) { ship = null; return; }

        boolean screenOpen = mc.screen instanceof AbstractContainerScreen<?>;
        if (screenOpen) { screenWasOpen = true; closeGrace = 0; }
        if (screenWasOpen && !screenOpen) {
            // 容器畫面開過又關了 → 收蓋。但重開選單瞬間畫面會閃關 1~2 tick，寬限幾 tick 才真收，
            // 否則快速重複點右鍵時每次閃關都收蓋一下 → 蓋子全開↔全關拍動。
            if (++closeGrace > 4) wantOpen = false;
        } else if (wantOpen && !screenWasOpen && ++waitTicks > 30) {
            wantOpen = false; // ~1.5s 沒等到畫面(誤觸發)→ 收蓋
        }

        BlockEntity be = ship.getRenderBlockEntities().get(local);
        if (!(be instanceof ChestBlockEntity chest)) { ship = null; return; }

        chest.triggerEvent(1, wantOpen ? 1 : 0);                                              // 事件 1 = 開蓋；count>0 開、0 關
        ChestBlockEntity.lidAnimateTick(ship.level(), local, chest.getBlockState(), chest);   // 推進蓋子插值
        if (!wantOpen && chest.getOpenNess(1.0f) <= 0.0f) ship = null;                        // 收完了 → 清掉
    }
}
