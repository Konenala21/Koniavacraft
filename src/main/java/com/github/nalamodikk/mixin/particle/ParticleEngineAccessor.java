package com.github.nalamodikk.mixin.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor("RENDER_ORDER")
    @Mutable
    static void setRenderOrder(List<ParticleRenderType> renderOrder) {
        throw new UnsupportedOperationException();
    }

    @Accessor("RENDER_ORDER")
    static List<ParticleRenderType> getRenderOrder() {
        throw new UnsupportedOperationException();
    }
}
