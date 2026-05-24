package com.github.nalamodikk.client.particle;

import com.github.nalamodikk.register.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record FormationParticleOptions(
        double cx, double cy, double cz,
        int behavior,
        float radius,
        float speed
) implements ParticleOptions {

    public static final int ORBIT        = 0;
    public static final int RISE         = 1;
    public static final int BURST        = 2;
    public static final int POINT        = 3;
    public static final int TRACK_PLAYER = 4;
    public static final int STATIC       = 5;

    public static final MapCodec<FormationParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("cx").forGetter(FormationParticleOptions::cx),
            Codec.DOUBLE.fieldOf("cy").forGetter(FormationParticleOptions::cy),
            Codec.DOUBLE.fieldOf("cz").forGetter(FormationParticleOptions::cz),
            Codec.INT.fieldOf("behavior").forGetter(FormationParticleOptions::behavior),
            Codec.FLOAT.fieldOf("radius").forGetter(FormationParticleOptions::radius),
            Codec.FLOAT.fieldOf("speed").forGetter(FormationParticleOptions::speed)
    ).apply(i, FormationParticleOptions::new));

    public static final StreamCodec<FriendlyByteBuf, FormationParticleOptions> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FormationParticleOptions decode(FriendlyByteBuf buf) {
            return new FormationParticleOptions(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readByte(), buf.readFloat(), buf.readFloat()
            );
        }

        @Override
        public void encode(FriendlyByteBuf buf, FormationParticleOptions v) {
            buf.writeDouble(v.cx()).writeDouble(v.cy()).writeDouble(v.cz());
            buf.writeByte(v.behavior()).writeFloat(v.radius()).writeFloat(v.speed());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return ModParticles.FORMATION_SPARK.get();
    }
}
