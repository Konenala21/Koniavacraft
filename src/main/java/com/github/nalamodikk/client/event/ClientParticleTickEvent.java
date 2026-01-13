package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.ParticleManager;
import com.github.nalamodikk.particle.effects.ClientEffectManager;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.github.nalamodikk.particle.style.ParticleStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * 客戶端粒子 tick 事件
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ClientParticleTickEvent {

    private static final List<ParticleStyle> activeStyles = new ArrayList<>();

    public static void registerStyle(ParticleStyle style) {
        activeStyles.add(style);
    }

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            // ✅ 更新 CooScheduler（所有 Helper 動畫依賴此調用）
            CooScheduler.getInstance().tick();

            // 更新粒子效果
            ClientEffectManager.getInstance().tick();

            // 更新樣式
            for (ParticleStyle style : new ArrayList<>(activeStyles)) {
                try {
                    style.tick();
                } catch (Exception e) {
                    KoniavacraftMod.LOGGER.error("Error ticking style", e);
                    activeStyles.remove(style);
                }
            }

            // 清理過期的粒子引用
            if (event.getLevel().getGameTime() % 100 == 0) {
                ParticleManager.getInstance().cleanup();
            }
        }
    }
}
