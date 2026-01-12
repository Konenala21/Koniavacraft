package com.github.nalamodikk.init;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 處理系統全域 Tick 事件
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class KoniavaTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 在伺服器每一 Tick 結束時更新所有管理器
        KoniavaAutomation.globalTick();
    }
}
