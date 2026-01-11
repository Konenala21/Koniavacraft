package com.github.nalamodikk.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 可控粒子的參數
 */
public class CooParticleOptions implements ParticleOptions {

    public static final MapCodec<CooParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("size").forGetter(o -> o.size),
            Codec.INT.fieldOf("color").forGetter(o -> o.color),
            Codec.FLOAT.fieldOf("alpha").forGetter(o -> o.alpha)
        ).apply(instance, CooParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CooParticleOptions> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.FLOAT, o -> o.size,
            ByteBufCodecs.INT, o -> o.color,
            ByteBufCodecs.FLOAT, o -> o.alpha,
            CooParticleOptions::new
        );

    private final float size;
    private final int color;
    private final float alpha;

    public CooParticleOptions(float size, int color, float alpha) {
        this.size = size;
        this.color = color;
        this.alpha = alpha;
    }

    public CooParticleOptions() {
        this(0.1f, 0xFFFFFF, 1.0f);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.COO_PARTICLE.get();
    }

    public float getSize() {
        return size;
    }

    public int getColor() {
        return color;
    }

    public float getAlpha() {
        return alpha;
    }
}
