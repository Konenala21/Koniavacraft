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
 * ??????????? */
public class CooParticleOptions implements ParticleOptions {

    public static final MapCodec<CooParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("size").forGetter((CooParticleOptions o) -> o.size),
            Codec.INT.fieldOf("color").forGetter((CooParticleOptions o) -> o.color),
            Codec.FLOAT.fieldOf("alpha").forGetter((CooParticleOptions o) -> o.alpha),
            net.minecraft.core.UUIDUtil.CODEC.fieldOf("uuid").forGetter((CooParticleOptions o) -> o.uuid)
        ).apply(instance, CooParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CooParticleOptions> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.FLOAT, (CooParticleOptions o) -> o.size,
            ByteBufCodecs.INT, (CooParticleOptions o) -> o.color,
            ByteBufCodecs.FLOAT, (CooParticleOptions o) -> o.alpha,
            net.minecraft.core.UUIDUtil.STREAM_CODEC, (CooParticleOptions o) -> o.uuid,
            CooParticleOptions::new
        );

    private final float size;
    private final int color;
    private final float alpha;
    private final java.util.UUID uuid;

    public CooParticleOptions(float size, int color, float alpha, java.util.UUID uuid) {
        this.size = size;
        this.color = color;
        this.alpha = alpha;
        this.uuid = uuid;
    }

    public CooParticleOptions(float size, int color, float alpha) {
        this(size, color, alpha, java.util.UUID.randomUUID());
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

    public java.util.UUID getUuid() {
        return uuid;
    }
}
