package com.github.nalamodikk.particle;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 粒子類型註冊
 */
public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, KoniavacraftMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, CooParticleType> COO_PARTICLE =
        PARTICLE_TYPES.register("coo_particle", CooParticleType::new);

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
