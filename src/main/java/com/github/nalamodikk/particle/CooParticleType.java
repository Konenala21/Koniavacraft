package com.github.nalamodikk.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 可控粒子類型
 */
public class CooParticleType extends ParticleType<CooParticleOptions> {

    public CooParticleType() {
        super(false);  // 不進行遠距離剔除
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
