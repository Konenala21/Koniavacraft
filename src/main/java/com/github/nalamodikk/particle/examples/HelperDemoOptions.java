package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Helper 演示粒子選項
 */
public class HelperDemoOptions extends CooParticleOptions {

    private final HelperDemoParticle.DemoMode mode;

    public HelperDemoOptions(float size, int color, float alpha, UUID uuid, HelperDemoParticle.DemoMode mode) {
        super(size, color, alpha, uuid);
        this.mode = mode;
    }

    public HelperDemoParticle.DemoMode getMode() {
        return mode;
    }

    // Codec for serialization
    public static final MapCodec<HelperDemoOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            com.mojang.serialization.Codec.FLOAT.fieldOf("size").forGetter(HelperDemoOptions::getSize),
            com.mojang.serialization.Codec.INT.fieldOf("color").forGetter(HelperDemoOptions::getColor),
            com.mojang.serialization.Codec.FLOAT.fieldOf("alpha").forGetter(HelperDemoOptions::getAlpha),
            net.minecraft.core.UUIDUtil.CODEC.fieldOf("uuid").forGetter(HelperDemoOptions::getUuid),
            com.mojang.serialization.Codec.INT.fieldOf("mode").forGetter(o -> o.mode.ordinal())
        ).apply(instance, (size, color, alpha, uuid, modeOrdinal) ->
            new HelperDemoOptions(size, color, alpha, uuid, HelperDemoParticle.DemoMode.values()[modeOrdinal])
        )
    );

    // StreamCodec for network
    public static final StreamCodec<RegistryFriendlyByteBuf, HelperDemoOptions> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.FLOAT, HelperDemoOptions::getSize,
            ByteBufCodecs.INT, HelperDemoOptions::getColor,
            ByteBufCodecs.FLOAT, HelperDemoOptions::getAlpha,
            net.minecraft.core.UUIDUtil.STREAM_CODEC, HelperDemoOptions::getUuid,
            ByteBufCodecs.INT, o -> o.mode.ordinal(),
            (size, color, alpha, uuid, modeOrdinal) ->
                new HelperDemoOptions(size, color, alpha, uuid, HelperDemoParticle.DemoMode.values()[modeOrdinal])
        );

    @Override
    public ParticleType<?> getType() {
        return com.github.nalamodikk.particle.ModParticles.HELPER_DEMO.get();
    }
}
