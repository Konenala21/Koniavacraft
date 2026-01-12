package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.github.nalamodikk.particle.ModParticles;
import net.minecraft.core.particles.ParticleType;
import java.util.UUID;

/**
 * 蝢收擳????摮??? */
public class MagicCircleOptions extends CooParticleOptions {

    public MagicCircleOptions(float size, int color, float alpha, UUID uuid) {
        super(size, color, alpha, uuid);
    }

    public MagicCircleOptions(float size, int color, float alpha) {
        super(size, color, alpha);
    }

    public MagicCircleOptions() {
        super();
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.MAGIC_CIRCLE.get();
    }
}