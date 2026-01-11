package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.CooParticleProvider;
import com.github.nalamodikk.particle.ModParticles;
import com.github.nalamodikk.particle.examples.GuidedFlowParticle;
import com.github.nalamodikk.particle.examples.MagicCircleParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * 客戶端粒子註冊事件
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ParticleRegistrationEvent {

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.COO_PARTICLE.get(), CooParticleProvider::new);
        event.registerSpriteSet(ModParticles.MAGIC_CIRCLE.get(), MagicCircleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GUIDED_FLOW.get(), GuidedFlowParticle.Provider::new);
        KoniavacraftMod.LOGGER.info("✅ 粒子系統註冊完成");
    }
}
