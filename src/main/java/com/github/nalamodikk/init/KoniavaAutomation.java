package com.github.nalamodikk.init;

import com.github.nalamodikk.animation.AnimateManager;
import com.github.nalamodikk.annotations.emitter.EmitterManager;
import com.github.nalamodikk.barrages.BarrageManager;
import com.github.nalamodikk.display.DisplayEntityManager;
import com.github.nalamodikk.event.KoniavaEventBus;
import com.github.nalamodikk.reflect.KoniavaScanner;
import com.github.nalamodikk.reflect.SimpleClassInfo;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 處理模組初始化時的自動化掃描
 */
public class KoniavaAutomation {
    public static void init(FMLCommonSetupEvent event) {
        // 利用 NeoForge 預掃描的數據，避免重複全盤掃描以提升啟動速度
        ModList.get().getAllScanData().forEach(scanData -> {
            scanData.getAnnotations().stream()
                    .collect(Collectors.groupingBy(a -> a.clazz().getClassName()))
                    .forEach((className, annotations) -> {
                        Set<String> annoNames = annotations.stream()
                                .map(a -> a.annotationType().getClassName())
                                .collect(Collectors.toSet());
                        KoniavaScanner.inputScanResult(new SimpleClassInfo(className, annoNames));
                    });
        });

        // 標記掃描完成，防止後續觸發慢速掃描
        KoniavaScanner.markAsLoaded();

        // 初始化事件監聽器
        KoniavaEventBus.init();

        // 初始化顯示實體管理
        DisplayEntityManager.init();
    }

    /**
     * 全域 Tick 勾點，應由 Minecraft 事件觸發
     */
    public static void globalTick() {
        DisplayEntityManager.tick();
        AnimateManager.tick();
        BarrageManager.getInstance().tick();
        EmitterManager.tick();
    }
}
