package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.github.nalamodikk.particle.ModParticles;
import net.minecraft.core.particles.ParticleType;

/**
 * 魔法陣粒子參數
 */
public class MagicCircleOptions extends CooParticleOptions {

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
