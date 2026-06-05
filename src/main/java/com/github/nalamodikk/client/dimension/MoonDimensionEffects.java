package com.github.nalamodikk.client.dimension;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 月球天空：純黑無大氣，看得到星空。
 * 與太空維度類似但有地表（站在月球上看天）。
 */
@OnlyIn(Dist.CLIENT)
public class MoonDimensionEffects extends DimensionSpecialEffects {

    public MoonDimensionEffects() {
        // cloudLevel=NaN（無雲）, hasGround=true（有地表，地平線分明）,
        // skyType=NONE（不畫 vanilla 藍天/太陽，全部自己用 shader 畫）
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO; // 無大氣散射 → 黑色霧
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        return null; // 無日出日落顏色
    }
}
