package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.ParticleManager;
import com.github.nalamodikk.particle.effects.ClientEffectManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 客戶端粒子 tick 事件
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ClientParticleTickEvent {

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            // 更新粒子效果
            ClientEffectManager.getInstance().tick();

            // 清理過期的粒子引用
            if (event.getLevel().getGameTime() % 100 == 0) {
                ParticleManager.getInstance().cleanup();
            }
        }
    }
}
