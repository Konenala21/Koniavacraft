package com.github.nalamodikk.particle;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.examples.GuidedFlowOptions;
import com.github.nalamodikk.particle.examples.MagicCircleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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

    public static final DeferredHolder<ParticleType<?>, ParticleType<MagicCircleOptions>> MAGIC_CIRCLE =
        PARTICLE_TYPES.register("magic_circle", () -> new ParticleType<>(false) {
            @Override
            public MapCodec<MagicCircleOptions> codec() {
                // 這裡需要 MagicCircleOptions 的 Codec，我們可以復用 CooParticleOptions 的結構但映射到新類
                // 為了簡單，我們假設字段一樣
                return CooParticleOptions.CODEC.xmap(
                    o -> new MagicCircleOptions(o.getSize(), o.getColor(), o.getAlpha()),
                    o -> o // MagicCircleOptions is CooParticleOptions
                );
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, MagicCircleOptions> streamCodec() {
                return CooParticleOptions.STREAM_CODEC.map(
                    o -> new MagicCircleOptions(o.getSize(), o.getColor(), o.getAlpha()),
                    o -> o
                );
            }
        });

    public static final DeferredHolder<ParticleType<?>, ParticleType<GuidedFlowOptions>> GUIDED_FLOW =
        PARTICLE_TYPES.register("guided_flow", () -> new ParticleType<>(false) {
            @Override
            public MapCodec<GuidedFlowOptions> codec() {
                return CooParticleOptions.CODEC.xmap(
                    o -> new GuidedFlowOptions(o.getSize(), o.getColor(), o.getAlpha()),
                    o -> o
                );
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, GuidedFlowOptions> streamCodec() {
                return CooParticleOptions.STREAM_CODEC.map(
                    o -> new GuidedFlowOptions(o.getSize(), o.getColor(), o.getAlpha()),
                    o -> o
                );
            }
        });

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}