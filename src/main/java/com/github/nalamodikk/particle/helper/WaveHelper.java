package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;

/**
 * 波動助手
 * 基於 MovementHelper 提供的波動運動功能的快捷方式
 */
public class WaveHelper {

    /**
     * 正弦波運動（委託給 MovementHelper）
     */
    public static void sinusoidalMotion(ControlableParticle particle, net.minecraft.world.phys.Vec3 start,
                                       net.minecraft.world.phys.Vec3 end, float amplitude, float frequency, int duration) {
        MovementHelper.wave(particle, start, end, amplitude, frequency, duration);
    }
}
