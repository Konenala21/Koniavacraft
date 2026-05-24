package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.particle.FormationParticle;
import com.github.nalamodikk.client.particle.FormationParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KoniavacraftMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<FormationParticleOptions>> FORMATION_SPARK =
            PARTICLE_TYPES.register("formation_spark", () -> new ParticleType<>(true) {
                @Override
                public MapCodec<FormationParticleOptions> codec() {
                    return FormationParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, FormationParticleOptions> streamCodec() {
                    return FormationParticleOptions.STREAM_CODEC;
                }
            });

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }

    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FORMATION_SPARK.get(), FormationParticle.Factory::new);
    }
}
