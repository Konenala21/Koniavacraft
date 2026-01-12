package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.github.nalamodikk.particle.ModParticles;
import net.minecraft.core.particles.ParticleType;
import java.util.UUID;

/**
 * 撘?瘚?摮??? */
public class GuidedFlowOptions extends CooParticleOptions {

    public GuidedFlowOptions(float size, int color, float alpha, UUID uuid) {
        super(size, color, alpha, uuid);
    }

    public GuidedFlowOptions(float size, int color, float alpha) {
        super(size, color, alpha);
    }

    public GuidedFlowOptions() {
        super();
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.GUIDED_FLOW.get();
    }
}