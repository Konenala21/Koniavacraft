package com.github.nalamodikk.client.dimension;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VoidMirrorDimensionEffects extends DimensionSpecialEffects {

    public VoidMirrorDimensionEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.25, 0.25, 0.25);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
