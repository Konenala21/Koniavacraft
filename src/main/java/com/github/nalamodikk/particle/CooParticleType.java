package com.github.nalamodikk.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * ???????遴竣?
 */
public class CooParticleType extends ParticleType<CooParticleOptions> {

    public CooParticleType() {
        super(false);  // ??????蹎??嚗???
    }

    @Override
    public MapCodec<CooParticleOptions> codec() {
        return CooParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, CooParticleOptions> streamCodec() {
        return CooParticleOptions.STREAM_CODEC;
    }
}
